package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class ViewAllRecommendedTracksActivity extends PlayerActivity {
    @Inject BaseLayoutHelper baseLayoutHelper;

    public ViewAllRecommendedTracksActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.activity_title_view_all_recommendations);

        if (savedInstanceState == null) {
            createFragmentForRecommendations();
        }
    }

    private void createFragmentForRecommendations() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new ViewAllRecommendedTracksFragment(), ViewAllRecommendedTracksFragment.TAG)
                .commit();
    }

    @Override
    public Screen getScreen() {
        return Screen.RECOMMENDATIONS_MAIN;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

}
