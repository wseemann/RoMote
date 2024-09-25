package wseemann.media.romote.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import wseemann.media.romote.R;

/**
 * Created by wseemann on 6/19/16.
 */
public class ConnectivityDialog extends DialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.connectivity_dialog_title));
        builder.setMessage(getString(R.string.connectivity_dialog_message));
        builder.setNeutralButton(R.string.connectivity_dialog_button, (dialog, id) -> {
            try {
                // In some cases, a matching Activity may not exist,so ensure you safeguard against this.
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            } catch (ActivityNotFoundException ex) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });

        return builder.create();
    }
}
