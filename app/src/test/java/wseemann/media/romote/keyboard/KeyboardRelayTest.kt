package wseemann.media.romote.keyboard

import com.wseemann.ecp.core.KeyPressKeyValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The relay's job is to turn an edit into the keys that reproduce it on a device whose own field
 * cannot be read back, so what is asserted here is the key sequence, in order.
 *
 * Dispatchers.Unconfined resumes the sending coroutine on the thread that queued the key, which
 * makes the relay's sends land synchronously and keeps these tests free of a scheduler.
 */
class KeyboardRelayTest {

    private val sent = mutableListOf<KeyboardRelay.Key>()

    private val relay = KeyboardRelay(
        scope = CoroutineScope(Dispatchers.Unconfined),
        dispatcher = Dispatchers.Unconfined
    ) { key -> sent += key }

    /**
     * The keys sent since the last call, so each step of a test asserts only its own output,
     * rendered the way Device puts them on the wire.
     */
    private fun drain(): List<String> = drainKeys().map { key ->
        when (key) {
            is KeyboardRelay.Key.Named -> key.value.value
            is KeyboardRelay.Key.Literal -> KeyPressKeyValues.LIT_.value + key.text
        }
    }

    /** The keys themselves, for the tests that care which kind of key was sent. */
    private fun drainKeys(): List<KeyboardRelay.Key> = sent.toList().also { sent.clear() }

    @Test
    fun `typing sends one literal per character`() {
        relay.onTextChanged("h")
        relay.onTextChanged("hi")

        assertEquals(listOf("Lit_h", "Lit_i"), drain())
    }

    @Test
    fun `a space goes out as a literal space, not as an escape`() {
        relay.onTextChanged(" ")

        assertEquals(listOf("Lit_ "), drain())
    }

    @Test
    fun `deleting a character sends one backspace`() {
        relay.onTextChanged("hi")
        drain()

        relay.onTextChanged("h")

        assertEquals(listOf("Backspace"), drain())
    }

    @Test
    fun `clearing the field backspaces over everything that was typed`() {
        relay.onTextChanged("hi")
        drain()

        relay.onTextChanged("")

        assertEquals(listOf("Backspace", "Backspace"), drain())
    }

    @Test
    fun `pasting onto existing text sends only what was added`() {
        relay.onTextChanged("ab")
        drain()

        relay.onTextChanged("abcde")

        assertEquals(listOf("Lit_c", "Lit_d", "Lit_e"), drain())
    }

    @Test
    fun `an autocorrect replacement backspaces to the common prefix and retypes the rest`() {
        relay.onTextChanged("teh")
        drain()

        relay.onTextChanged("the")

        assertEquals(listOf("Backspace", "Backspace", "Lit_h", "Lit_e"), drain())
    }

    @Test
    fun `an edit in the middle replays the tail, since the device's caret is at the end`() {
        relay.onTextChanged("ac")
        drain()

        relay.onTextChanged("abc")

        assertEquals(listOf("Backspace", "Lit_b", "Lit_c"), drain())
    }

    @Test
    fun `an emoji goes out whole rather than as two surrogate halves`() {
        relay.onTextChanged("😀")

        assertEquals(listOf("Lit_😀"), drain())
    }

    @Test
    fun `deleting an emoji costs one backspace, not two`() {
        relay.onTextChanged("a😀")
        drain()

        relay.onTextChanged("a")

        assertEquals(listOf("Backspace"), drain())
    }

    @Test
    fun `two emoji sharing a high surrogate are not treated as a common prefix`() {
        relay.onTextChanged("😀")
        drain()

        relay.onTextChanged("😁")

        assertEquals(listOf("Backspace", "Lit_😁"), drain())
    }

    @Test
    fun `the bar's backspace sends even when nothing has been typed, because the device has text`() {
        relay.backspace()

        assertEquals(listOf("Backspace"), drain())
        assertEquals("", relay.state.value.text)
    }

    @Test
    fun `the bar's backspace drops a whole code point from the text it has`() {
        relay.onTextChanged("a😀")
        drain()

        relay.backspace()

        assertEquals("a", relay.state.value.text)
    }

    @Test
    fun `done commits with enter and puts the keyboard away`() {
        relay.toggle()
        relay.onTextChanged("hi")
        drain()

        relay.done()

        assertEquals(listOf("Enter"), drain())
        assertEquals(KeyboardRelay.State(), relay.state.value)
    }

    @Test
    fun `raising the keyboard again starts from an empty buffer`() {
        relay.toggle()
        relay.onTextChanged("hi")
        relay.toggle()
        relay.toggle()
        drain()

        assertEquals("", relay.state.value.text)

        // The device's field cannot be read back, so a second session must not assume the first
        // one's text is still there to backspace over.
        relay.onTextChanged("a")

        assertEquals(listOf("Lit_a"), drain())
    }

    @Test
    fun `a named key stays a named key rather than text to be typed`() {
        relay.backspace()

        assertEquals(
            listOf(KeyboardRelay.Key.Named(KeyPressKeyValues.BACKSPACE)),
            drainKeys()
        )
    }

    @Test
    fun `a typed character is a literal of exactly one code point`() {
        relay.onTextChanged("\uD83D\uDE00")

        assertEquals(listOf(KeyboardRelay.Key.Literal("\uD83D\uDE00")), drainKeys())
    }
}
