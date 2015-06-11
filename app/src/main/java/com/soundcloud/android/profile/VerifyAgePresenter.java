package com.soundcloud.android.profile;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import javax.inject.Inject;

public class VerifyAgePresenter extends DefaultLightCycleActivity<Activity> {

    @InjectView(R.id.verify_age_input) EditText yearInput;
    @InjectView(R.id.verify_button) Button submitButton;

    private final UpdateAgeCommand updateAgeCommand;
    private final FollowingOperations followingOperations;

    private Activity activity;
    private Urn userToFollowUrn;

    @Inject
    VerifyAgePresenter(UpdateAgeCommand updateAgeCommand, FollowingOperations followingOperations) {
        this.updateAgeCommand = updateAgeCommand;
        this.followingOperations = followingOperations;
    }

    @Override
    public void onCreate(Activity activity, @Nullable Bundle bundle) {
        this.activity = activity;
        activity.setContentView(R.layout.verify_age);

        ButterKnife.inject(this, activity);

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
                fireAndForget(followingOperations.addFollowing(new PublicApiUser(userToFollowUrn.toString())));
                activity.finish();
            }
        };
    }

}
