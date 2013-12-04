package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.Event;
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
                publishScreenEnteredEvent(bundle, Screen.PLAYER_LIKES);
                break;
            case TRACK_REPOST:
                setTitle(R.string.list_header_track_reposters);
                publishScreenEnteredEvent(bundle, Screen.PLAYER_REPOSTS);
                break;
            case COMMENT:
                setTitle(R.string.list_header_track_comments);
                publishScreenEnteredEvent(bundle, Screen.PLAYER_COMMENTS);
                break;
        }

        mPlaybackOperations = new PlaybackOperations();
        mPlayableInfoBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if it comes from a mention, might not have a user
                if (mPlayable.user != null) {
                    mPlaybackOperations.playTrack(TrackInteractionActivity.this, (Track) mPlayable);
                }
            }
        });
    }

    private void publishScreenEnteredEvent(Bundle savedInstanceState, Screen screen) {
        boolean wasConfigurationChange = savedInstanceState != null;
        if (!wasConfigurationChange) {
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