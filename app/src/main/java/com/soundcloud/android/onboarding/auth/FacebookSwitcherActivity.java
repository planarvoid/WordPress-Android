package com.soundcloud.android.onboarding.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Handles Facebook auth-flow: this activity first tries to use SSO to log in, falling
 * back to a WebView based login flow if FB app is not installed or fails to return a valid token.
 */
public class FacebookSwitcherActivity extends Activity {
    public static final int SSO = 1;
    public static final int WEBFLOW = 2;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (isSSOEnabled() && clientSupportsSSO()) {
            startSSOFlow();
        } else {
            Log.d(TAG, "SSO not possible, falling back to webview login");
            startWebFlow();
        }
    }

    /* package */ boolean clientSupportsSSO() {
        return FacebookSSOActivity.isSupported(this);
    }

    /* package */ boolean isSSOEnabled() {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK
            && requestCode == SSO
            && (data == null || (data.hasExtra("error") && !data.getBooleanExtra("canceled", false)))) {
                Log.d(TAG, "error using SSO: '" +
                        (data == null ? "<none>" : data.getStringExtra("error"))
                        + "', falling back to webview-based login");

                startWebFlow();
        } else {
            setResult(resultCode, data);
            finish();
        }
    }

    private void startSSOFlow() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "starting FB proxy auth");
        startActivityForResult(passExtras(new Intent(this, FacebookSSOActivity.class)), SSO);
    }

    private void startWebFlow() {
        startActivityForResult(passExtras(new Intent(this, FacebookWebFlowActivity.class)), WEBFLOW);
    }

    /**
     * Primarily to pass along the extra telling whether this action was triggered via {@link com.soundcloud.android.onboarding.OnboardActivity.StartState.SIGN_UP}
     */
    private Intent passExtras(Intent intent) {
        return getIntent().getExtras() != null ? intent.putExtras(getIntent().getExtras()) : intent;
    }
}