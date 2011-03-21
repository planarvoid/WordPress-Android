
package com.soundcloud.android.activity;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.CloudAPI;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.WriteState;
import com.soundcloud.android.objects.User;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.concurrent.Semaphore;

/**
 * Adopted from UrbanStew's soundclouddroid authorization process
 *
 * @http://code.google.com/p/soundclouddroid/
 */
public class Authorize extends Activity implements CloudAPI.Client {
    private WebView mWebView;
    private Exception mAuthorizationException;
    private Semaphore mVerificationCodeAvailable;
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

        ((SoundCloudApplication) getApplication()).authorizeWithoutCallback(this);
        showDialog(CloudUtils.Dialogs.DIALOG_AUTHENTICATION_CONTACTING);
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

    @Override
    public void authorizationCompleted(final AuthorizationStatus status) {
        if (status == AuthorizationStatus.CANCELED)
            return;

        runOnUiThread(new Runnable() {
            public void run() {
                if (status == AuthorizationStatus.SUCCESSFUL) {
                    Intent intent = new Intent(Authorize.this, Main.class);
                    startActivity(intent);
                    finish();
                } else {
                    String message = "";
                    if (mAuthorizationException != null) {
                        if (mAuthorizationException.getLocalizedMessage() != null) {
                            message += "\n\n" + mAuthorizationException.getLocalizedMessage() + ".";
                        } else {
                            message += "Authorization failed with exception "
                                    + mAuthorizationException.getClass().getName();
                        }
                    }
                    if (!isFinishing()) {
                        new AlertDialog.Builder(Authorize.this)
                                .setTitle(R.string.authorization_failed)
                                .setMessage(message)
                                .setCancelable(false)
                                .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                }).create().show();
                    }
                }
            }
        });

    }

    @Override
    public void exceptionOccurred(Exception e) {
        Log.i(TAG, "Exception Occured " + e.toString());
        mAuthorizationException = e;
    }

    @Override
    public String getVerificationCode() {
        try {
            mVerificationCodeAvailable.acquire();
            return mVerificationCode;
        } catch (InterruptedException e) {
            Log.w(Authorize.class.getSimpleName(), e);
            return null;
        }
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

    @Override
    public void storeUser(User me, String token, String secret) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit()
                .putLong(SoundCloudApplication.USER_ID, me.id)
                .putString(SoundCloudApplication.USERNAME, me.username)
                .putString(SoundCloudApplication.TOKEN, token)
                .putString(SoundCloudApplication.SECRET, secret)
                .putBoolean(SoundCloudApplication.EMAIL_CONFIRMED, me.primary_email_confirmed)
                .commit();

        SoundCloudDB.getInstance().resolveUser(getContentResolver(), me, WriteState.all, me.id);
    }


    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case CloudUtils.Dialogs.DIALOG_AUTHENTICATION_CONTACTING:
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setTitle(R.string.authentication_contacting_title);
                dialog.setMessage(getResources().getString(
                        R.string.authentication_contacting_message));
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                return dialog;

            default:
                return super.onCreateDialog(which);

        }
    }
}
