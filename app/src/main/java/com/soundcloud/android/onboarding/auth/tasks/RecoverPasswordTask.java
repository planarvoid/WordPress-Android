package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.onboarding.auth.RecoverPasswordOperations;

import android.content.res.Resources;
import android.os.AsyncTask;

public class RecoverPasswordTask extends AsyncTask<String, Void, Boolean> {
    private final Resources resources;
    private final RecoverPasswordOperations recoverPasswordOperations;
    protected String reason;

    protected RecoverPasswordTask(Resources resources,
                                  RecoverPasswordOperations recoverPasswordOperations) {
        this.resources = resources;
        this.recoverPasswordOperations = recoverPasswordOperations;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        ApiResponse apiResponse = recoverPasswordOperations.recoverPassword(params[0]);

        if (apiResponse.isNotSuccess() && apiResponse.getFailure().reason() == ApiRequestException.Reason.VALIDATION_ERROR) {
            reason = resources.getString(R.string.authentication_recover_password_unknown_email_address);
        }

        return apiResponse.isSuccess();
    }
}
