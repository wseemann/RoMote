package wseemann.media.romote.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.RomoteTheme

@Composable
fun LicensesScreen(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }

    val libraries by produceLibraries(R.raw.aboutlibraries)

    val filteredLibraries = remember(libraries, query) {
        val loaded = libraries
        when {
            loaded == null -> null

            query.isBlank() -> loaded

            else -> loaded.copy(
                libraries = loaded.libraries.filter { library ->
                    library.name.contains(query, ignoreCase = true) ||
                        library.uniqueId.contains(query, ignoreCase = true)
                },
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            RomoteTopAppBar(
                title = stringResource(R.string.open_source_licenses_title_preference),
                onBackClick = onBackClick,
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.action_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            LibrariesContainer(
                libraries = filteredLibraries,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview
@Composable
private fun LicensesScreenPreview() {
    RomoteTheme {
        LicensesScreen(onBackClick = {})
    }
}
