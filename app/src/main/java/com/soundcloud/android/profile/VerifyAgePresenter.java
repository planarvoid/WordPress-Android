package com.soundcloud.android.profile;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import butterknife.OnTextChanged;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.crop.util.VisibleForTesting;
import com.soundcloud.android.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ScTextUtils;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import javax.inject.Inject;
import java.util.Calendar;

public class VerifyAgePresenter extends DefaultActivityLightCycle {
    @InjectView(R.id.verify_month_spinner) Spinner monthInput;
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
    public void onCreate(FragmentActivity activity, @Nullable Bundle bundle) {
        this.activity = activity;
        activity.setContentView(R.layout.verify_age);

        ButterKnife.inject(this, activity);

        userToFollowUrn = activity.getIntent().getParcelableExtra(VerifyAgeActivity.EXTRA_USER_TO_FOLLOW_URN);

        monthInput.setAdapter(ArrayAdapter.createFromResource(activity, R.array.birth_month_options, R.layout.verify_age_month));

        submitButton.setEnabled(false);
    }

    @OnItemSelected(R.id.verify_month_spinner)
    public void onMonthSelected() {
        maybeEnableSubmitButton();
    }

    @OnItemSelected(value = R.id.verify_month_spinner, callback = OnItemSelected.Callback.NOTHING_SELECTED)
    public void onNothingSelected() {
        maybeEnableSubmitButton();
    }

    @OnTextChanged(R.id.verify_year_input)
    public void yearTextListener() {
        maybeEnableSubmitButton();
    }

    @OnClick(R.id.verify_button)
    public void submitButtonListener() {
        submitButton.setEnabled(false);
        updateAgeCommand.call(new BirthdayInfo(getMonth(), getYear()), updateResponseHandler());
    }

    private void maybeEnableSubmitButton() {
        submitButton.setEnabled(validMonthAndYear(getMonth(), getYear()));
    }

    private int getMonth() {
        return monthInput.getSelectedItemPosition();
    }

    private int getYear() {
        return (int) ScTextUtils.safeParseLong(yearInput.getText().toString());
    }

    @VisibleForTesting
    static boolean validMonthAndYear(int month, int year) {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        return month > 0 && year >= (currentYear - 100) && year < (currentYear - 13);
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
