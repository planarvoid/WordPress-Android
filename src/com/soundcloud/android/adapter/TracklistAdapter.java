
package com.soundcloud.android.adapter;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TracklistRow;

import android.os.Parcelable;

import java.util.ArrayList;

public class TracklistAdapter extends LazyBaseAdapter {

    public static final String IMAGE = "TracklistAdapter_image";

    public static final String TAG = "TracklistAdapter";

    protected long _playingId = -1;

    protected int _playingPosition = -1;

    public TracklistAdapter(ScActivity activity, ArrayList<Parcelable> data) {
        super(activity, data);
    }

    @Override
    protected LazyRow createRow() {
        return new TracklistRow(mActivity, this);
    }

    public Track getTrackAt(int index) {
        return (Track) mData.get(index);
    }

    public void setPlayingId(long currentTrackId) {
        _playingId = currentTrackId;

        for (int i = 0; i < mData.size(); i++) {
            if (getTrackAt(i).id.compareTo(currentTrackId) == 0) {
                getTrackAt(i).user_played = true;
            }
        }

        notifyDataSetChanged();
    }
    
    public void setFavoriteStatus(long trackId, boolean isFavorite) {
        for (int i = 0; i < mData.size(); i++) {
            if (getTrackAt(i).id.compareTo(trackId) == 0) {
                getTrackAt(i).user_favorite = isFavorite;
                break;
            }
        }
        notifyDataSetChanged();
    }
    
    public long getPlayingId(){
        return _playingId;
    }

    public void setPlayingPosition(int position) {
        _playingPosition = position;
    }

}
