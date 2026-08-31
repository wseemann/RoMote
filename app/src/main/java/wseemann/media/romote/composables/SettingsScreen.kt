package wseemann.media.romote.composables

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.SettingsScreenUiEvent
import wseemann.media.romote.model.SettingsScreenUiState

/**
 * The settings screen: what SettingsFragment used to build out of res/xml/preferences.xml.
 *
 * The three switches are the CheckBoxPreferences; PreferenceFragmentCompat persisted those itself,
 * so here the writes go through SettingsScreenViewModel instead. Leaving the screen - to the
 * licenses list or the donation page - stays with the Activity, so this composable remains a pure
 * function of its state.
 */
@Composable
fun SettingsScreen(
    uiState: SettingsScreenUiState,
    onEvent: (SettingsScreenUiEvent) -> Unit,
    onLicensesClick: () -> Unit,
    onRateClick: () -> Unit,
    onDonateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RomoteTopAppBar(
                title = stringResource(R.string.title_settings),
                onBackClick = onBackClick
            )
        }
    ) { contentPadding ->
        // The padding goes inside the scrolling column so the rows pass under the navigation bar
        // rather than stopping short of it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
        ) {
            SettingsSwitchRow(
                title = stringResource(R.string.shake_to_pause_title_checkbox_preference),
                summary = stringResource(R.string.shake_to_pause_summary_checkbox_preference),
                checked = uiState.shakeToPauseEnabled,
                onCheckedChange = { onEvent(SettingsScreenUiEvent.ShakeToPauseToggledEvent(it)) }
            )

            SettingsSwitchRow(
                title = stringResource(R.string.notification_title_checkbox_preference),
                summary = stringResource(R.string.notification_to_pause_summary_checkbox_preference),
                checked = uiState.notificationWidgetEnabled,
                onCheckedChange = rememberNotificationWidgetToggle(onEvent)
            )

            SettingsSwitchRow(
                title = stringResource(R.string.haptic_feedback_title_checkbox_preference),
                summary = stringResource(R.string.haptic_feedback_summary_checkbox_preference),
                checked = uiState.hapticFeedbackEnabled,
                onCheckedChange = { onEvent(SettingsScreenUiEvent.HapticFeedbackToggledEvent(it)) }
            )

            SettingsCategoryHeader(title = stringResource(R.string.settings_other_category))

            SettingsActionRow(
                title = stringResource(R.string.find_remote_title_preference),
                summary = stringResource(R.string.find_remote_summary_preference),
                enabled = uiState.findRemoteSupported,
                onClick = { onEvent(SettingsScreenUiEvent.FindRemoteClickedEvent) }
            )

            SettingsActionRow(
                title = stringResource(R.string.open_source_licenses_title_preference),
                summary = null,
                onClick = onLicensesClick
            )

            SettingsActionRow(
                title = stringResource(R.string.rate_app_title_preference),
                summary = stringResource(R.string.rate_app_summary_preference),
                onClick = onRateClick
            )

            SettingsActionRow(
                title = stringResource(R.string.donate_title_preference),
                summary = stringResource(R.string.donate_summary_preference),
                onClick = onDonateClick
            )
        }
    }
}

/**
 * The notification widget switch, wrapped in the POST_NOTIFICATIONS request API 33 introduced.
 *
 * The switch is only allowed to latch on once the permission is actually held: NotificationService
 * posts through NotificationManager, which drops everything silently while the permission is
 * missing, so a switch that turned on regardless would be advertising a notification that never
 * arrives.
 *
 * @return the onCheckedChange to hand the switch.
 */
@Composable
private fun rememberNotificationWidgetToggle(
    onEvent: (SettingsScreenUiEvent) -> Unit
): (Boolean) -> Unit {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onEvent(SettingsScreenUiEvent.NotificationWidgetToggledEvent(true))
        } else {
            // A second refusal, or a refusal the user made permanently, comes straight back here
            // without a dialog ever appearing, so say why the switch didn't move.
            Toast.makeText(
                context,
                R.string.notification_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    return { enabled ->
        // Turning the preference off never needs the permission, and neither does anything below
        // API 33, where it is granted at install time.
        if (!enabled || hasNotificationPermission(context)) {
            onEvent(SettingsScreenUiEvent.NotificationWidgetToggledEvent(enabled))
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Stands in for PreferenceCategory, which drew its title in the accent color above the group.
 */
@Composable
private fun SettingsCategoryHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    HorizontalDivider()

    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsListItem(
        title = title,
        summary = summary,
        enabled = true,
        trailingContent = {
            // The whole row is the toggle, so the Switch itself takes no click of its own.
            Switch(checked = checked, onCheckedChange = null)
        },
        modifier = modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
    )
}

@Composable
private fun SettingsActionRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    SettingsListItem(
        title = title,
        summary = summary,
        enabled = enabled,
        trailingContent = null,
        modifier = modifier.clickable(enabled = enabled, onClick = onClick)
    )
}

/**
 * ListItem's container defaults to colorScheme.surface, which RomoteTheme leaves at the tinted
 * Material 3 default rather than the window background it pins for `background` - see
 * DeviceInfoScreen for the same note. Staying transparent keeps every row on the Activity's
 * background, the way the preference list used to sit.
 *
 * ListItem has no disabled state of its own, so a disabled row dims its own text.
 */
@Composable
private fun SettingsListItem(
    title: String,
    summary: String?,
    enabled: Boolean,
    trailingContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val contentAlpha = if (enabled) 1f else 0.38f

    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = summary?.let { { Text(text = it) } },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    RomoteTheme {
        SettingsScreen(
            uiState = SettingsScreenUiState(
                shakeToPauseEnabled = true,
                notificationWidgetEnabled = false,
                hapticFeedbackEnabled = true,
                findRemoteSupported = true
            ),
            onEvent = {},
            onLicensesClick = {},
            onRateClick = {},
            onDonateClick = {},
            onBackClick = {}
        )
    }
}
