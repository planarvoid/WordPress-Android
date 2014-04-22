package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.activities.Activity;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.storage.provider.Content;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class PlaylistInteractionActivity extends PlayableInteractionActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // TODO: do we need different titles for playlists?
        if (mInteraction == Activity.Type.PLAYLIST_LIKE) {
            setTitle(R.string.list_header_track_likers);
        } else {
            setTitle(R.string.list_header_track_reposters);
        }

        findViewById(R.id.playable_bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistDetailActivity.start(
                        PlaylistInteractionActivity.this, (Playlist) mPlayable,
                        SoundCloudApplication.sModelManager, getCurrentScreen());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            mEventBus.publish(EventQueue.SCREEN_ENTERED, getCurrentScreen().get());
        }
    }

    @Override
    protected Screen getCurrentScreen() {
        if (mInteraction == Activity.Type.PLAYLIST_LIKE) {
            return Screen.PLAYLIST_LIKES;
        } else {
            return Screen.PLAYLIST_REPOSTS;
        }
    }

    @Override
    protected Playable getPlayableFromIntent(Intent intent) {
        Bundle bundle = intent.getExtras();
        Playlist playlist;
        if (bundle.containsKey(Playlist.EXTRA)) {
            playlist = bundle.getParcelable(Playlist.EXTRA);
        } else if (bundle.containsKey(Playlist.EXTRA_ID)) {
            playlist = SoundCloudApplication.sModelManager.getPlaylist(bundle.getLong(Playlist.EXTRA_ID, 0));
        } else if (bundle.containsKey(Playlist.EXTRA_URI)) {
            Uri uri1 = bundle.getParcelable(Playlist.EXTRA_URI);
            playlist = SoundCloudApplication.sModelManager.getPlaylist(uri1);
        } else {
            throw new IllegalArgumentException("Could not obtain playlist from bundle");
        }
        return playlist;
    }

    @Override
    protected Uri getContentUri() {
        Content content = mInteraction == Activity.Type.PLAYLIST_LIKE ? Content.PLAYLIST_LIKERS : Content.PLAYLIST_REPOSTERS;
        return content.forQuery(String.valueOf(mPlayable.getId()));
    }
}