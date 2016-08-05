package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class UserPlaylistsActivity extends PlayerActivity {

    public static final String EXTRA_USER_URN = "userUrn";
    public static final String EXTRA_SEARCH_QUERY_SOURCE_INFO = "searchQuerySourceInfo";

    @Inject BaseLayoutHelper baseLayoutHelper;

    public UserPlaylistsActivity() {
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
        return Screen.USER_PLAYLISTS;
    }

    private void attachFragment() {
        final Urn userUrn = getIntent().getParcelableExtra(EXTRA_USER_URN);
        final Screen screen = Screen.fromIntent(getIntent());
        final SearchQuerySourceInfo searchQuerySourceInfo = getIntent()
                .getParcelableExtra(EXTRA_SEARCH_QUERY_SOURCE_INFO);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, UserPlaylistsFragment.create(userUrn, screen, searchQuerySourceInfo))
                .commit();
    }

}
