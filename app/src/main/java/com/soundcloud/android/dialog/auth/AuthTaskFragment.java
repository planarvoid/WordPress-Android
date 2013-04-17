package com.soundcloud.android.dialog.auth;


import com.soundcloud.android.R;
import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.auth.AuthTask;
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
    private AuthTask.Result mResult;
    private WeakReference<OnAuthResultListener> mListenerRef;

    public interface OnAuthResultListener {
        void onAccountAdded(User user, SignupVia signupVia);
        void onError(String message);
    }

    @NotNull
    abstract AuthTask   createAuthTask();
    abstract Bundle     getTaskParams();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mTask = createAuthTask();
        mTask.setFragment(this);

        mTask.execute(getTaskParams());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.authentication_login_progress_message));
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
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

    public void onTaskResult(AuthTask.Result result) {
        mTask = null;
        mResult = result;
        // Don't try to dismiss if we aren't in the foreground
        if (isResumed()) deliverResultAndDismiss();
    }

    private void deliverResultAndDismiss(){
        final OnAuthResultListener listener = mListenerRef.get();
        if (listener != null){
            if (mResult.wasSuccess()){
                listener.onAccountAdded(mResult.getUser(), mResult.getSignupVia());
            } else {
                listener.onError(getErrorFromResult((Activity) listener, mResult));
            }
        }

        dismiss();
    }

    protected String getErrorFromResult(Activity activity, AuthTask.Result result){
        final Exception exception = result.getException();
        int messageId;
        if (exception instanceof CloudAPI.ApiResponseException
                && ((CloudAPI.ApiResponseException) exception).getStatusCode() >= 400) {
            messageId = R.string.error_server_problems_message;
        } else {
            messageId = R.string.authentication_error_no_connection_message;
        }
        return activity.getString(messageId);
    }

}
