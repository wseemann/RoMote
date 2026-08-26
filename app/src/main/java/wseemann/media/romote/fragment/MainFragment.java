package wseemann.media.romote.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.activity.DeviceInfoActivity;
import wseemann.media.romote.activity.ManualConnectionActivity;
import wseemann.media.romote.adapter.DeviceAdapter;
import wseemann.media.romote.adapter.SeparatedListAdapter;
import wseemann.media.romote.composables.MainScreenDialogHost;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import javax.inject.Inject;
import wseemann.media.romote.R;
import wseemann.media.romote.event.MainScreenUiEvent;
import wseemann.media.romote.data.Device;
import wseemann.media.romote.model.MainScreenUiState;
import wseemann.media.romote.utils.BroadcastUtils;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.WindowInsetsUtils;
import wseemann.media.romote.viewmodels.MainScreenViewModel;
import wseemann.media.romote.widget.RokuAppWidgetProvider;

@AndroidEntryPoint
public class MainFragment extends ListFragment {

    @Inject
    protected SharedPreferences sharedPreferences;

    @Inject
    protected PreferenceUtils preferenceUtils;

    private TextView mSelectDeviceText;
    private RelativeLayout mProgressLayout;
    private ListView mList;
    private SeparatedListAdapter mAdapter;
    private DeviceAdapter mPairedDeviceAdapter;
    private DeviceAdapter mAvailableDeviceAdapter;

    private SwipeRefreshLayout mSwiperefresh;
    private FloatingActionButton mFab;

    private OnDeviceSelectedListener mListener;

    private MainScreenViewModel mainScreenViewModel;
    private MainScreenUiState mUiState;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showMenu((View) msg.obj);
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mListener = (OnDeviceSelectedListener) context;
        } catch (ClassCastException ex) {
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainScreenViewModel = new ViewModelProvider(this).get(MainScreenViewModel.class);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        WindowInsetsUtils.applyNavigationBarBottomPadding(view);

        mSelectDeviceText = view.findViewById(R.id.select_device_text);
        mProgressLayout = view.findViewById(R.id.progress_layout);

        MainScreenDialogHost.bindRenameDialog(view.findViewById(R.id.rename_dialog_host), mainScreenViewModel);

        mList = view.findViewById(android.R.id.list);
        View emptyView = view.findViewById(android.R.id.empty);
        mList.setEmptyView(emptyView);

        mSwiperefresh = view.findViewById(R.id.swiperefresh);
        mSwiperefresh.setOnRefreshListener(
                () -> {
                    // Kicks off the actual data-refresh operation. The uiState observer
                    // clears the refreshing and loading indicators when it's finished.
                    mainScreenViewModel.onHandleEvent(MainScreenUiEvent.RefreshEvent.INSTANCE);
                }
        );

        mFab = view.findViewById(R.id.fab);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // The lists are rendered on every emission. Gating them on !isLoading meant a scan that
        // never finished froze the list at its previous contents - which is exactly what a
        // forgotten device looked like when it refused to come back.
        mainScreenViewModel.getUiStateLiveData().observe(getViewLifecycleOwner(), state -> {
            mUiState = state;

            mSwiperefresh.setRefreshing(state.isLoading());
            setLoadingText(state.isLoading());

            if (mAdapter == null) {
                // The adapters are built in onActivityCreated, which follows the first emission
                // only if the view lifecycle raced ahead; refreshList() there republishes.
                return;
            }

            onAvailableDevicesLoadFinished(state.getAvailableDevices());
            onPairedDeviceLoadFinished(state.getPairedDevices());
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mList.setOnItemClickListener((parent, view, position, id) -> {
            Device device = (Device) parent.getItemAtPosition(position);

            DBUtils.insertDevice(getActivity(), device);
            preferenceUtils.setConnectedDevice(device.getSerialNumber());

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("first_use", false);
            editor.commit();

            Toast.makeText(getActivity(), "Device " + device.getSerialNumber() + " " + getString(R.string.connected), Toast.LENGTH_SHORT).show();

            BroadcastUtils.Companion.sendUpdateDeviceBroadcast(requireContext());

            AppWidgetManager widgetManager = AppWidgetManager.getInstance(getActivity());
            ComponentName widgetComponent = new ComponentName(requireActivity(), RokuAppWidgetProvider.class);
            int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
            Intent update = new Intent();
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
            update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            requireActivity().sendBroadcast(update);

            if (mListener != null) {
                mListener.onDeviceSelected();
            }

            mAvailableDeviceAdapter.clear();
            mAdapter.notifyDataSetChanged();

            mainScreenViewModel.onHandleEvent(MainScreenUiEvent.LoadPairedDevicesEvent.INSTANCE);
        });

        mFab.setOnClickListener(view -> {
            Intent intent = new Intent(MainFragment.this.getActivity(), ManualConnectionActivity.class);
            startActivityForResult(intent, 0);
        });

        mAdapter = new SeparatedListAdapter(getActivity());
        mPairedDeviceAdapter = new DeviceAdapter(getActivity(), new ArrayList<>(), mHandler, preferenceUtils);
        mAvailableDeviceAdapter = new DeviceAdapter(getActivity(), new ArrayList<>(), mHandler, preferenceUtils);

        mAdapter.addSection("Paired devices", mPairedDeviceAdapter);
        mAdapter.addSection("Available devices", mAvailableDeviceAdapter);

        setListAdapter(mAdapter);

        refreshList();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            mainScreenViewModel.onHandleEvent(MainScreenUiEvent.RefreshEvent.INSTANCE);
            return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            mAvailableDeviceAdapter.clear();
            mAdapter.notifyDataSetChanged();

            refreshList();
        }
    }

    private void onAvailableDevicesLoadFinished(List<Device> devices) {
        mAvailableDeviceAdapter.clear();
        mAdapter.notifyDataSetChanged();

        if (devices.isEmpty()) {
            return;
        }

        // Set the new devices in the adapter.
        for (int i = 0; i < devices.size(); i++) {
            mAvailableDeviceAdapter.add(devices.get(i));
        }

        mAdapter.notifyDataSetChanged();
    }

    public void setLoadingText(boolean shown) {
        if (shown) {
            mSelectDeviceText.setVisibility(View.GONE);
            mProgressLayout.setVisibility(View.VISIBLE);
        } else {
            mSelectDeviceText.setVisibility(View.GONE);
            mProgressLayout.setVisibility(View.GONE);
        }
    }

    private void showMenu(final View v) {
        PopupMenu popup = new PopupMenu(requireActivity(), v);
        popup.setOnMenuItemClickListener(item -> {
            Device device = (Device) v.getTag();

            int itemId = item.getItemId();
            if (itemId == R.id.action_rename) {
                String customUserDeviceName = device.getCustomUserDeviceName();

                mainScreenViewModel.onHandleEvent(new MainScreenUiEvent.RenameDeviceClickedEvent(
                        device.getSerialNumber(),
                        customUserDeviceName == null ? "" : customUserDeviceName));
                return true;
            } else if (itemId == R.id.action_info) {
                Intent intent = new Intent(getActivity(), DeviceInfoActivity.class);
                intent.putExtra("serial_number", device.getSerialNumber());
                intent.putExtra("host", device.getHost());
                startActivity(intent);
                return true;
            } else if (itemId == R.id.action_unpair) {
                mainScreenViewModel.onHandleEvent(
                        new MainScreenUiEvent.ForgetDeviceEvent(device.getSerialNumber()));
                return true;
            } else {
                return false;
            }
        });
        popup.inflate(R.menu.device_menu);

        Device device = (Device) v.getTag();

        if (!isPaired(device.getSerialNumber())) {
            popup.getMenu().removeItem(R.id.action_unpair);
        }

        popup.show();
    }

    private void refreshList() {
        mainScreenViewModel.onHandleEvent(MainScreenUiEvent.RefreshEvent.INSTANCE);
    }

    private boolean isPaired(String serialNumber) {
        if (mUiState == null) {
            return false;
        }

        for (Device pairedDevice : mUiState.getPairedDevices()) {
            if (serialNumber.equals(pairedDevice.getSerialNumber())) {
                return true;
            }
        }

        return false;
    }

    private void onPairedDeviceLoadFinished(List<Device> devices) {
        mPairedDeviceAdapter.clear();
        mAdapter.notifyDataSetChanged();

        if (devices.isEmpty()) {
            return;
        }

        // Set the new devices in the adapter.
        for (int i = 0; i < devices.size(); i++) {
            mPairedDeviceAdapter.add(devices.get(i));
        }

        mAdapter.notifyDataSetChanged();
    }

    public interface OnDeviceSelectedListener {
        void onDeviceSelected();
    }
}
