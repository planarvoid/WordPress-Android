package com.soundcloud.android.profile;

import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Urn;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import javax.inject.Inject;

public class VerifyAgeActivity extends ScActivity {
    static final String EXTRA_USER_TO_FOLLOW_URN = "userToFollowUrn";

    @Inject VerifyAgePresenter presenter;


    @Inject UpdateAgeCommand updateAgeCommand;
    @Inject FollowingOperations followingOperations;

    @InjectView(R.id.verify_month_spinner) Spinner monthInput;
    @InjectView(R.id.verify_year_input) EditText yearInput;
    @InjectView(R.id.verify_button) Button submitButton;

    public static Intent getIntent(Context context, Urn userToFollowUrn) {
        return new Intent(context, VerifyAgeActivity.class).putExtra(EXTRA_USER_TO_FOLLOW_URN, userToFollowUrn);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter.onCreate(this, savedInstanceState);
    }

}
