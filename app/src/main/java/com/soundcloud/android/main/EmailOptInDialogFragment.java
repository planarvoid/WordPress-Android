package com.soundcloud.android.main;

import static android.view.View.OnClickListener;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.onboarding.OnboardingOperations;
import eu.inmite.android.lib.dialogs.SimpleDialogFragment;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;

import javax.inject.Inject;

public class EmailOptInDialogFragment extends SimpleDialogFragment {

    private static final String TAG = "email_opt_in";

    @Inject
    OnboardingOperations onboardingOperations;

    public EmailOptInDialogFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
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
                onboardingOperations.sendEmailOptIn();
                dismiss();
            }
        });
        builder.setNegativeButton(R.string.optin_no, new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

}
