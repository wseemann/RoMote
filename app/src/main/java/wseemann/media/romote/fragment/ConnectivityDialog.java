package wseemann.media.romote.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import wseemann.media.romote.R;

/**
 * Created by wseemann on 6/19/16.
 */
public class ConnectivityDialog extends DialogFragment {

    private static ConnectivityFragmentListener mListener;

    public static ConnectivityDialog getInstance(Activity activity) {
        /*try {
            // Instantiate the ConnectivityFragmentListener so we can send events with it
            mListener = (ConnectivityFragmentListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement ConnectivityFragmentListener");
        }*/

        return new ConnectivityDialog();
    }

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
        builder.setNeutralButton(R.string.connectivity_dialog_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        return builder.create();
    }

    public interface ConnectivityFragmentListener {
        public void onDialogCancelled();
    }
}
