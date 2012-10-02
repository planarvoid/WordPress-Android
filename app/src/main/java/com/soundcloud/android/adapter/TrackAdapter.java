package com.soundcloud.android.adapter;


import static com.soundcloud.android.service.playback.CloudPlaybackService.PlayExtras;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.view.adapter.LazyRow;
import com.soundcloud.android.view.adapter.TrackInfoBar;
import com.soundcloud.android.view.quickaction.QuickAction;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends ScBaseAdapter<Track> {
    private QuickAction mQuickActionMenu;

    public TrackAdapter(Context context, Uri uri) {
        super(context, uri);
        mQuickActionMenu = new QuickTrackMenu(context, this);
    }

    @Override
    protected LazyRow createRow(int position) {
        return new TrackInfoBar(mContext,this);
    }

    @Override
    public QuickAction getQuickActionMenu() {
        return mQuickActionMenu;
    }

    @Override
    public void handleListItemClick(int position, long id) {
        Playable.PlayInfo info = new Playable.PlayInfo();
        info.uri      = mContentUri;
        info.position = position;

        List<Playable> playables = new ArrayList<Playable>(mData.size());

        for (Parcelable p : mData) {
            if (p instanceof Playable) {
                playables.add((Playable) p);
            } else {
                throw new AssertionError("No playable");
            }
        }

        info.playables = playables;

        Intent intent = new Intent(mContext, CloudPlaybackService.class).setAction(CloudPlaybackService.PLAY_ACTION);

        if (mContentUri != null) {
            SoundCloudApplication.MODEL_MANAGER.cache(info.getTrack());
            intent.putExtra(PlayExtras.trackId,      info.getTrack().id)
                  .putExtra(PlayExtras.playPosition, info.position)
                  .setData(info.uri);
        } else {
            CloudPlaybackService.playlistXfer = info.playables;

            intent.putExtra(PlayExtras.playPosition,      info.position)
                  .putExtra(PlayExtras.playFromXferCache, true);
        }

        mContext.startService(intent);
        mContext.startActivity(new Intent(mContext, ScPlayer.class));

    }
}
