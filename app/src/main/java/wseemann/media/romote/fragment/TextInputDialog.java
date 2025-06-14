package wseemann.media.romote.fragment;

import java.io.UnsupportedEncodingException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.core.KeyPressKeyValues;
import com.wseemann.ecp.request.KeyPressRequest;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.utils.CommandHelper;

/**
 * Created by wseemann on 6/20/16.
 */
@AndroidEntryPoint
public class TextInputDialog extends DialogFragment {

    @Inject
    protected CommandHelper commandHelper;

    private String mOldText = "";
    private EditText mTextBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_fragment_test_input, null);
        mTextBox = view.findViewById(R.id.text_box);

        setupTextBox();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setNegativeButton(R.string.close, (dialog, id) -> dismiss());

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        showSoftKeyboard(mTextBox);
    }

    private void showSoftKeyboard(final View view) {
        if (view.requestFocus()) {
            final InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            view.postDelayed(() -> {
                view.requestFocus();
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }, 100);
        }
    }

    private void setupTextBox() {
        mTextBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (event.getKeyCode() == 67 &&
                        event.getAction() == KeyEvent.ACTION_DOWN) { //&&
                    //textbox.length() == 0) {
                    sendBackspace();
                    return true;
                }
            }
            return false;
        });
        mTextBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == 67 &&
                        event.getAction() == KeyEvent.ACTION_DOWN) { //&&
                        //textbox.length() == 0) {
                    sendBackspace();
                    return true;
                }

                return false;
            }
        });
        mTextBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newText = s.toString();

                int difference = newText.length() - mOldText.length();

                // clear button was pressed
                if (newText.equals("")) {
                    int diff = mOldText.length() - newText.length();

                    for (int i = 0; i < diff; i++) {
                        sendBackspace();
                    }

                    mOldText = newText;

                    return;
                }

                if (difference > 1) {
                    newText.replace(mOldText, "");

                    mOldText = newText;

                    sendStringLiteral(newText);

                    return;
                }

                String key = null;

                if (newText.length() > 0) {
                    key = newText.substring(newText.length() - 1);
                }

                if (mOldText.length() > newText.length()) {
                    key = "BACKSPACE";
                }

                mOldText = newText;

                if (key != null) {
                    if (key.equals("BACKSPACE")) {
                        sendBackspace();
                    } else {
                        if (key.equals(" ")) {
                            key = "%20";
                        }

                        sendStringLiteral(key);
                    }
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void sendBackspace() {
        String url = commandHelper.getDeviceURL();

        KeyPressRequest keyPressRequest;
        try {
            keyPressRequest = new KeyPressRequest(url, KeyPressKeyValues.BACKSPACE.getValue());
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return;
        }
        keyPressRequest.sendAsync(new ResponseCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void unused) {

            }

            @Override
            public void onError(@NonNull Exception e) {

            }
        });
    }

    private void sendStringLiteral(String stringLiteral) {
        String url = commandHelper.getDeviceURL();

        KeyPressRequest keypressRequest;
        try {
            keypressRequest = new KeyPressRequest(url, KeyPressKeyValues.LIT_.getValue() + stringLiteral);
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return;
        }
        keypressRequest.sendAsync(new ResponseCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void unused) {

            }

            @Override
            public void onError(@NonNull Exception e) {

            }
        });
    }
}
