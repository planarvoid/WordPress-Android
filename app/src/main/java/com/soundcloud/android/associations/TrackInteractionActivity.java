package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.Event;
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
    private Screen screen;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        switch (mInteraction) {
            case TRACK_LIKE:
                screen = Screen.PLAYER_LIKES;
                setTitle(R.string.list_header_track_likers);
                break;
            case TRACK_REPOST:
                screen = Screen.PLAYER_REPOSTS;
                setTitle(R.string.list_header_track_reposters);
                break;
            case COMMENT:
                screen = Screen.PLAYER_COMMENTS;
                setTitle(R.string.list_header_track_comments);
                break;
            default:
                throw new IllegalArgumentException("Unexpected track interation: " + mInteraction);
        }

        publishScreenEnteredEvent(screen);

        mPlaybackOperations = new PlaybackOperations();
        mPlayableInfoBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if it comes from a mention, might not have a user
                if (mPlayable.user != null) {
                    mPlaybackOperations.playTrack(TrackInteractionActivity.this, (Track) mPlayable, screen);
                }
            }
        });
    }

    private void publishScreenEnteredEvent(Screen screen) {
        if (!isConfigurationChange()) {
            Event.SCREEN_ENTERED.publish(screen.get());
        }
    }

    @Override
    protected Playable getPlayableFromIntent(Intent intent) {
        return Track.fromIntent(intent);
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