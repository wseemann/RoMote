package wseemann.media.romote.keyboard

import com.wseemann.ecp.core.KeyPressKeyValues
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Relays what the user types on the phone's soft keyboard to the device, one ECP key press at a
 * time.
 *
 * The screen hands over the whole contents of its text field after every edit rather than
 * individual keystrokes, because that is all an IME gives it: printable characters are committed
 * through the InputConnection, not delivered as key events, and a paste, a swipe-typed word or an
 * autocorrect replacement arrives as a single change. [onTextChanged] turns each of those into keys
 * by backspacing to the longest common prefix and re-typing the rest. The device's caret is always
 * at the end of its own field, so replaying the tail is also what repairs it when the user edits
 * the middle of the text on the phone.
 *
 * The device's field cannot be read back over the ECP, so [text] is only this app's picture of it,
 * and it starts empty every time the keyboard is raised.
 *
 * @param sendKey sends one [Key] and blocks until the device answers.
 */
class KeyboardRelay(scope: CoroutineScope, dispatcher: CoroutineDispatcher, private val sendKey: (Key) -> Unit) {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    /**
     * Keys waiting to go out. ECPRequest.sendAsync starts a fresh thread per request, so characters
     * posted back to back can reach the device out of order; draining them through a single
     * consumer is what keeps them in sequence.
     */
    private val keyQueue = Channel<Key>(Channel.UNLIMITED)

    init {
        scope.launch(dispatcher) {
            for (key in keyQueue) {
                try {
                    sendKey(key)
                } catch (ex: Exception) {
                    Timber.tag(TAG).e(ex, "Failed to relay a typed key")
                }
            }
        }
    }

    fun toggle() {
        if (_state.value.isActive) dismiss() else _state.update { State(isActive = true) }
    }

    fun dismiss() {
        if (_state.value.isActive) {
            _state.update { State() }
        }
    }

    fun done() {
        enqueue(Key.Named(KeyPressKeyValues.ENTER))
        dismiss()
    }

    /**
     * Also the way to delete text the device already had before the keyboard was raised, so it
     * always sends, even once [text] has run empty.
     */
    fun backspace() {
        enqueue(Key.Named(KeyPressKeyValues.BACKSPACE))

        _state.update { current ->
            if (current.text.isEmpty()) current else current.copy(text = current.text.dropLastCodePoint())
        }
    }

    fun onTextChanged(text: String) {
        val previous = _state.value.text

        if (text == previous) {
            return
        }

        val common = commonPrefixLength(previous, text)

        repeat(previous.codePointCount(common, previous.length)) {
            enqueue(Key.Named(KeyPressKeyValues.BACKSPACE))
        }

        // Walk by code point so an emoji goes out as one literal key rather than as the two halves
        // of a surrogate pair, neither of which is a character on its own.
        var index = common
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            enqueue(Key.Literal(String(Character.toChars(codePoint))))
            index += Character.charCount(codePoint)
        }

        _state.update { it.copy(text = text) }
    }

    private fun enqueue(key: Key) {
        keyQueue.trySend(key)
    }

    /**
     * The two kinds stay apart all the way down because the wrapper sends them through different
     * calls: a named key goes out under its own name, while a literal one has to be prefixed to
     * become a Lit_ key.
     */
    sealed interface Key {

        data class Named(val value: KeyPressKeyValues) : Key

        /** Exactly one code point, which the device takes as a Lit_ key. */
        data class Literal(val text: String) : Key
    }

    data class State(val isActive: Boolean = false, val text: String = "")

    private companion object {
        const val TAG = "KeyboardRelay"

        /**
         * Never splits a surrogate pair: two strings that share a high surrogate but differ in the
         * low one differ in that character, they do not share it.
         */
        fun commonPrefixLength(first: String, second: String): Int {
            val shared = first.commonPrefixWith(second)

            return if (shared.isNotEmpty() && shared.last().isHighSurrogate()) {
                shared.length - 1
            } else {
                shared.length
            }
        }

        fun String.dropLastCodePoint(): String = substring(0, offsetByCodePoints(length, -1))
    }
}
