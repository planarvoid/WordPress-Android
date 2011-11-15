package com.soundcloud.android.activity.auth;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Handles Facebook auth-flow: this activity first tries to use SSO to log in, falling
 * back to a WebView based login flow if FB app is not installed or fails to return a valid token.
 */
public class Facebook extends Activity {
    public static final int SSO = 1;
    public static final int WEBFLOW = 2;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (clientSupportsSSO()) {
            startSSOFlow();
        } else {
            Log.d(TAG, "SSO not possible, falling back to webview login");
            startWebFlow();
        }
    }

    /* package */ boolean clientSupportsSSO() {
        return FacebookSSO.validateAppSignatureForIntent(this, FacebookSSO.getAuthIntent(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SSO:
                if (data == null || data.hasExtra("error")) {
                    Log.d(TAG, "error using SSO: '"+
                            (data == null ? "<none>" : data.getStringExtra("error"))
                            +"', falling back to webview-based login");
                    startWebFlow();
                } else {
                    setResult(resultCode, data);
                    finish();
                }

                break;
            /* WebFlow result, just forward back to caller */
            default:
                setResult(resultCode, data);
                finish();
                break;
        }
    }

    private void startSSOFlow() {
        Log.d(TAG, "starting FB proxy auth");
        startActivityForResult(new Intent(this, FacebookSSO.class), SSO);
    }

    private void startWebFlow() {
        startActivityForResult(new Intent(this, FacebookWebFlow.class), WEBFLOW);
    }
}