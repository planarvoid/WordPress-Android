
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.task.FavoriteAddTask;
import com.soundcloud.android.task.FavoriteRemoveTask;
import com.soundcloud.android.task.FavoriteTask;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TracklistRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class TracklistAdapter extends LazyBaseAdapter {
    public static final String TAG = "TracklistAdapter";

    public long playingId = -1;
    public boolean isPlaying = false;

    public TracklistAdapter(ScActivity activity, ArrayList<Parcelable> data, Class<?> model) {
        super(activity, data, model);
    }

    @Override
    protected LazyRow createRow() {
        return new TracklistRow(mActivity, this);
    }

    public Track getTrackAt(int index) {
        return (Track) mData.get(index);
    }

    public void setPlayingId(long currentTrackId, boolean isPlaying) {
        this.playingId = currentTrackId;
        this.isPlaying = isPlaying;

        for (int i = 0; i < mData.size(); i++) {
            if (getTrackAt(i).id == currentTrackId) {
                getTrackAt(i).user_played = true;
            }
        }
    }

    public void addFavorite(Track t) {
        FavoriteAddTask f = new FavoriteAddTask(mActivity.getApp());
        f.setOnFavoriteListener(mFavoriteListener);
        f.execute(t);
    }

    public void removeFavorite(Track t) {
        FavoriteRemoveTask f = new FavoriteRemoveTask(mActivity.getApp());
        f.setOnFavoriteListener(mFavoriteListener);
        f.execute(t);
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
