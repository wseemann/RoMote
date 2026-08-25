package wseemann.media.romote.fragment;

import android.os.Bundle;

import androidx.fragment.app.ListFragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.adapter.DeviceInfoAdapter;
import wseemann.media.romote.model.DeviceInfoUiState;
import wseemann.media.romote.viewmodels.DeviceInfoScreenViewModel;

/**
 * Created by wseemann on 6/19/16.
 */
@AndroidEntryPoint
public class DeviceInfoFragment extends ListFragment {

    private DeviceInfoAdapter mAdapter;

    public static DeviceInfoFragment getInstance(String serialNumber, String host) {
        DeviceInfoFragment fragment = new DeviceInfoFragment();

        Bundle bundle = new Bundle();
        bundle.putString("serial_number", serialNumber);
        bundle.putString("host", host);

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_device_info));

        Bundle bundle = getArguments();

        if (bundle == null) {
            return;
        }

        String serialNumber = bundle.getString("serial_number");
        String host = bundle.getString("host");

        mAdapter = new DeviceInfoAdapter(getActivity(), new ArrayList<>());
        setListAdapter(mAdapter);
        setListShown(false);

        DeviceInfoScreenViewModel viewModel = new ViewModelProvider(this).get(DeviceInfoScreenViewModel.class);

        viewModel.getDeviceInfoLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof DeviceInfoUiState.Success) {
                mAdapter.clear();
                mAdapter.addAll(((DeviceInfoUiState.Success) state).getEntries());
                mAdapter.notifyDataSetChanged();
                setListShown(true);
            } else if (state instanceof DeviceInfoUiState.Error) {
                setListShown(true);
            }
        });

        viewModel.queryDeviceInfo(serialNumber, host);
    }
}