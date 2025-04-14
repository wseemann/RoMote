package wseemann.media.romote.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.request.SearchRequest;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.fragment.ChannelFragment;
import wseemann.media.romote.fragment.InstallChannelDialog;
import wseemann.media.romote.fragment.MainFragment;
import wseemann.media.romote.fragment.RemoteFragment;
import wseemann.media.romote.fragment.SearchDialog;
import wseemann.media.romote.fragment.StoreFragment;
import wseemann.media.romote.service.NotificationService;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.InputDeviceManager;


@AndroidEntryPoint
public class MainActivity extends ConnectivityActivity implements
        InstallChannelDialog.InstallChannelListener, SearchDialog.SearchDialogListener {

    @Inject
    protected SharedPreferences sharedPreferences;

    @Inject
    protected CommandHelper commandHelper;

    private InputDeviceManager mInputDeviceManager;

    private StoreFragment mStoreFragment;

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    boolean mBound = false;

    private ChannelFragment mChannelFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (sharedPreferences.getBoolean("first_use", true)) {
            startActivity(new Intent(this, ConfigureDeviceActivity.class));
            finish();
        }

        Intent intent = getIntent();

        if (intent != null && intent.getData() != null) {
            String channelCode = intent.getData().getPath().replace("/install/", "");

            InstallChannelDialog fragment = InstallChannelDialog.getInstance(this, channelCode);
            fragment.show(getSupportFragmentManager(), InstallChannelDialog.class.getName());
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {}
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            public void onPageSelected(int position) {
                if (mChannelFragment != null) {
                    mChannelFragment.refresh();
                }
            }
        });

        if (!commandHelper.getDeviceURL().equals("")) {
            mViewPager.setCurrentItem(1);
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        /*BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_devices) {
                    mViewPager.setCurrentItem(0);
                } else if (menuItem.getItemId() == R.id.action_remote) {
                    mViewPager.setCurrentItem(1);
                } else if (menuItem.getItemId() == R.id.action_channels) {
                    mViewPager.setCurrentItem(2);
                } else if (menuItem.getItemId() == R.id.action_store) {
                    mViewPager.setCurrentItem(3);
                }

                return false;
            }
        });*/

        // Bind to NotificationService
        Intent intent1 = new Intent(this, NotificationService.class);
        bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);

        mInputDeviceManager = new InputDeviceManager(this, commandHelper);
        if (!sharedPreferences.getBoolean("input_devices_enabled", true))
            mInputDeviceManager.setEnabled(false);
        mInputDeviceManager.
            setHardwareDevicesListener(new InputDeviceManager.HardwareDevicesListener() {
                    @Override
                    public void onIsEnabledWithDevices(boolean is_enabled, boolean has_devices) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("input_devices_enabled", is_enabled);
                        editor.commit();
                        invalidateOptionsMenu();
                    }
                });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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
    public void onResume() {
        super.onResume();
        mInputDeviceManager.resume();
    }

    @Override
    public void onPause() {
        mInputDeviceManager.pause();
        super.onPause();
    }

    @Override
    protected void onWifiConnected() {
        if (mChannelFragment != null) {
            mChannelFragment.refresh();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        if (mInputDeviceManager != null) {
            MenuItem menuItem = menu.findItem(R.id.action_input_devices);
            if (menuItem != null) {
                if (mInputDeviceManager.isEnabledWithDevices())
                    menuItem.setIcon(getDrawable(R.drawable.ic_videogame_asset));
                else
                    menuItem.setIcon(getDrawable(R.drawable.ic_videogame_asset_off));
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
        case R.id.action_settings:
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        case R.id.action_search:
            SearchDialog fragment = SearchDialog.Companion.newInstance(this);
            fragment.show(getSupportFragmentManager(), SearchDialog.class.getName());
            return true;
        case R.id.action_input_devices:
            if (mInputDeviceManager != null) {
                boolean is_enabled = !mInputDeviceManager.isEnabled();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("input_devices_enabled", is_enabled);
                editor.commit();

                mInputDeviceManager.setEnabled(is_enabled);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mInputDeviceManager.dispatchKeyEvent(event))
            return true;
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mViewPager.getCurrentItem() == 3 &&
            mStoreFragment != null &&
            mStoreFragment.onKeyDown(keyCode))
            return true;
        return super.onKeyDown(keyCode, event);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
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
                mChannelFragment = new ChannelFragment();
                return mChannelFragment;
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
        String url = commandHelper.getDeviceURL();

        SearchRequest searchRequest = new SearchRequest(url, searchText, null, null, null, null, null, null, null, null, null);
        searchRequest.sendAsync(new ResponseCallback<>() {
            @Override
            public void onSuccess(@Nullable Void unused) {

            }

            @Override
            public void onError(@NonNull Exception e) {

            }
        });
    }
}
