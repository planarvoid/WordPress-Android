
package com.soundcloud.android.activity;

import java.util.concurrent.Semaphore;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.utils.SoundCloudAuthorizationClient;

/**
 * Adopted from UrbanStew's soundclouddroid authorization process
 * 
 * @http://code.google.com/p/soundclouddroid/
 */

public class Authorize extends ScActivity implements SoundCloudAuthorizationClient {

    private static final String TAG = "Authorize";

    private SharedPreferences mPreferences;

    WebView mWebView;

    private Exception mAuthorizationException;

    Semaphore mVerificationCodeAvailable;

    private Handler mHandler = new Handler();

    private String mVerificationCode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorize);
        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setBlockNetworkImage(false);
        mWebView.getSettings().setLoadsImagesAutomatically(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("soundcloud://auth")) {
                    mVerificationCode = Uri.parse(url).getQueryParameter("oauth_verifier");
                    mVerificationCodeAvailable.release();
                    return true;
                }
                return false;
            }
        });

        mVerificationCodeAvailable = new Semaphore(0);
        this.getSoundCloudApplication().authorizeWithoutCallback(this);

        safeShowDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_CONTACTING);

    }

    @Override
    public void authorizationCompleted(final AuthorizationStatus status) {
        if (status == AuthorizationStatus.CANCELED)
            return;

        runOnUiThread(new Runnable() {
            public void run() {
                if (status == AuthorizationStatus.SUCCESSFUL) {
                    // showToast("Authorization Successful");
                    Intent intent = new Intent(Authorize.this, Dashboard.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    String message = "";
                    if (mAuthorizationException != null) {
                        // if(mAuthorizationException.getCause() != null)
                        // message += " (" + mAuthorizationException..getCause()
                        // + ")";

                        if (mAuthorizationException.getLocalizedMessage() != null) {
                            message += "\n\n" + mAuthorizationException.getLocalizedMessage() + ".";
                        } else {
                            message += "Authorization failed with exception "
                                    + mAuthorizationException.getClass().getName();
                        }
                    }
                    new AlertDialog.Builder(Authorize.this).setTitle("Authorization Failed")
                            .setMessage(message).setCancelable(false).setPositiveButton("OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            finish();
                                        }
                                    }).create().show();
                }
            }
        });

    }

    @Override
    public void exceptionOccurred(Exception e) {
        Log.i(TAG, "Exception Occured " + e.toString());
        e.printStackTrace();
        mAuthorizationException = e;

    }

    @Override
    public String getVerificationCode() {
        try {
            mVerificationCodeAvailable.acquire();
        } catch (InterruptedException e) {
            Log.v(Authorize.class.getSimpleName(), Log.getStackTraceString(e));
            return null;
        }
        return mVerificationCode;
    }

    @Override
    public void openAuthorizationURL(String url) {
        mHandler.removeCallbacks(mRemoveDialog);
        mHandler.postDelayed(mRemoveDialog, 2000);
        mWebView.loadUrl(url);
    }

    // Stop a call request after some amount of time
    private Runnable mRemoveDialog = new Runnable() {
        public void run() {
            removeDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_CONTACTING);
        }
    };

}
