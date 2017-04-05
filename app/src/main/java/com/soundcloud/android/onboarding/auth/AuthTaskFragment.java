package com.soundcloud.android.onboarding.auth;

import static android.util.Log.INFO;
import static com.soundcloud.android.onboarding.OnboardActivity.ONBOARDING_TAG;
import static com.soundcloud.android.utils.ErrorUtils.log;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.onboarding.auth.tasks.AgeRestrictionAuthResult;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskException;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public abstract class AuthTaskFragment extends DialogFragment {
    private AuthTask task;
    private AuthTaskResult result;
    private WeakReference<OnAuthResultListener> listenerRef;

    @Inject NetworkConnectionHelper networkConnectionHelper;
    @Inject AccountOperations accountOperations;
    @Inject ApiClient apiClient;
    @Inject StoreUsersCommand storeUsersCommand;
    @Inject SyncInitiatorBridge syncInitiatorBridge;
    @Inject SignInOperations signInOperations;
    @Inject SignUpOperations signUpOperations;

    public interface OnAuthResultListener {
        void onAuthTaskComplete(ApiUser user, SignupVia signupVia, boolean shouldAddUserInfo);

        void onError(String message, boolean allowFeedback);

        void onEmailTaken();

        void onSpam();

        void onBlocked();

        void onEmailInvalid();

        void onUsernameInvalid(String message);

        void onDeviceConflict(Bundle loginBundle);

        void onDeviceBlock();

        void onAgeRestriction(String minimumAge);
    }

    protected AuthTaskFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @NotNull
    abstract AuthTask createAuthTask();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setCancelable(false);

        task = createAuthTask();
        task.setTaskOwner(this);
        task.executeOnThreadPool(getArguments());
    }

    @Override
    @NotNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity(), AlertDialog.THEME_HOLO_DARK);
        dialog.setMessage(getString(R.string.authentication_login_progress_message));
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listenerRef = new WeakReference<>((OnAuthResultListener) activity);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement OnAuthResultListener");
        }
    }

    @Override
    public void onDestroyView() {
        final Dialog dialog = getDialog();

        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        if ((dialog != null) && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }

        super.onDestroyView();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (task != null) {
            task.cancel(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Dismiss if the task finished while we were away
        if (task == null) {
            deliverResultAndDismiss();
        }
    }

    public void onTaskResult(AuthTaskResult result) {
        task = null;
        this.result = result;
        // Don't try to dismiss if we aren't in the foreground
        if (isResumed()) {
            deliverResultAndDismiss();
        }
    }

    protected String getErrorFromResult(Activity activity, AuthTaskResult result) {
        final Throwable rootException = ErrorUtils.removeTokenRetrievalException(result.getException());
        final boolean isNetworkUnavailable = !networkConnectionHelper.isNetworkConnected();
        
        if (result.wasServerError()) {
            return activity.getString(R.string.error_server_problems_message);
        } else if (result.wasNetworkError() && isNetworkUnavailable) {
            return activity.getString(R.string.authentication_error_no_connection_message);
        } else if (rootException instanceof AuthTaskException) {
            return ((AuthTaskException) rootException).getFirstError(); // message provided by the individual task
        } else {
            return activity.getString(R.string.authentication_error_generic);
        }
    }

    private void deliverResultAndDismiss() {
        final OnAuthResultListener listener = listenerRef.get();
        if (listener != null) {
            log(INFO, ONBOARDING_TAG, "auth result will be sent to listener: " + result);

            if (result.wasSuccess()) {
                listener.onAuthTaskComplete(result.getAuthResponse().me.getUser(), result.getSignupVia(), this instanceof SignupTaskFragment);
            } else if (result.wasEmailTaken()) {
                listener.onEmailTaken();
            } else if (result.wasSpam()) {
                listener.onSpam();
            } else if (result.wasDenied()) {
                listener.onBlocked();
            } else if (result.wasEmailInvalid()) {
                listener.onEmailInvalid();
            } else if (result.wasDeviceConflict()) {
                listener.onDeviceConflict(result.getLoginBundle());
            } else if (result.wasDeviceBlock()) {
                listener.onDeviceBlock();
            } else if (result.wasValidationError()) {
                listener.onUsernameInvalid(result.getErrorMessage());
            } else if (result.wasAgeRestricted()) {
                listener.onAgeRestriction(((AgeRestrictionAuthResult) result).getMinimumAge());
            } else {
                listener.onError(getErrorFromResult((Activity) listener, result), shouldAllowFeedback(result));
            }
        } else {
            log(INFO, ONBOARDING_TAG, "auth result listener is gone, when delivering result");
        }
        dismiss();
    }

    private boolean shouldAllowFeedback(AuthTaskResult result) {
        return networkConnectionHelper.isNetworkConnected() && result.wasUnexpectedError();
    }
}
