
package com.soundcloud.android.view;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.animation.Transformation;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.adapter.ITracklistAdapter;
import com.soundcloud.android.adapter.ScCursorAdapter;
import com.soundcloud.android.adapter.UserFavoritesAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.ImageUtils;

import android.os.Parcelable;
import android.view.View;
import android.widget.ImageView;

public class TracklistRow extends LazyRow {
    private Track mTrack;

    private final TrackInfoBar mTrackInfoBar;
    protected final ImageView mCloseIcon;

    public TracklistRow(Context context, IScAdapter adapter) {
        super(context, adapter);

        mCloseIcon = (ImageView) findViewById(R.id.close_icon);
        mTrackInfoBar = (TrackInfoBar) findViewById(R.id.track_info_bar);

        if (mIcon != null && ((ITracklistAdapter) mAdapter).getQuickTrackMenu() != null) {
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
    public void display(Cursor cursor) {
        display(cursor.getPosition(), new Track(cursor));
    }
    @Override
    public void display(int position, Parcelable p) {
        mTrack = getTrackFromParcelable(p);
        super.display(position);
        if (mTrack.isStreamable()) {
            setStaticTransformationsEnabled(false);
        } else {
            setStaticTransformationsEnabled(true);
        }
        mTrackInfoBar.display(p, false, ((ITracklistAdapter) mAdapter).getPlayingId(), false, mCurrentUserId);
    }

    protected Track getTrackFromParcelable(Parcelable p) {
        return (Track) p;
    }

    @Override
    public String getIconRemoteUri() {
        if (mTrack == null){
           return "";
        } else {
            final String iconUrl = mTrack.getArtwork();
            return TextUtils.isEmpty(iconUrl) ? null : ImageUtils.formatGraphicsUriForList(getContext(),mTrack.getArtwork());
        }
    }
}
