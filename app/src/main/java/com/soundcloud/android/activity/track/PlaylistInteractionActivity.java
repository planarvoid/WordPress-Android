package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.Activity;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.PlayUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class PlaylistInteractionActivity extends PlayableInteractionActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: do we need different titles for playlists?
        if (mInteraction == Activity.Type.PLAYLIST_LIKE) {
            setTitle(R.string.list_header_track_likers);
        } else {
            setTitle(R.string.list_header_track_reposters);
        }

        mPlayableInfoBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaylistActivity.start(PlaylistInteractionActivity.this, (Playlist) mPlayable);
            }
        });

    }

    @Override
    protected Playable getPlayableFromIntent(Intent intent) {
        return Playlist.fromIntent(intent);
    }

    @Override
    protected Uri getContentUri() {
        Content content = mInteraction == Activity.Type.PLAYLIST_LIKE ? Content.PLAYLIST_LIKERS : Content.PLAYLIST_REPOSTERS;
        return content.forQuery(String.valueOf(mPlayable.id));
    }
}