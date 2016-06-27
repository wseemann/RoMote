package wseemann.media.romote.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.ImageButton;

import wseemann.media.romote.R;
import wseemann.media.romote.service.CommandService;
import wseemann.media.romote.utils.CommandConstants;
import wseemann.media.romote.view.KeyboardEditText;
import wseemann.media.romote.view.RepeatingImageButton;

/**
 * Created by wseemann on 6/19/16.
 */
public class RemoteFragment extends Fragment {

    private String mOldText = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote, container, false);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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

        final KeyboardEditText textbox = (KeyboardEditText) getView().findViewById(R.id.textbox);
        textbox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == 67 &&
                        event.getAction() == KeyEvent.ACTION_DOWN &&
                        textbox.length() == 0) {
                    Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                    intent.setAction(CommandConstants.BACKSPACE_COMMAND);
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
                        intent.setAction(key);
                    } else {
                        intent.setAction(CommandConstants.INPUT_COMMAND + key);
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
        textbox.setOnKeyboardListener(new KeyboardEditText.KeyboardListener() {
            @Override
            public void onStateChanged(KeyboardEditText keyboardEditText, boolean showing) {
                if (!showing) {
                    textbox.setVisibility(View.INVISIBLE);
                    textbox.setBackgroundColor(getResources().getColor(android.R.color.black));
                }
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
                intent.setAction(command);
                RemoteFragment.this.getActivity().startService(intent);
            }
        });

        button.setRepeatListener(new RepeatingImageButton.RepeatListener() {
            @Override
            public void onRepeat(View v, long duration, int repeatcount) {
                Intent intent = new Intent(RemoteFragment.this.getContext(), CommandService.class);
                intent.setAction(command);
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
                intent.setAction(command);
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
                intent.setAction(command);
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
}
