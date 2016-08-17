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
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import wseemann.media.romote.activity.DeviceInfoActivity;
import wseemann.media.romote.activity.ManualConnectionActivity;
import wseemann.media.romote.adapter.DeviceAdapter;
import wseemann.media.romote.adapter.SeparatedListAdapter;
import wseemann.media.romote.loader.AvailableDevicesLoader;
import wseemann.media.romote.loader.PairedDevicesLoader;
import wseemann.media.romote.model.Device;

import wseemann.media.romote.R;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.widget.RokuAppWidgetProvider;

/**
 * Created by wseemann on 6/19/16.
 */
public class MainFragment extends ListFragment {

    private TextView mSelectDeviceText;
    private RelativeLayout mProgressLayout;
    private ListView mList;
    private SeparatedListAdapter mAdapter;
    private DeviceAdapter mPairedDeviceAdapter;
    private DeviceAdapter mAvailableDeviceAdapter;

    private SwipeRefreshLayout mSwiperefresh;
    private FloatingActionButton mFab;

    private OnDeviceSelectedListener mListener;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showMenu((View) msg.obj);
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (OnDeviceSelectedListener) context;
        } catch (ClassCastException ex) {
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        mSelectDeviceText = (TextView) view.findViewById(R.id.select_device_text);
        mProgressLayout = (RelativeLayout) view.findViewById(R.id.progress_layout);

        mList = (ListView) view.findViewById(android.R.id.list);
        View emptyView = view.findViewById(android.R.id.empty);
        mList.setEmptyView(emptyView);

        mSwiperefresh = (SwipeRefreshLayout) view.findViewById(R.id.swiperefresh);
        mSwiperefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        setLoadingText(true);
                        getLoaderManager().restartLoader(0, new Bundle(), mAvailableDevicesLoader);
                    }
                }
        );

        mFab = (FloatingActionButton) view.findViewById(R.id.fab);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Device device = (Device) parent.getItemAtPosition(position);

                DBUtils.insertDevice(getActivity(), device);
                PreferenceUtils.setConnectedDevice(getActivity(), device.getSerialNumber());

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("first_use", false);
                editor.commit();

                Toast.makeText(getActivity(), "Device " + device.getSerialNumber() + " " + getString(R.string.connected), Toast.LENGTH_LONG).show();

                MainFragment.this.getActivity().sendBroadcast(new Intent(Constants.UPDATE_DEVICE_BROADCAST));

                AppWidgetManager widgetManager = AppWidgetManager.getInstance(getActivity());
                ComponentName widgetComponent = new ComponentName(getActivity(), RokuAppWidgetProvider.class);
                int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
                Intent update = new Intent();
                update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
                update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                getActivity().sendBroadcast(update);

                if (mListener != null) {
                    mListener.onDeviceSelected();
                }

                mAvailableDeviceAdapter.clear();
                mAdapter.notifyDataSetChanged();

                getLoaderManager().restartLoader(1, new Bundle(), mPairedDevicesLoader);
            }
        });

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainFragment.this.getActivity(), ManualConnectionActivity.class);
                startActivityForResult(intent, 0);
            }
        });

        mAdapter = new SeparatedListAdapter(getActivity());
        mPairedDeviceAdapter = new DeviceAdapter(getActivity(), new ArrayList<Device>(), mHandler);
        mAvailableDeviceAdapter = new DeviceAdapter(getActivity(), new ArrayList<Device>(), mHandler);

        mAdapter.addSection("Paired devices", mPairedDeviceAdapter);
        mAdapter.addSection("Available devices", mAvailableDeviceAdapter);

        setListAdapter(mAdapter);

        mSwiperefresh.setRefreshing(true);
        refreshList(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            setLoadingText(true);
            //mSwiperefresh.setRefreshing(true);
            getLoaderManager().restartLoader(0, new Bundle(), mAvailableDevicesLoader);
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

            refreshList(false);
        }
    }

    private LoaderManager.LoaderCallbacks<List<Device>> mPairedDevicesLoader
            = new LoaderManager.LoaderCallbacks<List<Device>>() {

        @Override
        public Loader<List<Device>> onCreateLoader(int arg0, Bundle args) {
            return new PairedDevicesLoader(getActivity(), args);
        }

        @Override
        public void onLoadFinished(Loader<List<Device>> loader, List<Device> devices) {
            mPairedDeviceAdapter.clear();
            mAdapter.notifyDataSetChanged();

            if (devices.size() == 0) {
                return;
            }

            // Set the new devices in the adapter.
            for (int i = 0; i < devices.size(); i++) {
                mPairedDeviceAdapter.add(devices.get(i));
            }

            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<Device>> devices) {
            // Clear the devices in the adapter.
            mPairedDeviceAdapter.clear();
        }
    };

    private LoaderManager.LoaderCallbacks<List<Device>> mAvailableDevicesLoader
            = new LoaderManager.LoaderCallbacks<List<Device>>() {

        @Override
        public Loader<List<Device>> onCreateLoader(int arg0, Bundle args) {
            return new AvailableDevicesLoader(getActivity(), args);
        }

        @Override
        public void onLoadFinished(Loader<List<Device>> loader, List<Device> devices) {
            setLoadingText(false);
            mSwiperefresh.setRefreshing(false);

            mAvailableDeviceAdapter.clear();
            mAdapter.notifyDataSetChanged();

            if (devices.size() == 0) {
                return;
            }

            // Set the new devices in the adapter.
            for (int i = 0; i < devices.size(); i++) {
                mAvailableDeviceAdapter.add(devices.get(i));
            }

            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<Device>> devices) {
            // Clear the devices in the adapter.
            mAvailableDeviceAdapter.clear();
        }
    };

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
        PopupMenu popup = new PopupMenu(getActivity(), v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Device device = (Device) v.getTag();

                switch (item.getItemId()) {
                    case R.id.action_info:
                        Intent intent = new Intent(getActivity(), DeviceInfoActivity.class);
                        intent.putExtra("serial_number", device.getSerialNumber());
                        intent.putExtra("host", device.getHost());
                        startActivity(intent);
                        return true;
                    case R.id.action_unpair:
                        PreferenceUtils.setConnectedDevice(getActivity(), "");
                        DBUtils.removeDevice(getActivity(), device.getSerialNumber());
                        refreshList(false);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.inflate(R.menu.device_menu);

        Device device = (Device) v.getTag();

        if (DBUtils.getDevice(getActivity(), device.getSerialNumber()) == null) {
            popup.getMenu().removeItem(R.id.action_unpair);
        }

        popup.show();
    }

    private void refreshList(boolean showLoadingText) {
        setLoadingText(showLoadingText);
        getLoaderManager().restartLoader(1, new Bundle(), mPairedDevicesLoader);
        getLoaderManager().restartLoader(0, new Bundle(), mAvailableDevicesLoader);
    }

    public interface OnDeviceSelectedListener {
        public void onDeviceSelected();
    }
}
