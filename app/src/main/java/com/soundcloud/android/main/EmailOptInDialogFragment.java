package com.soundcloud.android.main;

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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class EmailOptInDialogFragment extends DialogFragment {

    private static final String TAG = "email_opt_in";

    @Inject OnboardingOperations onboardingOperations;
    @Inject EventBus eventBus;

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
        AppCompatDialog dialog = new AppCompatDialog(getActivity());
        dialog.setTitle(R.string.optin_title);
        dialog.setContentView(R.layout.email_optin_fragment);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.optin_title)
                .setView(R.layout.email_optin_fragment)
                .setPositiveButton(R.string.optin_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.acceptEmailOptIn());
                        onboardingOperations.sendEmailOptIn();
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.optin_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.rejectEmailOptIn());
                        dismiss();
                    }
                })
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.dismissEmailOptIn());
    }

}
