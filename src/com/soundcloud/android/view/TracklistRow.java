
package com.soundcloud.android.view;

import android.view.animation.Transformation;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.ITracklistAdapter;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.MyTracksAdapter;
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

     @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
         super.getChildStaticTransformation(child, t);
         t.setAlpha((float) 0.4);
         return true;

     }

    private void configureQuickActionMenu(){

    }

    protected int getRowResourceId() {
        return R.layout.track_list_row;
    }

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        final Parcelable p = (Parcelable) mAdapter.getItem(position);
        mTrack = getTrackFromParcelable(p);
        super.display(position);
        if (mTrack.isStreamable()) {
            setStaticTransformationsEnabled(false);
        } else {
            setStaticTransformationsEnabled(true);
        }
        mTrackInfoBar.display(p, false, ((ITracklistAdapter) mAdapter).getPlayingId(), false, mActivity.getCurrentUserId());
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
