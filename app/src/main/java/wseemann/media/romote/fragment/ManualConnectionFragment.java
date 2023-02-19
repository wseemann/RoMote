package wseemann.media.romote.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.RokuRequestTypes;

import com.jaku.core.JakuRequest;
import com.jaku.parser.DeviceParser;
import com.jaku.request.QueryDeviceInfoRequest;

import javax.inject.Inject;

/**
 * Created by wseemann on 6/26/16.
 */
@AndroidEntryPoint
public class ManualConnectionFragment extends Fragment {

    @Inject
    protected SharedPreferences sharedPreferences;

    @Inject
    protected PreferenceUtils preferenceUtils;

    private EditText mIpAddressText;
    private Button mConnectButton;
    private LinearLayout mProgressLayout;
    private TextView mErrorText;

    private String mHost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manual_connection, container, false);

        mIpAddressText = (EditText) view.findViewById(R.id.ip_address_text);
        mConnectButton = (Button) view.findViewById(R.id.connect_button);
        mProgressLayout = (LinearLayout) view.findViewById(R.id.progress_layout);
        mErrorText = (TextView) view.findViewById(R.id.error_text);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getActivity().setResult(Activity.RESULT_CANCELED);

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String ipAddress = mIpAddressText.getText().toString();
                mHost = "http://" + ipAddress + ":8060";

                mErrorText.setVisibility(View.GONE);
                mProgressLayout.setVisibility(View.VISIBLE);
                sendCommand(mHost);
            }
        });

        ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(mIpAddressText, InputMethodManager.SHOW_FORCED);
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

        Intent intent = new Intent();
        getActivity().setResult(Activity.RESULT_OK, intent);
        getActivity().finish();
    }
}