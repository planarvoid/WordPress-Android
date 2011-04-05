package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.R;

import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;

public class WebViewAuthorize extends Authorize {
    private WebView mWebView;

    @Override
    public void build() {
        setContentView(R.layout.web_view_authorize);
        String authorizeHtml;
        try {
            InputStream is = getAssets().open("authorize.html");
            StringBuilder sb = new StringBuilder();
            byte[] b = new byte[8192];
            int n;
            while ((n = is.read(b)) != -1) sb.append(new String(b, 0, n));
            authorizeHtml = sb.toString();
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mWebView = (WebView) findViewById(R.id.webview_legacy);
        mWebView.clearCache(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "shouldOverrideUrlLoading(" + url + ")");
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.d(TAG, "onLoadResource(" + url + ")");
                super.onLoadResource(view, url);
            }
        });
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new JSInterface(), "authorize");
        mWebView.loadDataWithBaseURL("file:///android_asset/", authorizeHtml, null, null, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public class JSInterface {
        public void submit(String login, String password) {
            login(login, password);
        }
    }
}
