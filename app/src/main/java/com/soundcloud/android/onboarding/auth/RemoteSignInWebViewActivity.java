package com.soundcloud.android.onboarding.auth;

import com.soundcloud.android.R;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.view.CustomFontTitleToolbar;

import android.os.Bundle;

public class RemoteSignInWebViewActivity extends WebViewActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureToolbar();
    }

    private void configureToolbar() {
        CustomFontTitleToolbar customFontTitleToolbar = (CustomFontTitleToolbar) findViewById(R.id.toolbar_id);
        customFontTitleToolbar.setTitle(getString(R.string.remote_signin_activity_title));
        customFontTitleToolbar.setNavigationIcon(R.drawable.close_icon);
        customFontTitleToolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.remote_signin_web_view);
    }

    @Override
    protected boolean shouldWebViewGoBack() {
        // Tapping the back button just close the activity
        return false;
    }
}
