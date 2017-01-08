package wseemann.media.romote.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import wseemann.media.romote.R;
import wseemann.media.romote.utils.CommandConstants;

public class VolumeDialogFragment extends DialogFragment implements DialogInterface.OnCancelListener {

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface VolumeDialogListener {
        public void onVolumeChanged(String command);
    }

    // Use this instance of the interface to deliver action events
    static VolumeDialogListener mListener;

    /* Call this to instantiate a new VolumeTimerDialog.
     * @param activity  The activity hosting the dialog, which must implement the
     *                  VolumeDialogListener to receive event callbacks.
     * @returns A new instance of VolumeTimerDialog.
     * @throws  ClassCastException if the host activity does not
     *          implement VolumeDialogListener
     */
    public static VolumeDialogFragment newInstance(Fragment fragment) {
        // Verify that the host activity implements the callback interface
        try {
            mListener = (VolumeDialogListener) fragment;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(fragment.toString()
                    + " must implement VolumeDialogListener");
        }

        VolumeDialogFragment frag = new VolumeDialogFragment();

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View view = factory.inflate(R.layout.dialog_fragment_volume, null);

        Button muteVolumeButton = (Button) view.findViewById(R.id.mute_volume_button);
        addOnClickListener(muteVolumeButton, CommandConstants.VOLUME_MUTE_COMMAND);
        Button decreaseVolumeButton = (Button) view.findViewById(R.id.decrease_volume_button);
        addOnClickListener(decreaseVolumeButton, CommandConstants.VOLUME_DOWN_COMMAND);
        Button increaseVolumeButton = (Button) view.findViewById(R.id.increase_volume_button);
        addOnClickListener(increaseVolumeButton, CommandConstants.VOLUME_UP_COMMAND);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.volume_dialog_title);
        builder.setCancelable(true);
        builder.setView(view);
        builder.setOnCancelListener(this);
        builder.setNeutralButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        return builder.create();
    }

    public void onCancel(DialogInterface dialog) {

    }

    private void addOnClickListener(Button button, final String keypress) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onVolumeChanged(keypress);
                }
            }
        });
    }
}