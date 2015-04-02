package com.soundcloud.android.main;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class EmailOptInDialogFragment extends DialogFragment {

    private static final String TAG = "email_opt_in";

    @Inject OnboardingOperations onboardingOperations;
    @Inject EventBus eventBus;

    private final MaterialDialog.ButtonCallback buttonCallback = new MaterialDialog.ButtonCallback() {
        @Override
        public void onPositive(MaterialDialog dialog) {
            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.acceptEmailOptIn());
            onboardingOperations.sendEmailOptIn();
            dismiss();
        }

        @Override
        public void onNegative(MaterialDialog dialog) {
            eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.rejectEmailOptIn());
            dismiss();
        }
    };

    public EmailOptInDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    EmailOptInDialogFragment(OnboardingOperations onboardingOperations, EventBus eventBus) {
        this.onboardingOperations = onboardingOperations;
        this.eventBus = eventBus;
    }

    public static void show(FragmentActivity activity) {
        new EmailOptInDialogFragment().show(activity.getFragmentManager(), TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialDialog.Builder(getActivity())
                .title(R.string.optin_title)
                .customView(R.layout.email_optin_fragment, false)
                .positiveText(R.string.optin_yes)
                .negativeText(R.string.optin_no)
                .callback(buttonCallback)
                .build();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.dismissEmailOptIn());
    }

}
