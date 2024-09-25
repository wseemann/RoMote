package wseemann.media.romote.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.request.QueryDeviceInfoRequest;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.tasks.ResponseCallbackWrapper;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;

import javax.inject.Inject;

/**
 * Created by wseemann on 6/20/16.
 */
@AndroidEntryPoint
public class ManualConnectionDialog extends DialogFragment {

    @Inject
    protected SharedPreferences sharedPreferences;

    @Inject
    protected CommandHelper commandHelper;

    @Inject
    protected PreferenceUtils preferenceUtils;

    private EditText mIpAddressText;
    private LinearLayout mProgressLayout;
    private TextView mErrorText;

    private String mHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_fragment_manual_connection, null);
        mIpAddressText = view.findViewById(R.id.ip_address_text);
        Button connectButton = view.findViewById(R.id.connect_button);
        mProgressLayout = view.findViewById(R.id.progress_layout);
        mErrorText = view.findViewById(R.id.error_text);

        connectButton.setOnClickListener(v -> {

            String ipAddress = mIpAddressText.getText().toString();
            mHost = "http://" + ipAddress + ":8060";

            mErrorText.setVisibility(View.GONE);
            mProgressLayout.setVisibility(View.VISIBLE);
            sendCommand(commandHelper.getDeviceInfoURL(mHost));
        });


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setTitle(getString(R.string.connect_manually));
        builder.setMessage(getString(R.string.connect_help));
        builder.setPositiveButton(R.string.install_channel_dialog_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, id) -> { });

        return builder.create();
    }

    private void sendCommand(String command) {
        QueryDeviceInfoRequest queryActiveAppRequest = new QueryDeviceInfoRequest(command);
        queryActiveAppRequest.sendAsync(new ResponseCallbackWrapper<>(new ResponseCallback<>() {
            @Override
            public void onSuccess(@Nullable com.wseemann.ecp.model.Device device) {
                mProgressLayout.setVisibility(View.GONE);
                storeDevice(device);
            }

            @Override
            public void onError(@NonNull Exception e) {
                mProgressLayout.setVisibility(View.GONE);
                mErrorText.setVisibility(View.VISIBLE);
            }
        }));
    }

    private void storeDevice(com.wseemann.ecp.model.Device device) {
        device.setHost(mHost);

        DBUtils.insertDevice(getActivity(), wseemann.media.romote.model.Device.Companion.fromDevice(device));
        preferenceUtils.setConnectedDevice(device.getSerialNumber());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("first_use", false);
        editor.commit();

        dismiss();
    }
}