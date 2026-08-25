package wseemann.media.romote.composables

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import wseemann.media.romote.composables.theme.RomoteTheme
import wseemann.media.romote.event.StoreScreenUiEvent
import wseemann.media.romote.model.StoreScreenUiState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StoreScreen(
    uiState: StoreScreenUiState,
    isCurrentPage: Boolean,
    onEvent: (StoreScreenUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = isCurrentPage && uiState.canGoBack) {
        webView?.goBack()
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            onEvent(StoreScreenUiEvent.PageStartedEvent)
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            onEvent(StoreScreenUiEvent.PageFinishedEvent)
                        }

                        override fun doUpdateVisitedHistory(
                            view: WebView,
                            url: String?,
                            isReload: Boolean
                        ) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            onEvent(StoreScreenUiEvent.HistoryChangedEvent(view.canGoBack()))
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    loadUrl(uiState.url)
                    webView = this
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StoreScreenPreview() {
    RomoteTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            StoreScreen(
                uiState = StoreScreenUiState(isLoading = true),
                isCurrentPage = true,
                onEvent = {}
            )
        }
    }
}
