package wseemann.media.romote.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.persistentListOf
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.data.Entry
import wseemann.media.romote.model.DeviceInfoScreenUiState

/**
 * There is nothing to interact with here beyond leaving, so unlike the other screens this one takes
 * no `onEvent` - DeviceInfoScreenViewModel starts its one query as soon as it is created.
 */
@Composable
fun DeviceInfoScreen(uiState: DeviceInfoScreenUiState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            RomoteTopAppBar(
                title = stringResource(R.string.title_device_info),
                onBackClick = onBackClick
            )
        }
    ) { contentPadding ->
        DeviceInfoContent(uiState = uiState, contentPadding = contentPadding)
    }
}

@Composable
private fun DeviceInfoContent(
    uiState: DeviceInfoScreenUiState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (uiState.entries.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = stringResource(R.string.no_device_info),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        return
    }

    // The padding goes to the list rather than around it, so the rows scroll under the navigation
    // bar instead of stopping short of it.
    LazyColumn(
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize()
    ) {
        items(uiState.entries, key = { entry -> entry.key }) { entry ->
            DeviceInfoRow(entry = entry)
            HorizontalDivider()
        }
    }
}

@Composable
private fun DeviceInfoRow(entry: Entry, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(text = "${entry.key}:") },
        supportingContent = { Text(text = entry.value) },
        // ListItem's container defaults to colorScheme.surface, which RomoteTheme leaves at the
        // tinted Material 3 default (#FEF7FF / #141218) rather than the window background it pins
        // for `background`. Staying transparent keeps every row on the Activity's background.
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DeviceInfoScreenPreview() {
    RomoteTheme {
        DeviceInfoScreen(
            uiState = DeviceInfoScreenUiState(
                entries = persistentListOf(
                    Entry("udn", "29ee43a2-9a7c-51e0-9d4c-fc3c48b5f2a1"),
                    Entry("serial-number", "X00500ABCDEF"),
                    Entry("vendor-name", "Roku"),
                    Entry("model-name", "Roku Ultra"),
                    Entry("software-version", "13.0.0")
                ),
                isLoading = false
            ),
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceInfoScreenLoadingPreview() {
    RomoteTheme {
        DeviceInfoScreen(uiState = DeviceInfoScreenUiState(), onBackClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceInfoScreenEmptyPreview() {
    RomoteTheme {
        DeviceInfoScreen(uiState = DeviceInfoScreenUiState(isLoading = false), onBackClick = {})
    }
}
