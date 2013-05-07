package com.soundcloud.android.dialog.auth;


import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.auth.AuthTask;
import com.soundcloud.android.task.auth.AuthTaskException;
import com.soundcloud.android.task.auth.AuthTaskResult;
import com.soundcloud.api.CloudAPI;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import java.lang.ref.WeakReference;

public abstract class AuthTaskFragment extends DialogFragment {

    private AuthTask mTask;
    private AuthTaskResult mResult;
    private WeakReference<OnAuthResultListener> mListenerRef;

    public interface OnAuthResultListener {
        void onAuthTaskComplete(User user, SignupVia signupVia, boolean shouldAddUserInfo);
        void onError(String message);
    }

    @NotNull
    abstract AuthTask   createAuthTask();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mTask = createAuthTask();
        mTask.setTaskOwner(this);
        mTask.executeOnThreadPool(getArguments());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.authentication_login_progress_message));
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListenerRef = new WeakReference<OnAuthResultListener>((OnAuthResultListener) activity);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAuthResultListener");
        }
    }

    @Override
    public void onDestroyView()
    {
        final Dialog dialog = getDialog();

        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        if ((dialog != null) && getRetainInstance()) dialog.setDismissMessage(null);

        super.onDestroyView();
    }

    @Override
    public void onDismiss(DialogInterface dialog)
    {
        super.onDismiss(dialog);
        if (mTask != null) mTask.cancel(false);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // Dismiss if the task finished while we were away
        if (mTask == null) deliverResultAndDismiss();
    }

    public void onTaskResult(AuthTaskResult result) {
        mTask = null;
        mResult = result;
        // Don't try to dismiss if we aren't in the foreground
        if (isResumed()) deliverResultAndDismiss();
    }

    protected String getErrorFromResult(Activity activity, AuthTaskResult result){
        final Exception exception = result.getException();
        if (exception instanceof CloudAPI.ApiResponseException) {
            // server error, tell them to try again later
            return activity.getString(R.string.error_server_problems_message);
        } else if (exception instanceof AuthTaskException){
            // custom exception, message provided by the individual task
            return ((AuthTaskException) exception).getFirstError();
        } else {
            // as a fallback, just say connection problem
            return activity.getString(R.string.authentication_error_no_connection_message);
        }
    }

    private void deliverResultAndDismiss(){
        final OnAuthResultListener listener = mListenerRef.get();
        if (listener != null){
            if (mResult.wasSuccess()){
                listener.onAuthTaskComplete(mResult.getUser(), mResult.getSignupVia(),
                        this instanceof SignupTaskFragment);
            } else {
                listener.onError(getErrorFromResult((Activity) listener, mResult));
            }
        }
        dismiss();
    }

}
