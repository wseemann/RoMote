package wseemann.media.romote.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.activity.LicensesActivity;
import wseemann.media.romote.data.Device;
import wseemann.media.romote.event.SettingsScreenUiEvent;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.viewmodels.SettingsScreenViewModel;

@AndroidEntryPoint
public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SettingsScreenViewModel settingsScreenViewModel;

    @Inject
    protected PreferenceUtils preferenceUtils;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsScreenViewModel = new ViewModelProvider(this).get(SettingsScreenViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!deviceSupportsFindRemote()) {
            findPreference("find_remote").setEnabled(false);
        }

        findPreference("find_remote").setOnPreferenceClickListener(
                preference -> {
                    settingsScreenViewModel.onHandleEvent(SettingsScreenUiEvent.FindRemoteClickedEvent.INSTANCE);
                    return true;
                });

        findPreference("open_source_licenses").setOnPreferenceClickListener(
                preference -> {
                    startActivity(new Intent(requireContext(), LicensesActivity.class));
                    return true;
                });

        findPreference("donate").setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(Constants.PAYPAL_DONATION_LINK));
                    startActivity(intent);
                    return true;
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    private boolean deviceSupportsFindRemote() {
        try {
            Device device = preferenceUtils.getConnectedDevice();

            if (device.getSupportsFindRemote() != null) {
                return Boolean.parseBoolean(device.getSupportsFindRemote());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }
}
