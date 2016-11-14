package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

public class UserAlbumsActivity extends PlayerActivity {

    public static final String EXTRA_USER_URN = "userUrn";

    @Inject BaseLayoutHelper baseLayoutHelper;

    public UserAlbumsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            attachFragment();
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.USER_ALBUMS;
    }

    private void attachFragment() {
        final Urn userUrn = getIntent().getParcelableExtra(EXTRA_USER_URN);
        final Intent intent = getIntent();
        final Screen screen = Screen.fromIntent(intent);
        final SearchQuerySourceInfo searchQuerySourceInfo = intent
                .getParcelableExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, UserAlbumsFragment.create(userUrn, screen, searchQuerySourceInfo))
                .commit();
    }

}