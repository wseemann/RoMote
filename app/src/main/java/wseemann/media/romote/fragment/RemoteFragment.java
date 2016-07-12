package wseemann.media.romote.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.List;

import wseemann.media.romote.R;
import wseemann.media.romote.service.CommandService;
import wseemann.media.romote.utils.CommandConstants;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.view.RepeatingImageButton;

/**
 * Created by wseemann on 6/19/16.
 */
public class RemoteFragment extends Fragment {

    private String mOldText = "";
    private SearchView mSearchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote, container, false);

        mSearchView = (SearchView) view.findViewById(R.id.search_view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        // Assumes current activity is the searchable activity
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setQueryHint("Enter search query");
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                int difference = newText.length() - mOldText.length();

                // clear button was pressed
                if (newText.equals("")) {
                    int diff = mOldText.length() - newText.length();

                    for (int i = 0; i < diff; i++) {
                        Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                        intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), CommandConstants.BACKSPACE_COMMAND));
                        RemoteFragment.this.getActivity().startService(intent);
                    }

                    mOldText = newText;

                    return false;
                }

                if (difference > 1) {
                    newText.replace(mOldText, "");

                    char [] chars =  newText.toCharArray();

                    String [] commands = new String[chars.length];

                    for (int i = 0; i < chars.length; i++) {
                        char key = chars[i];

                        if (key == ' ') {
                            key = '+';
                        }

                        commands[i] = CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), CommandConstants.INPUT_COMMAND + String.valueOf(key));
                    }

                    mOldText = newText;

                    Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                    intent.setAction("commands");
                    intent.putExtra("commands", commands);
                    RemoteFragment.this.getActivity().startService(intent);

                    return false;
                }

                String key = null;

                if (newText.length() > 0) {
                    key = newText.substring(newText.length() - 1);
                }

                if (mOldText.length() > newText.length()) {
                    key = CommandConstants.BACKSPACE_COMMAND;
                }

                if (key != null && key.equals(" ")) {
                    key = "+";
                }

                mOldText = newText;

                if (key != null) {
                    Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);

                    if (key.equals(CommandConstants.BACKSPACE_COMMAND)) {
                        intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), key));
                    } else {
                        intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), CommandConstants.INPUT_COMMAND + key));
                    }

                    RemoteFragment.this.getActivity().startService(intent);
                }

                return false;
            }

        });

        linkButton(CommandConstants.BACK_COMMAND, R.id.back_button);
        linkRepeatingButton(CommandConstants.UP_COMMAND, R.id.up_button);
        linkAltButton(CommandConstants.HOME_COMMAND, R.id.home_button);

        linkRepeatingButton(CommandConstants.LEFT_COMMAND, R.id.left_button);
        linkAltButton(CommandConstants.SELECT_COMMAND, R.id.select_button);
        linkRepeatingButton(CommandConstants.RIGHT_COMMAND, R.id.right_button);

        linkButton(CommandConstants.INSTANT_REPLAY_COMMAND, R.id.instant_replay_button);
        linkRepeatingButton(CommandConstants.DOWN_COMMAND, R.id.down_button);
        linkButton(CommandConstants.INFO_COMMAND, R.id.info_button);

        linkButton(CommandConstants.REV_COMMAND, R.id.rev_button);
        linkButton(CommandConstants.PLAY_COMMAND, R.id.play_button);
        linkButton(CommandConstants.FWD_COMMAND, R.id.fwd_button);

        final EditText textbox = (EditText) getView().findViewById(R.id.textbox);
        textbox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == 67 &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        textbox.length() == 0) {
                    Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                    intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), CommandConstants.BACKSPACE_COMMAND));
                    RemoteFragment.this.getActivity().startService(intent);
                    return true;
                }

                return false;
            }
        });
        textbox.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newText = s.toString();

                int difference = newText.length() - mOldText.length();

                // clear button was pressed
                if (newText.equals("")) {
                    int diff = mOldText.length() - newText.length();

                    for (int i = 0; i < diff; i++) {
                        Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                        intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), CommandConstants.BACKSPACE_COMMAND));
                        RemoteFragment.this.getActivity().startService(intent);
                    }

                    mOldText = newText;

                    return;
                }

                if (difference > 1) {
                    newText.replace(mOldText, "");

                    char [] chars =  newText.toCharArray();

                    String [] commands = new String[chars.length];

                    for (int i = 0; i < chars.length; i++) {
                        char key = chars[i];

                        if (key == ' ') {
                            key = '+';
                        }

                        commands[i] = CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), CommandConstants.INPUT_COMMAND + String.valueOf(key));
                    }

                    mOldText = newText;

                    Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                    intent.setAction("commands");
                    intent.putExtra("commands", commands);
                    RemoteFragment.this.getActivity().startService(intent);

                    return;
                }

                String key = null;

                if (newText.length() > 0) {
                    key = newText.substring(newText.length() - 1);
                }

                if (mOldText.length() > newText.length()) {
                    key = CommandConstants.BACKSPACE_COMMAND;
                }

                if (key != null && key.equals(" ")) {
                    key = "+";
                }

                mOldText = newText;

                if (key != null) {
                    Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);

                    if (key.equals(CommandConstants.BACKSPACE_COMMAND)) {
                        intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), key));
                    } else {
                        intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), CommandConstants.INPUT_COMMAND + key));
                    }

                    RemoteFragment.this.getActivity().startService(intent);
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

        ImageButton keyboardButton = (ImageButton) getView().findViewById(R.id.keyboard_button);
        keyboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textbox.setVisibility(View.VISIBLE);
                textbox.setBackgroundColor(getResources().getColor(android.R.color.white));

                ((InputMethodManager) RemoteFragment.this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(textbox, InputMethodManager.SHOW_FORCED);
            }
        });
    }

    private void linkRepeatingButton(final String command, int id) {
        RepeatingImageButton button = (RepeatingImageButton) getView().findViewById(id);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), command));
                RemoteFragment.this.getActivity().startService(intent);
            }
        });

        button.setRepeatListener(new RepeatingImageButton.RepeatListener() {
            @Override
            public void onRepeat(View v, long duration, int repeatcount) {
                Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), command));
                RemoteFragment.this.getActivity().startService(intent);
            }
        }, 400);
    }

    private void linkButton(final String command, int id) {
        ImageButton button = (ImageButton) getView().findViewById(id);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), command));
                RemoteFragment.this.getActivity().startService(intent);
            }
        });
    }

    private void linkAltButton(final String command, int id) {
        Button button = (Button) getView().findViewById(id);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                intent.setAction(CommandHelper.getKeypressURL(RemoteFragment.this.getActivity(), command));
                RemoteFragment.this.getActivity().startService(intent);
            }
        });
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
            Toast.makeText(getActivity(), spokenText, Toast.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
