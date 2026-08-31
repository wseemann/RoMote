package wseemann.media.romote.composables

import androidx.compose.foundation.layout.RowScope
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
 * The purple app bar the XML screens used to get from AppBarLayout + Toolbar (see styles.xml ->
 * AppTheme.AppBarOverlay). The colors come from the theme rather than the `purple` color resource
 * so the bar follows RomoteTheme the way the rest of the Compose screens do.
 *
 * Hosted in a Scaffold's `topBar` slot, this also consumes the status bar inset, which is what
 * applyStatusBarTopPadding used to do for the XML app bars.
 *
 * [onBackClick] is null on the main screen, which is the root of the task and has nowhere to go up
 * to; every other screen passes one and gets the up arrow. [actions] is what MainActivity's options
 * menu (menu/main.xml) became.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomoteTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.navigate_up_content_description)
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
    )
}
