package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.activities.Activity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.storage.provider.Content;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class PlaylistInteractionActivity extends PlayableInteractionActivity {

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // TODO: do we need different titles for playlists?
        if (interaction == Activity.Type.PLAYLIST_LIKE) {
            setTitle(R.string.list_header_track_likers);
        } else {
            setTitle(R.string.list_header_track_reposters);
        }

        findViewById(R.id.playable_bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistDetailActivity.start(PlaylistInteractionActivity.this, propertySet.get(PlayableProperty.URN),
                        getCurrentScreen());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.SCREEN_ENTERED, getCurrentScreen().get());
        }
    }

    @Override
    protected Screen getCurrentScreen() {
        if (interaction == Activity.Type.PLAYLIST_LIKE) {
            return Screen.PLAYLIST_LIKES;
        } else {
            return Screen.PLAYLIST_REPOSTS;
        }
    }

    @Override
    protected Uri getContentUri() {
        Content content = interaction == Activity.Type.PLAYLIST_LIKE ? Content.PLAYLIST_LIKERS : Content.PLAYLIST_REPOSTERS;
        return content.forQuery(String.valueOf(propertySet.get(PlayableProperty.URN).numericId));
    }
}