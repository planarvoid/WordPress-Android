package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.playback.PlaybackOperations;

import android.content.Intent;
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
                if (playable.user != null) {
                    playbackOperations.playTrack(TrackInteractionActivity.this, (Track) playable, getCurrentScreen());
                }
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
    protected Playable getPlayableFromIntent(Intent intent) {
        // I inlined this lookup from a method I removed from the Track class, since I wanted to get rid of
        // the dependency to ScModelManager in Track.java
        if (intent == null) throw new IllegalArgumentException("intent is null");
        Track track = intent.getParcelableExtra(Track.EXTRA);
        if (track == null) {
            track = SoundCloudApplication.sModelManager.getTrack(intent.getLongExtra(Track.EXTRA_ID, 0));
        }
        if (track == null) {
            throw new IllegalArgumentException("Could not obtain track from intent " + intent);
        }
        return track;
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
        return content.forId(playable.getId());
    }
}