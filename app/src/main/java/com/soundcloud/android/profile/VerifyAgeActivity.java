package com.soundcloud.android.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;

public class VerifyAgeActivity extends ScActivity implements MonthPickerDialogFragment.CallbackProvider {
    static final String EXTRA_USER_TO_FOLLOW_URN = "userToFollowUrn";

    @Inject VerifyAgePresenter presenter;
    @Inject UpdateAgeCommand updateAgeCommand;
    @Inject FollowingOperations followingOperations;

    public static Intent getIntent(Context context, Urn userToFollowUrn) {
        return new Intent(context, VerifyAgeActivity.class).putExtra(EXTRA_USER_TO_FOLLOW_URN, userToFollowUrn);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter.onCreate(this, savedInstanceState);
    }

    @Override
    public MonthPickerDialogFragment.Callback getMonthPickerCallback() {
        return presenter;
    }
}
