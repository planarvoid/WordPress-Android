package com.soundcloud.android.playlists;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class PlaylistDetailActivity extends ScActivity {

    static final String LOG_TAG = "PlaylistDetails";

    @Deprecated
    public static void start(Context context, @NotNull PublicApiPlaylist playlist, ScModelManager modelManager, Screen screen) {
        modelManager.cache(playlist);
        context.startActivity(getIntent(playlist.getUrn(), screen));
    }

    public static void start(Context context, PlaylistUrn playlistUrn, Screen screen) {
        context.startActivity(getIntent(playlistUrn, screen));
    }

    public static Intent getIntent(@NotNull PlaylistUrn playlistUrn, Screen screen) {
        Intent intent = new Intent(Actions.PLAYLIST);
        screen.addToIntent(intent);
        return intent.putExtra(PublicApiPlaylist.EXTRA_URN, playlistUrn);
    }

    public PlaylistDetailActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.activity_title_playlist);
        setContentView(R.layout.playlist_activity);

        if (savedInstanceState == null) {
            createFragmentForPlaylist();
        }
    }

    private void createFragmentForPlaylist() {
        Bundle extras = getIntent().getExtras();
        Log.d(LOG_TAG, "(Re-)creating fragment for " + extras.getParcelable(PublicApiPlaylist.EXTRA_URN));
        Fragment fragment = PlaylistFragment.create(extras);
        getSupportFragmentManager().beginTransaction().replace(R.id.playlist_tracks_fragment, fragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.PLAYLIST_DETAILS.get());
        }
    }
}
