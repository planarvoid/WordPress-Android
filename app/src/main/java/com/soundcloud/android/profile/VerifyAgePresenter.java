package com.soundcloud.android.profile;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public class VerifyAgePresenter extends DefaultLightCycleActivity<Activity> implements MonthPickerDialogFragment.Callback {

    private static final String VERIFY_MONTH_DIALOG_TAG = "verify_month_dialog";

    @InjectView(R.id.verify_month) TextView monthInput;
    @InjectView(R.id.verify_year_input) EditText yearInput;
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
    }

    @OnClick(R.id.verify_month)
    public void monthTextListener() {
        FragmentActivity fragmentActivity = (FragmentActivity) activity;
        DialogFragment fragment = MonthPickerDialogFragment.build(getMonth());
        fragment.show(fragmentActivity.getSupportFragmentManager(), VERIFY_MONTH_DIALOG_TAG);
    }

    @Override
    public void onMonthSelected(String monthName, int monthOfYear) {
        monthInput.setTag(R.id.month_of_year_tag, monthOfYear);
        monthInput.setText(monthName);
        maybeEnableSubmitButton();
    }

    @OnTextChanged(R.id.verify_year_input)
    public void yearTextListener() {
        maybeEnableSubmitButton();
    }

    @OnClick(R.id.verify_button)
    public void submitButtonListener() {
        submitButton.setEnabled(false);
        updateAgeCommand.call(BirthdayInfo.buildFrom(getMonth(), getYear()), updateResponseHandler());
    }

    private void maybeEnableSubmitButton() {
        submitButton.setEnabled(BirthdayInfo.buildFrom(getMonth(), getYear()) != null);
    }

    private int getMonth() {
        Integer month = (Integer) monthInput.getTag(R.id.month_of_year_tag);
        return (month != null) ? month : 0;
    }

    private int getYear() {
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
