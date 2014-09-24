package com.soundcloud.android.main;

import static android.view.View.OnClickListener;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import eu.inmite.android.lib.dialogs.SimpleDialogFragment;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class EmailOptInDialogFragment extends SimpleDialogFragment {

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
        new EmailOptInDialogFragment().show(activity.getSupportFragmentManager(), TAG);
    }

    @Override
    protected Builder build(Builder builder) {
        builder.setTitle(R.string.optin_title);
        builder.setView(LayoutInflater.from(getActivity()).inflate(R.layout.email_optin_fragment, null));
        setupButtons(builder);
        return builder;
    }

    private void setupButtons(Builder builder) {
        builder.setPositiveButton(R.string.optin_yes, new OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.acceptEmailOptIn());
                onboardingOperations.sendEmailOptIn();
                dismiss();
            }
        });
        builder.setNegativeButton(R.string.optin_no, new OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.rejectEmailOptIn());
                dismiss();
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        eventBus.publish(EventQueue.ONBOARDING, OnboardingEvent.dismissEmailOptIn());
    }

}
