package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackProperty;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

public class TrackInteractionActivity extends PlayableInteractionActivity {


    @Inject PlaybackOperations playbackOperations;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        SoundCloudApplication.getObjectGraph().inject(this);

        switch (interaction) {
            case TRACK_LIKE:
                setTitle(R.string.list_header_track_likers);
                break;
            case TRACK_REPOST:
                setTitle(R.string.list_header_track_reposters);
                break;
            case COMMENT:
                setTitle(R.string.list_header_track_comments);
                break;
            default:
                throw new IllegalArgumentException("Unexpected track interation: " + interaction);
        }

        findViewById(R.id.playable_bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if it comes from a mention, might not have a user
                playbackOperations.startPlaybackWithRecommendations(propertySet.get(TrackProperty.URN), getCurrentScreen());
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

    protected Screen getCurrentScreen() {
        switch (interaction) {
            case TRACK_LIKE:
                return Screen.PLAYER_LIKES;
            case TRACK_REPOST:
                return Screen.PLAYER_REPOSTS;
            case COMMENT:
                return Screen.PLAYER_COMMENTS;
            default:
                throw new IllegalArgumentException("Unexpected track interation: " + interaction);
        }
    }

    @Override
    protected Uri getContentUri() {
        Content content = null;
        switch (interaction) {
            case TRACK_LIKE:
                content = Content.TRACK_LIKERS;
                break;
            case TRACK_REPOST:
                content = Content.TRACK_REPOSTERS;
                break;
            case COMMENT:
                content = Content.TRACK_COMMENTS;
                break;
        }
        return content.forId(propertySet.get(TrackProperty.URN).numericId);
    }
}