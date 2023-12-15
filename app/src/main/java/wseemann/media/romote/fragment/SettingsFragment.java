package wseemann.media.romote.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.mikepenz.aboutlibraries.LibsBuilder;
import com.wseemann.ecp.api.ResponseCallback;
import com.wseemann.ecp.core.KeyPressKeyValues;
import com.wseemann.ecp.request.KeyPressRequest;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.PreferenceUtils;

@AndroidEntryPoint
public class SettingsFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    protected PreferenceUtils preferenceUtils;

    @Inject
    protected CommandHelper commandHelper;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!deviceSupportsFindRemote()) {
            findPreference("find_remote").setEnabled(false);
        }

        findPreference("find_remote").setOnPreferenceClickListener(
                preference -> {
                    performKeypress(KeyPressKeyValues.FIND_REMOTE);
                    return true;
                });

        findPreference("open_source_licenses").setOnPreferenceClickListener(
                preference -> {
                    // When the user selects an option to see the licenses:
                    new LibsBuilder()
                            .withActivityTitle(getString(R.string.open_source_licenses_title_preference))
                            .withSearchEnabled(true)
                            .start(requireContext());
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

    private void performKeypress(KeyPressKeyValues keyPressKeyValue) {
        String url = commandHelper.getDeviceURL();

        KeyPressRequest keypressRequest = new KeyPressRequest(url, keyPressKeyValue.getValue());
        keypressRequest.sendAsync(new ResponseCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void unused) {

            }

            @Override
            public void onError(@NonNull Exception e) {

            }
        });
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
