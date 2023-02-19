package wseemann.media.romote.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.jaku.core.JakuRequest;
import com.jaku.core.KeypressKeyValues;
import com.jaku.request.KeypressRequest;
import com.mikepenz.aboutlibraries.LibsBuilder;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import wseemann.media.romote.R;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.RokuRequestTypes;

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
                    performKeypress(KeypressKeyValues.FIND_REMOTE);
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

    private void performKeypress(KeypressKeyValues keypressKeyValue) {
        String url = commandHelper.getDeviceURL();

        KeypressRequest keypressRequest = new KeypressRequest(url, keypressKeyValue.getValue());
        JakuRequest request = new JakuRequest(keypressRequest, null);

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {

            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {

            }
        }).execute(RokuRequestTypes.keypress);
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
