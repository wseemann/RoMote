package wseemann.media.romote.activity;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;

import android.app.LoaderManager;
import android.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jaku.core.JakuRequest;
import com.jaku.model.Device;
import com.jaku.request.SearchRequest;

import java.util.ArrayList;
import java.util.List;

import wseemann.media.romote.R;
import wseemann.media.romote.adapter.DeviceAdapter;
import wseemann.media.romote.adapter.SeparatedListAdapter;
import wseemann.media.romote.fragment.ChannelFragment;
import wseemann.media.romote.fragment.InstallChannelDialog;
import wseemann.media.romote.fragment.MainFragment;
import wseemann.media.romote.fragment.RemoteFragment;
import wseemann.media.romote.fragment.SearchDialog;
import wseemann.media.romote.fragment.StoreFragment;
import wseemann.media.romote.loader.AvailableDevicesLoader;
import wseemann.media.romote.loader.PairedDevicesLoader;
import wseemann.media.romote.service.NotificationService;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.DBUtils;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.RokuRequestTypes;
import wseemann.media.romote.widget.RokuAppWidgetProvider;

public class AltMainActivity extends ConnectivityActivity implements
        InstallChannelDialog.InstallChannelListener, SearchDialog.SearchDialogListener,
        NavigationView.OnNavigationItemSelectedListener {

    private StoreFragment mStoreFragment;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private NotificationService mService;
    boolean mBound = false;

    private TextView mSelectDeviceText;
    private RelativeLayout mProgressLayout;
    private ListView mList;
    private SeparatedListAdapter mAdapter;
    private DeviceAdapter mPairedDeviceAdapter;
    private DeviceAdapter mAvailableDeviceAdapter;

    private SwipeRefreshLayout mSwiperefresh;

    private OnDeviceSelectedListener mListener;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showMenu((View) msg.obj);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //ManualConnectionDialog fragment = ManualConnectionDialog.getInstance(this);
                //fragment.show(MainFragment.this.getFragmentManager(), ManualConnectionDialog.class.getName());

                Intent intent = new Intent(AltMainActivity.this, ManualConnectionActivity.class);
                startActivityForResult(intent, 0);

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                }
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean("first_use", true)) {
            startActivity(new Intent(this, ConfigureDeviceActivity.class));
            finish();
        }

        Intent intent = getIntent();

        if (intent != null && intent.getData() != null) {
            String channelCode = intent.getData().getPath().replace("/install/", "");

            InstallChannelDialog fragment = InstallChannelDialog.getInstance(this, channelCode);
            fragment.show(getFragmentManager(), InstallChannelDialog.class.getName());
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        // Bind to NotificationService
        Intent intent1 = new Intent(this, NotificationService.class);
        bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);

        mSelectDeviceText = (TextView) findViewById(R.id.select_device_text);
        mProgressLayout = (RelativeLayout) findViewById(R.id.progress_layout);

        mList = (ListView) findViewById(android.R.id.list);
        View emptyView = findViewById(android.R.id.empty);
        mList.setEmptyView(emptyView);

        mSwiperefresh = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        /*mSwiperefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        setLoadingText(true);
                        getLoaderManager().restartLoader(0, new Bundle(), mAvailableDevicesLoader);
                    }
                }
        );*/

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Device device = (Device) parent.getItemAtPosition(position);

                DBUtils.insertDevice(AltMainActivity.this, device);
                PreferenceUtils.setConnectedDevice(AltMainActivity.this, device.getSerialNumber());

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AltMainActivity.this);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("first_use", false);
                editor.commit();

                Toast.makeText(AltMainActivity.this, "Device " + device.getSerialNumber() + " " + getString(R.string.connected), Toast.LENGTH_SHORT).show();

                sendBroadcast(new Intent(Constants.UPDATE_DEVICE_BROADCAST));

                AppWidgetManager widgetManager = AppWidgetManager.getInstance(AltMainActivity.this);
                ComponentName widgetComponent = new ComponentName(AltMainActivity.this, RokuAppWidgetProvider.class);
                int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
                Intent update = new Intent();
                update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
                update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                sendBroadcast(update);

                if (mListener != null) {
                    mListener.onDeviceSelected();
                }

                mAvailableDeviceAdapter.clear();
                mAdapter.notifyDataSetChanged();

                getLoaderManager().restartLoader(1, new Bundle(), mPairedDevicesLoader);
            }
        });

        mAdapter = new SeparatedListAdapter(this);
        mPairedDeviceAdapter = new DeviceAdapter(this, new ArrayList<Device>(), mHandler);
        mAvailableDeviceAdapter = new DeviceAdapter(this, new ArrayList<Device>(), mHandler);

        mAdapter.addSection("Paired devices", mPairedDeviceAdapter);
        mAdapter.addSection("Available devices", mAvailableDeviceAdapter);

        mList.setAdapter(mAdapter);

        refreshList(false);

        ImageButton refreshButton = (ImageButton) findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLoaderManager().restartLoader(0, new Bundle(), mAvailableDevicesLoader);
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_search) {
            SearchDialog fragment = SearchDialog.Companion.newInstance(this);
            fragment.show(getSupportFragmentManager(), SearchDialog.class.getName());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mViewPager.getCurrentItem() != 3) {
            return super.onKeyDown(keyCode, event);
        }

        if (mStoreFragment != null) {
            if (mStoreFragment.onKeyDown(keyCode, event)) {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        /*if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            //return PlaceholderFragment.newInstance(position + 1);

            if (position == 0) {
                return new MainFragment();
            } else if (position == 1) {
                return new RemoteFragment();
            } else if (position == 2) {
                return new ChannelFragment();
            } else {
                mStoreFragment = new StoreFragment();
                return mStoreFragment;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_devices);
                case 1:
                    return getString(R.string.title_remote);
                case 2:
                    return getString(R.string.title_channels);
                case 3:
                    return getString(R.string.title_store);
            }
            return null;
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationService.LocalBinder binder = (NotificationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void onDialogCancelled(DialogFragment dialog) {
        dialog.dismiss();
    }

    @Override
    public void onInstallSelected(DialogFragment dialog) {
        dialog.dismiss();
    }

    @Override
    public void onSearch(String searchText) {
        performSearch(searchText);
    }

    private void performSearch(String searchText) {
        String url = CommandHelper.getDeviceURL(this);

        SearchRequest searchRequest = new SearchRequest(url, searchText, null, null, null, null, null, null, null, null, null);
        JakuRequest request = new JakuRequest(searchRequest, null);

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {

            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {

            }
        }).execute(RokuRequestTypes.search);
    }

    private LoaderManager.LoaderCallbacks<List<Device>> mPairedDevicesLoader
            = new LoaderManager.LoaderCallbacks<List<Device>>() {

        @Override
        public Loader<List<Device>> onCreateLoader(int arg0, Bundle args) {
            return new PairedDevicesLoader(AltMainActivity.this, args);
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
            return new AvailableDevicesLoader(AltMainActivity.this, args);
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
        PopupMenu popup = new PopupMenu(this, v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Device device = (Device) v.getTag();

                switch (item.getItemId()) {
                    case R.id.action_info:
                        Intent intent = new Intent(AltMainActivity.this, DeviceInfoActivity.class);
                        intent.putExtra("serial_number", device.getSerialNumber());
                        intent.putExtra("host", device.getHost());
                        startActivity(intent);
                        return true;
                    case R.id.action_unpair:
                        PreferenceUtils.setConnectedDevice(AltMainActivity.this, "");
                        DBUtils.removeDevice(AltMainActivity.this, device.getSerialNumber());
                        refreshList(false);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.inflate(R.menu.device_menu);

        Device device = (Device) v.getTag();

        if (DBUtils.getDevice(this, device.getSerialNumber()) == null) {
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
