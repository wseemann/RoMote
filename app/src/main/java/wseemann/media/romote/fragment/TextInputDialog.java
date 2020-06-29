package wseemann.media.romote.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.jaku.core.JakuRequest;
import com.jaku.core.KeypressKeyValues;
import com.jaku.request.KeypressRequest;

import java.util.ArrayDeque;
import java.util.Deque;

import wseemann.media.romote.R;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.RokuRequestTypes;

/**
 * Created by wseemann on 6/20/16.
 */
public class TextInputDialog extends DialogFragment {

    private String mOldText = "";
    private EditText mTextBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_fragment_test_input, null);
        mTextBox = (EditText) view.findViewById(R.id.text_box);

        setupTextBox();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dismiss();
            }
        });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        showSoftKeyboard(mTextBox);

        //InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        //inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void showSoftKeyboard(final View view) {
        if (view.requestFocus()) {
            final InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.requestFocus();
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
            }, 100);
        }
    }

    private void setupTextBox() {
        mTextBox.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (event.getKeyCode() == 67 &&
                            event.getAction() == KeyEvent.ACTION_DOWN) { //&&
                        //textbox.length() == 0) {
                        sendBackspace();
                        return true;
                    }
                }
                return false;
            }
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
        String url = CommandHelper.getDeviceURL(getActivity());

        KeypressRequest keypressRequest = new KeypressRequest(url, KeypressKeyValues.BACKSPACE.getValue());
        JakuRequest request = new JakuRequest(keypressRequest, null);

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {

            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {

            }
        }).execute(RokuRequestTypes.keypress);
    }

    private void sendStringLiteral(String stringLiteral) {
        String url = CommandHelper.getDeviceURL(getActivity());

        KeypressRequest keypressRequest = new KeypressRequest(url, KeypressKeyValues.LIT_.getValue() + stringLiteral);
        JakuRequest request = new JakuRequest(keypressRequest, null);

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {
                Log.d("sasas", "OK");
            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {
                Log.d("sasas", "error");
            }
        }).execute(RokuRequestTypes.keypress);
        Deque<Integer> stack = new ArrayDeque<Integer>();
    }
}