package com.soundcloud.android.offline;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperimentStringHelper.ExperimentString;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.widget.TextView;

import javax.inject.Inject;

public class OfflineSettingsOnboardingActivity extends LoggedInActivity {

    @Inject @LightCycle OfflineSettingsOnboardingPresenter presenter;

    @Inject ChangeLikeToSaveExperimentStringHelper changeLikeToSaveExperimentStringHelper;

    @BindView(R.id.subtext) TextView subtext;

    public OfflineSettingsOnboardingActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.go_onboarding_settings);
    }

    @Override
    public Screen getScreen() {
        return Screen.SETTINGS_AUTOMATIC_SYNC_ONBOARDING;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ButterKnife.bind(this);
        subtext.setText(changeLikeToSaveExperimentStringHelper.getString(ExperimentString.GO_ONBOARDING_OFFLINE_SETTINGS_SUBTEXT));
    }
}
