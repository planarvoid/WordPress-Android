package com.soundcloud.android.activity.auth;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Google Plus based login. See
 * <a href="https://developers.google.com/+/mobile/android/getting-started">Google + Android Sign in</a>
 */
public class GooglePlusSignIn extends AbstractLoginActivity {

    private static final String SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";

    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;

    private String[] mGoogleAccountNames;
    private TextView mMessage;
    private Spinner mAccountNamesSpinner;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.google_plus_auth);

        mMessage = (TextView) findViewById(R.id.message);
        mGoogleAccountNames = getAccountNames();
        mAccountNamesSpinner = initializeSpinner(R.id.account_names_spinner, mGoogleAccountNames);
        initializeFetchButton(R.id.sign_in_btn);

    }

    public void show(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMessage.setText(message);
            }
        });
    }

    /**
     * This method is a hook for background threads and async tasks that need to launch a dialog.
     * It does this by launching a runnable under the UI thread.
     */
    public void showErrorDialog(final int code) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Dialog d = GooglePlayServicesUtil.getErrorDialog(
                        code,
                        GooglePlusSignIn.this,
                        REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                d.show();
            }
        });
    }

    private String[] getAccountNames() {
        Account[] accounts = AccountManager.get(this).getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        String[] names = new String[accounts.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = accounts[i].name;
        }
        return names;
    }

    private Spinner initializeSpinner(int id, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(GooglePlusSignIn.this,
                android.R.layout.simple_spinner_item, values);
        Spinner spinner = (Spinner) findViewById(id);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private void initializeFetchButton(int id) {
        findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int accountIndex = mAccountNamesSpinner.getSelectedItemPosition();
                if (accountIndex < 0) {
                    // this happens when the sample is run in an emulator which has no google account
                    // added yet.
                    show("No account available. Please add an account to the phone first.");
                    return;
                }

                new FetchName(GooglePlusSignIn.this, mGoogleAccountNames[accountIndex], SCOPE,
                        REQUEST_CODE_RECOVER_FROM_AUTH_ERROR).execute();
            }
        });
    }


    /**
     * Display personalized greeting. This class contains boilerplate code to consume the token but
     * isn't integral to getting the tokens.
     */
    public static class FetchName extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "TokenInfoTask";
        private static final String NAME_KEY = "given_name";
        protected GooglePlusSignIn mActivity;

        protected String mScope;
        protected String mEmail;
        protected int mRequestCode;

        FetchName(GooglePlusSignIn activity, String email, String scope, int requestCode) {
            mActivity = activity;
            mScope = scope;
            mEmail = email;
            mRequestCode = requestCode;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                fetchNameFromProfileServer();
            } catch (IOException ex) {
                onError("Following Error occured, please try again. " + ex.getMessage(), ex);
            } catch (JSONException e) {
                onError("Bad response: " + e.getMessage(), e);
            }
            return null;
        }

        protected void onError(String msg, Exception e) {
            if (e != null) {
                Log.e(TAG, "Exception: ", e);
            }
            mActivity.show(msg);
        }

        /**
         * Get a authentication token if one is not available. If the error is not recoverable then
         * it displays the error message on parent activity right away.
         */
        protected String fetchToken() throws IOException {
            try {
                return GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
            } catch (GooglePlayServicesAvailabilityException playEx) {
                // GooglePlayServices.apk is either old, disabled, or not present.
                mActivity.showErrorDialog(playEx.getConnectionStatusCode());
            } catch (UserRecoverableAuthException userRecoverableException) {
                // Unable to authenticate, but the user can fix this.
                // Forward the user to the appropriate activity.
                mActivity.startActivityForResult(userRecoverableException.getIntent(), mRequestCode);
            } catch (GoogleAuthException fatalException) {
                onError("Unrecoverable error " + fatalException.getMessage(), fatalException);
            }
            return null;
        }

        private void fetchNameFromProfileServer() throws IOException, JSONException {
            final String token = fetchToken();
            if (token != null) {
                final URL url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + token);
                final HttpURLConnection con = (HttpURLConnection) url.openConnection();
                final int sc = con.getResponseCode();
                if (sc == 200) {
                    final InputStream is = con.getInputStream();
                    final String name = new JSONObject(IOUtils.readInputStream(is)).getString(NAME_KEY);
                    mActivity.show("Hello " + name + "!");
                    is.close();
                } else if (sc == 401) {
                    GoogleAuthUtil.invalidateToken(mActivity, token);
                    onError("Server auth error, please try again.", null);
                    Log.i(TAG, "Server auth error: " + IOUtils.readInputStream(con.getErrorStream()));
                } else {
                    onError("Server returned the following error code: " + sc, null);
                }
            }
        }
    }

}