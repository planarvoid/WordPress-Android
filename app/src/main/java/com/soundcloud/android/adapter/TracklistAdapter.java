package com.soundcloud.android.adapter;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScListActivity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TrackInfoBar;

import android.content.Context;
import android.os.Parcelable;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;

import java.util.ArrayList;

public class TracklistAdapter extends LazyBaseAdapter implements ITracklistAdapter {
    public static final String TAG = "TracklistAdapter";

    private long mPlayingId = -1;
    private boolean mIsPlaying = false;
    private QuickTrackMenu mQuickTrackMenu;

    public TracklistAdapter(Context c, ArrayList<Parcelable> data, Class<?> model) {
        super(c, data, model);
        if (c instanceof ScListActivity){
            mQuickTrackMenu = new QuickTrackMenu((ScListActivity) c, this);
        }
    }

   @Override
    protected LazyRow createRow(int position) {
        return new TrackInfoBar(mContext, this);
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
        if (mPlayingId != currentTrackId || mIsPlaying != isPlaying) {
            mPlayingId = currentTrackId;
            mIsPlaying = isPlaying;
            notifyDataSetChanged();
        }
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
