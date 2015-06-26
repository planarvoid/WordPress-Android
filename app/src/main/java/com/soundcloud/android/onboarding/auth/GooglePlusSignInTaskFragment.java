package com.soundcloud.android.onboarding.auth;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.auth.tasks.GooglePlusSignInTask;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.api.CloudAPI;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;

public class GooglePlusSignInTaskFragment extends AuthTaskFragment {

    public static final String ARG_ACCT_NAME    = "account_name";
    public static final String ARG_REQ_CODE     = "request_code";

    private static final String GOOGLE_PLUS_SCOPE = "oauth2:https://www.googleapis.com/auth/plus.login https://www.googleapis.com/auth/userinfo.email";

    public static Bundle getParams(String name, int requestCode) {
        Bundle b = new Bundle();
        b.putString(ARG_ACCT_NAME, name);
        b.putInt(ARG_REQ_CODE, requestCode);
        return b;
    }

    public static GooglePlusSignInTaskFragment create(Bundle params) {
        GooglePlusSignInTaskFragment fragment = new GooglePlusSignInTaskFragment();
        fragment.setArguments(params);
        return fragment;
    }

    @NotNull
    @Override
    AuthTask createAuthTask() {
        return new GooglePlusSignInTask(
                (SoundCloudApplication) getActivity().getApplication(),
                getArguments().getString(ARG_ACCT_NAME),
                GOOGLE_PLUS_SCOPE, configurationOperations, eventBus, accountOperations, tokenUtils);
    }

    @Override
    protected String getErrorFromResult(Activity activity, AuthTaskResult result) {
        Throwable rootException = ErrorUtils.removeTokenRetrievalException(result.getException());

        if (rootException instanceof GooglePlayServicesAvailabilityException) {
            // GooglePlayServices.apk is either old, disabled, or not present.
            Dialog d = GooglePlayServicesUtil.getErrorDialog(
                    ((GooglePlayServicesAvailabilityException) rootException).getConnectionStatusCode(),
                    activity,
                    Consts.RequestCodes.RECOVER_FROM_PLAY_SERVICES_ERROR);
            d.show();
            return null;

        } else if (rootException instanceof UserRecoverableAuthException) {
            // Unable to authenticate, but the user can fix this.
            // Forward the user to the appropriate activity.
            Intent intent = ((UserRecoverableAuthException) rootException).getIntent();
            activity.startActivityForResult(intent, getArguments().getInt(ARG_REQ_CODE));
            return null;

        } else if (result.wasUnauthorized()) {
            // Normally this indicates that we could not swap the Google token for a SoundCloud API token, which can
            // happen if users try to sign in via G+ without actually having a G+ account
            // NOTE that using a dev build signed with a debug key that is NOT registered with the G+ client ID will
            // also raise this error.
            return activity.getString(R.string.error_google_sign_in_failed);
        } else if (rootException instanceof GoogleAuthException) {
            return "Unrecoverable error " + rootException.getMessage();
        } else {
            return super.getErrorFromResult(activity, result);
        }

    }
}
