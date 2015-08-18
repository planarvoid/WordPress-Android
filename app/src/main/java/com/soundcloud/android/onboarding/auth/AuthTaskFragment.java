package com.soundcloud.android.onboarding.auth;

import static android.util.Log.INFO;
import static com.soundcloud.android.onboarding.OnboardingOperations.ONBOARDING_TAG;
import static com.soundcloud.android.utils.ErrorUtils.log;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.onboarding.auth.tasks.AuthTask;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskException;
import com.soundcloud.android.onboarding.auth.tasks.AuthTaskResult;
import com.soundcloud.android.onboarding.exceptions.SignInException;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.rx.eventbus.EventBus;
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
    @Inject ConfigurationOperations configurationOperations;
    @Inject EventBus eventBus;
    @Inject AccountOperations accountOperations;
    @Inject TokenInformationGenerator tokenUtils;
    @Inject ApiClient apiClient;
    @Inject LegacyUserStorage userStorage;

    public interface OnAuthResultListener {
        void onAuthTaskComplete(PublicApiUser user, SignupVia signupVia, boolean shouldAddUserInfo, boolean showFacebookSuggestions);

        void onError(String message, boolean allowFeedback);

        void onEmailTaken();

        void onSpam();

        void onBlocked();

        void onEmailInvalid();

        void onUsernameInvalid(String message);

        void onDeviceConflict(Bundle loginBundle);
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

        networkConnectionHelper = new NetworkConnectionHelper();
        task = createAuthTask();
        task.setTaskOwner(this);
        task.executeOnThreadPool(getArguments());
    }

    @Override @NotNull
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
        final Throwable rootException = ErrorUtils.removeTokenRetrievalException(result.getException());
        final boolean isNetworkUnavailable = !networkConnectionHelper.isNetworkConnected();
        
        if (result.wasServerError()) {
            return activity.getString(R.string.error_server_problems_message);
        } else if (result.wasNetworkError() && isNetworkUnavailable) {
            return activity.getString(R.string.authentication_error_no_connection_message);
        } else if (rootException instanceof AuthTaskException) {
            return ((AuthTaskException) rootException).getFirstError(); // message provided by the individual task
        } else {
            if (rootException == null) {
                logOnboardingError(genericExceptionFrom(result));
            } else {
                logOnboardingError(rootException);
            }
            return activity.getString(R.string.authentication_error_generic);
        }
    }

    private Throwable genericExceptionFrom(AuthTaskResult result) {
        return new SignInException(result.toString());
    }

    private void logOnboardingError(Throwable rootException) {
        log(INFO, ONBOARDING_TAG, getLoginErrorDebugMessage(rootException));
        ErrorUtils.handleSilentException("other sign in error while network connected", rootException);
    }

    private String getLoginErrorDebugMessage(Throwable rootException) {
        String exceptionMessage = rootException == null ? "no exception" : rootException.getMessage();
        return String.format(
                "other sign in error while network connected. Message: %s, Network type: %s, Operator name: %s",
                exceptionMessage,
                networkConnectionHelper.getCurrentConnectionType().toString(),
                networkConnectionHelper.getNetworkOperatorName()
        );
    }
    
    private void deliverResultAndDismiss() {
        final OnAuthResultListener listener = listenerRef.get();
        if (listener != null) {
            log(INFO, ONBOARDING_TAG, "auth result will be sent to listener: " + result.toString());

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
            } else if (result.wasDeviceConflict()) {
                listener.onDeviceConflict(result.getLoginBundle());
            } else if (result.wasValidationError()) {
                listener.onUsernameInvalid(result.getServerErrorMessage());
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
