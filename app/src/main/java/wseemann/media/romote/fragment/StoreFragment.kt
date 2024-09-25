package wseemann.media.romote.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import wseemann.media.romote.R

/**
 * Created by wseemann on 8/6/16.
 */
class StoreFragment : Fragment() {

    private lateinit var mWebView: WebView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_store, container, false)
        mWebView = view.findViewById<View>(R.id.webview) as WebView
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mWebView.loadUrl("https://channelstore.roku.com/browse")
        mWebView.webViewClient = WebViewClient()
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.javaScriptCanOpenWindowsAutomatically = true
    }

    fun onKeyDown(keyCode: Int): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return false
    }
}