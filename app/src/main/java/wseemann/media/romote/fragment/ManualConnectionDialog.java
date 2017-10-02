package wseemann.media.romote.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import wseemann.media.romote.R;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.parser.DeviceInfoParser;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;

import static wseemann.media.romote.fragment.VolumeDialogFragment.mListener;

/**
 * Created by wseemann on 6/20/16.
 */
public class ManualConnectionDialog extends DialogFragment {

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
                sendCommand(CommandHelper.getDeviceInfoURL(ManualConnectionDialog.this.getContext(), mHost));
            }
        });


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setTitle(getString(R.string.connect_manually));
        builder.setMessage(getString(R.string.connect_help));
        builder.setPositiveButton(R.string.install_channel_dialog_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mListener != null) {
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (mListener != null) {
                }
            }
        });

        return builder.create();
    }

    private void sendCommand(String command) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(getActivity());

        String url = command;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        mProgressLayout.setVisibility(View.GONE);

                        DeviceInfoParser parser = new DeviceInfoParser();

                        Device device = parser.parse(response);

                        storeDevice(device);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mProgressLayout.setVisibility(View.GONE);
                mErrorText.setVisibility(View.VISIBLE);
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void storeDevice(Device device) {
        device.setHost(mHost);

        DBUtils.insertDevice(getActivity(), device);
        PreferenceUtils.setConnectedDevice(getActivity(), device.getSerialNumber());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("first_use", false);
        editor.commit();

        dismiss();
    }
}