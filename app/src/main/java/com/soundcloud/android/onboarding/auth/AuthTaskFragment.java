package com.soundcloud.android.onboarding.auth;


import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskException;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.CloudAPI;
import org.jetbrains.annotations.NotNull;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import java.lang.ref.WeakReference;

public abstract class AuthTaskFragment extends DialogFragment {
    private static final String TAG = AuthTaskFragment.class.getSimpleName();

    private AuthTask task;
    private AuthTaskResult result;
    private WeakReference<OnAuthResultListener> listenerRef;

    public interface OnAuthResultListener {
        void onAuthTaskComplete(PublicApiUser user, SignupVia signupVia, boolean shouldAddUserInfo, boolean showFacebookSuggestions);
        void onError(String message);
        void onEmailTaken();
        void onSpam();
        void onBlocked();
        void onEmailInvalid();
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
    @TargetApi(11)
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            dialog = new ProgressDialog(getActivity(), AlertDialog.THEME_HOLO_DARK);
        } else {
            dialog = new ProgressDialog(getActivity());
        }

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
            listenerRef = new WeakReference<OnAuthResultListener>((OnAuthResultListener) activity);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAuthResultListener");
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
        final Exception exception = result.getException();
        if (exception instanceof CloudAPI.ApiResponseException) {
            // server error, tell them to try again later
            return activity.getString(R.string.error_server_problems_message);
        } else if (exception instanceof AuthTaskException) {
            // custom exception, message provided by the individual task
            return ((AuthTaskException) exception).getFirstError();
        } else {
            // as a fallback, just say connection problem
            if (exception != null) {
                Log.d(TAG, "Received unexpected error/exception : " + exception.toString());
                exception.printStackTrace();
            }
            return activity.getString(R.string.authentication_error_no_connection_message);
        }
    }

    private void deliverResultAndDismiss() {
        final OnAuthResultListener listener = listenerRef.get();
        if (listener != null) {
            if (result.wasSuccess()) {
                listener.onAuthTaskComplete(result.getUser(), result.getSignupVia(),
                        this instanceof SignupTaskFragment, result.getShowFacebookSuggestions());
            } else if (result.wasEmailTaken()) {
                listener.onEmailTaken();
            } else if (result.wasSpam()) {
                listener.onSpam();
            } else if (result.wasDenied()) {
                listener.onBlocked();
            } else if (result.wasEmailInvalid()) {
                listener.onEmailInvalid();
            } else {
                listener.onError(getErrorFromResult((Activity) listener, result));
            }
        }
        dismiss();
    }

}
