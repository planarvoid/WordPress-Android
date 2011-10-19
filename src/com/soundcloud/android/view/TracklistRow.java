
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.ITracklistAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ImageUtils;

import android.os.Parcelable;
import android.view.View;
import android.widget.ImageView;

public class TracklistRow extends LazyRow {
    private Track mTrack;

    private final TrackInfoBar mTrackInfoBar;

    protected final ImageView mCloseIcon;

    public TracklistRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mCloseIcon = (ImageView) findViewById(R.id.close_icon);
        mTrackInfoBar = (TrackInfoBar) findViewById(R.id.track_info_bar);

        if (mIcon != null) {
            mIcon.setFocusable(false);
            mIcon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ITracklistAdapter) mAdapter).getQuickTrackMenu().show(mIcon, mTrack, mCurrentPosition);
                }
            });
        }
    }

    private void configureQuickActionMenu(){

    }

    @Override
    protected int getRowResourceId() {
        return R.layout.track_list_row;
    }



    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        final Parcelable p = (Parcelable) mAdapter.getItem(position);
        mTrack = getTrackFromParcelable(p);
        super.display(position);
        mTrackInfoBar.display(p, false, ((ITracklistAdapter) mAdapter).getPlayingId(), false);

    }

    protected Track getTrackFromParcelable(Parcelable p) {
        return (Track) p;
    }

    @Override
    public ImageView getRowIcon() {
        return mIcon;
    }

    @Override
    public String getIconRemoteUri() {
        if (mTrack == null || (mTrack.artwork_url == null && mTrack.user.avatar_url == null)){
           return "";
        }
        return ImageUtils.formatGraphicsUriForList(mActivity,
                mTrack.artwork_url == null ? mTrack.user.avatar_url : mTrack.artwork_url);
    }
}
