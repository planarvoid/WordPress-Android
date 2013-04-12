package com.soundcloud.android.task.auth;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.soundcloud.android.Consts;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;

public class GooglePlusSignInTask extends AsyncTask<String, Void, String> {

    private static final String TAG = "TokenInfoTask";

    protected WeakReference<Listener> mListenerRef;
    protected String mScope;
    protected int mRequestCode;
    private Exception mException;

    public interface Listener {
        Activity getActivity();
        void onGPlusToken(String token);
        void onGPlusError(String message);
    }

    public GooglePlusSignInTask(Listener listener, String scope, int requestCode) {
        setListener(listener);
        mScope = scope;
        mRequestCode = requestCode;
    }

    public void setListener(Listener listener){
        mListenerRef = new WeakReference<Listener>(listener);
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            Listener listener = mListenerRef.get();
            if (listener != null){
                return GoogleAuthUtil.getToken(listener.getActivity().getApplicationContext(), params[0], mScope);
            }
        } catch (Exception ex) {
            mException = ex;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String token) {
        Listener listener = mListenerRef.get();
        if (listener != null){
            if (token != null) {
                listener.onGPlusToken(token);

            } else if (mException != null) {
                Log.e(TAG, "Exception: ", mException);
                String message = null;
                if (mException instanceof GooglePlayServicesAvailabilityException) {
                    // GooglePlayServices.apk is either old, disabled, or not present.
                    Dialog d = GooglePlayServicesUtil.getErrorDialog(
                            ((GooglePlayServicesAvailabilityException) mException).getConnectionStatusCode(),
                            listener.getActivity(),
                            Consts.RequestCodes.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    d.show();

                } else if (mException instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, but the user can fix this.
                    // Forward the user to the appropriate activity.
                    Intent intent = ((UserRecoverableAuthException) mException).getIntent();
                    listener.getActivity().startActivityForResult(intent, mRequestCode);


                } else if (mException instanceof GoogleAuthException) {
                    message = "Unrecoverable error " + mException.getMessage();
                } else {
                    message = "Following Error occured, please try again. " + mException.getMessage();
                }
                listener.onGPlusError(message);
            }
        }
    }
}
