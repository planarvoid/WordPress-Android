package com.soundcloud.android.dialog.auth;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dialog.auth.AuthTaskFragment;
import com.soundcloud.android.task.auth.AuthTask;
import com.soundcloud.android.task.auth.GooglePlusSignInTask;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class GooglePlusSignInDialogFragment extends LoginTaskFragment {

    public static final String ARG_ACCT_NAME    = "account_name";
    public static final String ARG_REQ_CODE     = "request_code";

    private static final String GOOGLE_PLUS_SCOPE = "oauth2:https://www.googleapis.com/auth/userinfo.profile";

    private GooglePlusSignInTask mGooglePlusSignInTask;

    public static GooglePlusSignInDialogFragment create(String name, int requestCode) {
        Bundle b = new Bundle();
        b.putString(ARG_ACCT_NAME, name);
        b.putInt(ARG_REQ_CODE, requestCode);

        GooglePlusSignInDialogFragment fragment = new GooglePlusSignInDialogFragment();
        fragment.setArguments(b);
        return fragment;
    }

    @NotNull
    @Override
    AuthTask createAuthTask() {
        return new GooglePlusSignInTask(
                (SoundCloudApplication) getActivity().getApplication(),
                getArguments().getString(ARG_ACCT_NAME),
                GOOGLE_PLUS_SCOPE,
                getArguments().getInt(ARG_REQ_CODE));
    }

    @Override
    String getErrorFromResult(Activity activity, AuthTask.Result result) {
        Exception e = result.getException();

        if (e instanceof GooglePlayServicesAvailabilityException) {
            // GooglePlayServices.apk is either old, disabled, or not present.
            Dialog d = GooglePlayServicesUtil.getErrorDialog(
                    ((GooglePlayServicesAvailabilityException) e).getConnectionStatusCode(),
                    activity,
                    Consts.RequestCodes.REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
            d.show();
            return null;

        } else if (e instanceof UserRecoverableAuthException) {
            // Unable to authenticate, but the user can fix this.
            // Forward the user to the appropriate activity.
            Intent intent = ((UserRecoverableAuthException) e).getIntent();
            activity.startActivityForResult(intent, getArguments().getInt(ARG_REQ_CODE));
            return null;

        } else if (e instanceof GoogleAuthException) {
            return "Unrecoverable error " + e.getMessage();
        } else {
            return super.getErrorFromResult(activity, result);
        }

    }
}
