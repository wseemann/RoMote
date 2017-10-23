package wseemann.media.romote.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jaku.core.JakuRequest;
import com.jaku.core.KeypressKeyValues;
import com.jaku.request.KeypressRequest;

import java.util.List;

import wseemann.media.romote.R;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.RokuRequestTypes;
import wseemann.media.romote.view.MyLinearLayout;
import wseemann.media.romote.view.RepeatingImageButton;

/**
 * Created by wseemann on 6/19/16.
 */
public class RemoteFragment extends Fragment implements VolumeDialogFragment.VolumeDialogListener {

    private String mOldText = "";
    private EditText mTextBox;
    private ImageView mVoiceSearcButton;

    private ScrollView mScrollView;
    private MyLinearLayout mLinearLayout;
    private ImageView mSearchIcon;
    private Drawable mTextBoxBackground;

    private boolean mOverrideFocusChange = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote, container, false);

        mTextBox = (EditText) view.findViewById(R.id.textbox);
        mVoiceSearcButton = (ImageView) view.findViewById(R.id.voice_search_btn);

        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        mLinearLayout = (MyLinearLayout) view.findViewById(R.id.layout);

        mSearchIcon = (ImageView) view.findViewById(R.id.search_icon);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mVoiceSearcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySpeechRecognizer();
            }
        });
        mVoiceSearcButton.requestFocus();

        linkButton(KeypressKeyValues.BACK, R.id.back_button);
        linkRepeatingButton(KeypressKeyValues.UP, R.id.up_button);
        linkAltButton(KeypressKeyValues.HOME, R.id.home_button);

        linkRepeatingButton(KeypressKeyValues.LEFT, R.id.left_button);
        linkAltButton(KeypressKeyValues.SELECT, R.id.select_button);
        linkRepeatingButton(KeypressKeyValues.RIGHT, R.id.right_button);

        linkButton(KeypressKeyValues.INTANT_REPLAY, R.id.instant_replay_button);
        linkRepeatingButton(KeypressKeyValues.DOWN, R.id.down_button);
        linkButton(KeypressKeyValues.INFO, R.id.info_button);

        linkButton(KeypressKeyValues.REV, R.id.rev_button);
        linkButton(KeypressKeyValues.PLAY, R.id.play_button);
        linkButton(KeypressKeyValues.FWD, R.id.fwd_button);

        final EditText textbox = (EditText) getView().findViewById(R.id.textbox);
        mTextBoxBackground = textbox.getBackground();
        textbox.setBackgroundResource(0);
        textbox.setOnEditorActionListener(new EditText.OnEditorActionListener() {
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
        /*textbox.setOnKeyListener(new View.OnKeyListener() {
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
        });*/
        textbox.addTextChangedListener(new TextWatcher() {
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
        textbox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (!mOverrideFocusChange) {
                        mSearchIcon.setVisibility(View.INVISIBLE);
                        mVoiceSearcButton.setVisibility(View.INVISIBLE);

                        textbox.setBackgroundResource(0);
                        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(mTextBox.getWindowToken(), 0);
                    }
                }
            }
        });

        ImageButton volumeButton = (ImageButton) getView().findViewById(R.id.volume_button);
        volumeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment fragment = VolumeDialogFragment.newInstance(RemoteFragment.this);
                fragment.show(getFragmentManager(), "volume_dialog");
            }
        });

        ImageButton keyboardButton = (ImageButton) getView().findViewById(R.id.keyboard_button);
        keyboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOverrideFocusChange = true;

                mLinearLayout.setRedraw(false);
                new RedrawTask().execute();

                textbox.requestFocus();
                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(textbox, 0);
            }
        });
    }

    private void linkRepeatingButton(final KeypressKeyValues keypressKeyValue, int id) {
        RepeatingImageButton button = (RepeatingImageButton) getView().findViewById(id);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performKeypress(keypressKeyValue);

                clearTextBox();
            }
        });

        button.setRepeatListener(new RepeatingImageButton.RepeatListener() {
            @Override
            public void onRepeat(View v, long duration, int repeatcount) {
                performKeypress(keypressKeyValue);

                clearTextBox();
            }
        }, 400);
    }

    private void linkButton(final KeypressKeyValues keypressKeyValue, int id) {
        ImageButton button = (ImageButton) getView().findViewById(id);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performKeypress(keypressKeyValue);

                clearTextBox();
            }
        });
    }

    private void linkAltButton(final KeypressKeyValues keypressKeyValue, int id) {
        Button button = (Button) getView().findViewById(id);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performKeypress(keypressKeyValue);

                clearTextBox();
            }
        });
    }

    private void performKeypress(KeypressKeyValues keypressKeyValue) {
        String url = CommandHelper.getDeviceURL(getActivity());

        KeypressRequest keypressRequest = new KeypressRequest(url, keypressKeyValue.getValue());
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

    private void clearTextBox() {
        mTextBox.setText("");
        mTextBox.clearFocus();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.remote_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_cancel) {
            getActivity().finish();
            return true;
        }

        return false;
    }

    private static final int SPEECH_REQUEST_CODE = 0;

    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            // Do something with spokenText
            mTextBox.setText(spokenText);
            //Toast.makeText(getActivity(), spokenText, Toast.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class RedrawTask extends AsyncTask<Void, Void, String> {

        public RedrawTask() {

        }

        @Override
        public String doInBackground(Void... Void) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {

            }

            return null;
        }

        @Override
        protected void onPostExecute(String str) {
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);

            mSearchIcon.setVisibility(View.VISIBLE);
            mVoiceSearcButton.setVisibility(View.VISIBLE);
            mTextBox.setBackgroundDrawable(mTextBoxBackground);

            mTextBox.requestFocus();

            mOverrideFocusChange = false;
        }
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

            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {

            }
        }).execute(RokuRequestTypes.keypress);
    }

    @Override
    public void onVolumeChanged(final KeypressKeyValues keypressKeyValue) {
        String url = CommandHelper.getDeviceURL(getActivity());

        KeypressRequest keypressRequest = new KeypressRequest(url, keypressKeyValue.getValue());
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
}
