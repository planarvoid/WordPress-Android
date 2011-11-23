package com.soundcloud.android.adapter;

import android.view.View;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.model.Friend;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TracklistRow;
import com.soundcloud.android.view.TracklistSectionedRow;
import com.soundcloud.android.view.UserlistSectionedRow;

import android.os.Parcelable;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import java.util.ArrayList;

public class SectionedTracklistAdapter extends SectionedAdapter implements ITracklistAdapter {

    private long mPlayingId = -1;
    private boolean mIsPlaying = false;
    private QuickTrackMenu mQuickTrackMenu;

    public SectionedTracklistAdapter(ScActivity activity) {
        super(activity);
        mQuickTrackMenu = new QuickTrackMenu(activity, this);
    }

    @Override protected LazyRow createRow(int position) {
        return new TracklistSectionedRow(mContext, this);
    }

    public Track getTrackAt(int index) {
        return (Track) mData.get(index);
    }

    @Override
    public boolean isPlaying() {
        return mIsPlaying;
    }

    @Override
    public long getPlayingId() {
        return mPlayingId;
    }

    public void setPlayingId(long currentTrackId, boolean isPlaying) {
        this.mPlayingId = currentTrackId;
        this.mIsPlaying = isPlaying;
    }

    public void addFavorite(Track track) {
        SoundCloudApplication app = SoundCloudApplication.fromContext(mContext);
        if (app != null){
            FavoriteAddTask f = new FavoriteAddTask(app);
            f.setOnFavoriteListener(mFavoriteListener);
            f.execute(track);
        }

    }

    public void removeFavorite(Track track) {
        SoundCloudApplication app = SoundCloudApplication.fromContext(mContext);
        if (app != null){
        FavoriteRemoveTask f = new FavoriteRemoveTask(app);
        f.setOnFavoriteListener(mFavoriteListener);
        f.execute(track);
        }
    }

    @Override
    public QuickTrackMenu getQuickTrackMenu() {
        return mQuickTrackMenu;
    }

    private FavoriteTask.FavoriteListener mFavoriteListener = new FavoriteTask.FavoriteListener() {
        @Override
        public void onNewFavoriteStatus(long trackId, boolean isFavorite) {
            notifyDataSetChanged();
        }

        @Override
        public void onException(long trackId, Exception e) {
            notifyDataSetChanged();
        }
    };
}
