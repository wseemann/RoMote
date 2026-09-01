package wseemann.media.romote.composables

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import wseemann.media.romote.R
import wseemann.media.romote.composables.theme.OnPurple
import wseemann.media.romote.composables.theme.Purple

/**
 * Hosted in a Scaffold's `topBar` slot, this also consumes the status bar inset.
 *
 * [onBackClick] is null on the main screen, which is the root of the task and has nowhere to go up
 * to; every other screen passes one and gets the up arrow.
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
            // Named directly rather than taken from the primary role: the bar stays this purple in
            // both themes, and the dark scheme's primary is a light tone meant for foregrounds.
            containerColor = Purple,
            titleContentColor = OnPurple,
            navigationIconContentColor = OnPurple,
            actionIconContentColor = OnPurple
        ),
        modifier = modifier
    )
}
