package wseemann.media.romote.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import wseemann.media.romote.R;

/**
 * Created by wseemann on 8/6/16.
 */
public class StoreFragment extends Fragment {

    private WebView mWebView;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_store, container, false);
        mWebView = (WebView) view.findViewById(R.id.webview);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWebView.loadUrl("https://channelstore.roku.com/browse");
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    }
}
