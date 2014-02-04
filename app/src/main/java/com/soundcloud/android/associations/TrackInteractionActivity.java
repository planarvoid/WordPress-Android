package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.playback.PlaybackOperations;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class TrackInteractionActivity extends PlayableInteractionActivity {

    private PlaybackOperations mPlaybackOperations;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        switch (mInteraction) {
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
                throw new IllegalArgumentException("Unexpected track interation: " + mInteraction);
        }

        mPlaybackOperations = new PlaybackOperations();
        findViewById(R.id.playable_bar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if it comes from a mention, might not have a user
                if (mPlayable.user != null) {
                    mPlaybackOperations.playTrack(TrackInteractionActivity.this, (Track) mPlayable, getCurrentScreen());
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            EventBus.SCREEN_ENTERED.publish(getCurrentScreen().get());
        }
    }

    protected Screen getCurrentScreen() {
        switch (mInteraction) {
            case TRACK_LIKE:
                return Screen.PLAYER_LIKES;
            case TRACK_REPOST:
                return Screen.PLAYER_REPOSTS;
            case COMMENT:
                return Screen.PLAYER_COMMENTS;
            default:
                throw new IllegalArgumentException("Unexpected track interation: " + mInteraction);
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
        switch (mInteraction) {
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
        return content.forId(mPlayable.getId());
    }
}