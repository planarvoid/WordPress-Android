package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.PlayUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class TrackInteractionActivity extends PlayableInteractionActivity {
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
        }

        mPlayableInfoBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if it comes from a mention, might not have a user
                if (mPlayable.user != null) {
                    PlayUtils.playTrack(TrackInteractionActivity.this, new PlayInfo((Track) mPlayable));
                }
            }
        });
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