package com.soundcloud.android.profile;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EngagementsTracking;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import javax.inject.Inject;

public class VerifyAgePresenter extends DefaultActivityLightCycle<Activity> {

    @BindView(R.id.verify_age_input) EditText yearInput;
    @BindView(R.id.verify_button) Button submitButton;

    private final UpdateAgeCommand updateAgeCommand;
    private final FollowingOperations followingOperations;
    private final EngagementsTracking engagementsTracking;
    private final ScreenProvider screenProvider;

    private Activity activity;
    private Urn userToFollowUrn;

    @Inject
    VerifyAgePresenter(UpdateAgeCommand updateAgeCommand,
                       FollowingOperations followingOperations,
                       EngagementsTracking engagementsTracking,
                       ScreenProvider screenProvider) {
        this.updateAgeCommand = updateAgeCommand;
        this.followingOperations = followingOperations;
        this.engagementsTracking = engagementsTracking;
        this.screenProvider = screenProvider;
    }

    @Override
    public void onCreate(Activity activity, @Nullable Bundle bundle) {
        this.activity = activity;
        activity.setContentView(R.layout.verify_age);

        ButterKnife.bind(this, activity);

        userToFollowUrn = activity.getIntent().getParcelableExtra(VerifyAgeActivity.EXTRA_USER_TO_FOLLOW_URN);

        submitButton.setEnabled(false);
        yearInput.requestFocus();
    }

    @OnTextChanged(R.id.verify_age_input)
    public void yearTextListener() {
        maybeEnableSubmitButton();
    }

    @OnClick(R.id.verify_button)
    public void submitButtonListener() {
        submitButton.setEnabled(false);
        updateAgeCommand.call(BirthdayInfo.buildFrom(getAge()), updateResponseHandler());
    }

    private void maybeEnableSubmitButton() {
        submitButton.setEnabled(yearInput.getText().length() > 0);
    }

    private int getAge() {
        return (int) ScTextUtils.safeParseLong(yearInput.getText().toString());
    }

    private DefaultSubscriber<Boolean> updateResponseHandler() {
        return new DefaultSubscriber<Boolean>() {
            @Override
            public void onNext(Boolean success) {
                fireAndForget(followingOperations.toggleFollowing(userToFollowUrn, true));
                engagementsTracking.followUserUrn(userToFollowUrn,
                                                  true,
                                                  EventContextMetadata.builder()
                                                                      .module(Module.create(Module.SINGLE, 0))
                                                                      .pageName(screenProvider.getLastScreen().get())
                                                                      .build());
                activity.finish();
            }
        };
    }

}
