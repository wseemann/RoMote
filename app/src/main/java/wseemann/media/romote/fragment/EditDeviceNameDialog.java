package wseemann.media.romote.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;

import wseemann.media.romote.R;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.utils.DBUtils;

/**
 * Created by wseemann on 6/20/16.
 */
public class EditDeviceNameDialog extends DialogFragment {

    private static final String EXTRA_CUSTOM_USER_DEVICE_NAME = "custom_user_device_name";
    private static final String EXTRA_SERIAL_NUMBER = "serial_number";

    private EditDeviceNameDialogListener mListener;
    private EditText mCustomUserDeviceNameText;

    public static EditDeviceNameDialog getInstance(String customUserDeviceName, String serialNumber) {
        EditDeviceNameDialog fragment = new EditDeviceNameDialog();

        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_CUSTOM_USER_DEVICE_NAME, customUserDeviceName);
        bundle.putString(EXTRA_SERIAL_NUMBER, serialNumber);
        fragment.setArguments(bundle);

        return fragment;
    }

    public void setListener(EditDeviceNameDialogListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_fragment_edit_device_name, null);
        mCustomUserDeviceNameText = (EditText) view.findViewById(R.id.custom_user_device_name_text);
        mCustomUserDeviceNameText.setText(getArguments().getString(EXTRA_CUSTOM_USER_DEVICE_NAME));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setMessage(getString(R.string.device_name_help));
        builder.setPositiveButton(R.string.action_rename, (dialog, id) -> {
            Device device = DBUtils.getDevice(getContext(), getArguments().getString(EXTRA_SERIAL_NUMBER));
            device.setCustomUserDeviceName(mCustomUserDeviceNameText.getText().toString());
            DBUtils.updateDevice(getContext(), device);

            if (mListener != null) {
                mListener.onDeviceNameUpdated();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, id) -> {
        });

        return builder.create();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    public interface EditDeviceNameDialogListener {
        void onDeviceNameUpdated();
    }
}