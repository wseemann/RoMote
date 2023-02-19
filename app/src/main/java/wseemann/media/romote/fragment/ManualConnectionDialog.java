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

import androidx.fragment.app.DialogFragment;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.RokuRequestTypes;

import com.jaku.core.JakuRequest;
import com.jaku.parser.DeviceParser;
import com.jaku.request.QueryDeviceInfoRequest;

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
    private Button mConnectButton;
    private LinearLayout mProgressLayout;
    private TextView mErrorText;

    private String mHost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(true);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_fragment_manual_connection, null);
        mIpAddressText = (EditText) view.findViewById(R.id.ip_address_text);
        Button connectButton = (Button) view.findViewById(R.id.connect_button);
        mProgressLayout = (LinearLayout) view.findViewById(R.id.progress_layout);
        mErrorText = (TextView) view.findViewById(R.id.error_text);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String ipAddress = mIpAddressText.getText().toString();
                String mHost = "http://" + ipAddress + ":8060";

                mErrorText.setVisibility(View.GONE);
                mProgressLayout.setVisibility(View.VISIBLE);
                sendCommand(commandHelper.getDeviceInfoURL(mHost));
            }
        });


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setTitle(getString(R.string.connect_manually));
        builder.setMessage(getString(R.string.connect_help));
        builder.setPositiveButton(R.string.install_channel_dialog_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        return builder.create();
    }

    private void sendCommand(String command) {
        String url = command;

        QueryDeviceInfoRequest queryActiveAppRequest = new QueryDeviceInfoRequest(url);
        JakuRequest request = new JakuRequest(queryActiveAppRequest, new DeviceParser());

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {
                Device device = (Device) result.mResultValue;

                mProgressLayout.setVisibility(View.GONE);

                storeDevice(device);
            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {
                mProgressLayout.setVisibility(View.GONE);
                mErrorText.setVisibility(View.VISIBLE);
            }
        }).execute(RokuRequestTypes.query_device_info);
    }

    private void storeDevice(Device device) {
        device.setHost(mHost);

        DBUtils.insertDevice(getActivity(), device);
        preferenceUtils.setConnectedDevice(device.getSerialNumber());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("first_use", false);
        editor.commit();

        dismiss();
    }
}