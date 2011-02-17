
package com.soundcloud.android.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.view.LazyRow;
import com.soundcloud.android.view.TracklistRow;

public class TracklistAdapter extends LazyBaseAdapter {

    public static final String IMAGE = "TracklistAdapter_image";

    public static final String TAG = "TracklistAdapter";

    protected long _playingId = -1;

    protected int _playingPosition = -1;

    public TracklistAdapter(Context context, ArrayList<Parcelable> data) {
        super(context, data);
    }

    @Override
    public View getView(int index, View row, ViewGroup parent) {

        TracklistRow rowView = null;

        if (row == null) {
            rowView = (TracklistRow) createRow();
        } else {
            rowView = (TracklistRow) row;
        }

        rowView.display(mData.get(index), mSelectedIndex == index, (_playingId != -1 && getTrackAt(
        index).id == _playingId));

        BindResult result = BindResult.ERROR;
        try { // put the bind in a try catch to catch any loading error (or the
            // occasional bad url)
            if (CloudUtils.checkIconShouldLoad(rowView.getIconRemoteUri()))
                result = mImageLoader.bind(this, rowView.getRowIcon(), rowView.getIconRemoteUri());
            else
                mImageLoader.unbind(rowView.getRowIcon());
        } catch (Exception e) {
        }
        ;
        rowView.setTemporaryDrawable(result);
        return rowView;
    }

    @Override
    protected LazyRow createRow() {
        return new TracklistRow(mActivity);
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

    public void setPlayingPosition(int position) {
        _playingPosition = position;
    }

}
