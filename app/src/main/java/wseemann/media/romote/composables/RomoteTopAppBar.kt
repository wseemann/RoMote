package wseemann.media.romote.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import wseemann.media.romote.R

/**
 * The purple app bar with an up arrow that the XML screens get from AppBarLayout + Toolbar
 * (see styles.xml -> AppTheme.AppBarOverlay). The colors come from the theme rather than the
 * `purple` color resource so the bar follows RomoteTheme the way the rest of the Compose screens do.
 *
 * Hosted in a Scaffold's `topBar` slot, this also consumes the status bar inset, which is what
 * applyStatusBarTopPadding used to do for the XML app bars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomoteTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_up_content_description)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
}
