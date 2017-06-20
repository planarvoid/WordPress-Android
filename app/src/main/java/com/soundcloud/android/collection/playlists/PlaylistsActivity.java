package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

public class PlaylistsActivity extends PlayerActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject FeatureFlags featureFlags;

    public static final String EXTRA_PLAYLISTS_AND_ALBUMS = "extraPlaylistsAndAlbums";
    private static final String EXTRA_PLAYLISTS_ONLY = "extraPlaylistsOnly";
    private static final String EXTRA_ALBUMS_ONLY = "extraAlbumsOnly";

    public PlaylistsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public static Intent intentForPlaylistsAndAlbums(Context context) {
        return new Intent(context, PlaylistsActivity.class).putExtra(EXTRA_PLAYLISTS_AND_ALBUMS, true);
    }

    public static Intent intentForPlaylists(Context context) {
        return new Intent(context, PlaylistsActivity.class).putExtra(EXTRA_PLAYLISTS_ONLY, true);
    }

    public static Intent intentForAlbums(Context context) {
        return new Intent(context, PlaylistsActivity.class).putExtra(EXTRA_ALBUMS_ONLY, true);
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

        setActivityTitle();
    }

    private void setActivityTitle() {
        if (entities() == PlaylistsOptions.Entities.PLAYLISTS) {
            setTitle(R.string.collections_playlists_separate_header);
        } else if (entities() == PlaylistsOptions.Entities.ALBUMS) {
            setTitle(R.string.collections_albums_header);
        } else {
            setTitle(R.string.collections_playlists_header);
        }
    }

    private PlaylistsOptions.Entities entities() {
        if (shouldShowPlaylistsOnly()) {
            return PlaylistsOptions.Entities.PLAYLISTS;
        } else if (shouldShowAlbumsOnly()) {
            return PlaylistsOptions.Entities.ALBUMS;
        } else {
            return PlaylistsOptions.Entities.PLAYLISTS_AND_ALBUMS;
        }
    }

    boolean shouldShowPlaylistsOnly() {
        return getIntent().getBooleanExtra(EXTRA_PLAYLISTS_ONLY, false);
    }

    boolean shouldShowAlbumsOnly() {
        return getIntent().getBooleanExtra(EXTRA_ALBUMS_ONLY, false);
    }

    @Override
    public Screen getScreen() {
        return Screen.PLAYLISTS;
    }

    private void attachFragment() {
        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.container, PlaylistsFragment.create(entities()))
                                   .commit();
    }
}
