package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TracklistRow;

import android.os.Parcelable;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import java.util.ArrayList;

public class TracklistAdapter extends LazyBaseAdapter implements ITracklistAdapter {
    public static final String TAG = "TracklistAdapter";

    private long mPlayingId = -1;
    private boolean mIsPlaying = false;
    private QuickTrackMenu mQuickTrackMenu;

    public TracklistAdapter(ScActivity activity, ArrayList<Parcelable> data, Class<?> model) {
        super(activity, data, model);
        mQuickTrackMenu = new QuickTrackMenu(activity, this);
    }

    @Override
    protected LazyRow createRow(int position) {
        return new TracklistRow(mActivity, this);
    }

    @Override
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
        FavoriteAddTask f = new FavoriteAddTask(mActivity.getApp());
        f.setOnFavoriteListener(mFavoriteListener);
        f.execute(track);
    }

    @Override
    public QuickTrackMenu getQuickTrackMenu() {
        return mQuickTrackMenu;
    }

    public void removeFavorite(Track track) {
        FavoriteRemoveTask f = new FavoriteRemoveTask(mActivity.getApp());
        f.setOnFavoriteListener(mFavoriteListener);
        f.execute(track);
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
