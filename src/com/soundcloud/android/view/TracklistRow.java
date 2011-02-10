
package com.soundcloud.android.view;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.objects.Track;

public class TracklistRow extends LazyRow {

    private static final String TAG = "TracklistRow";

    protected Track mTrack;

    protected ImageView mPlayIndicator;

    protected ImageView mPrivateIndicator;

    protected TextView mUser;

    protected TextView mTitle;

    protected TextView mDuration;

    protected ImageButton mPlayBtn;

    protected ImageButton mPlaylistBtn;

    protected ImageButton mFavoriteBtn;

    protected ImageButton mDownloadBtn;

    protected ImageButton mDetailsBtn;

    protected Boolean _isPlaying = false;

    protected Boolean _isFavorite = false;

    private String _iconURL;

    private String _playURL;

    private int mCurrentIndex = -1;

    public TracklistRow(Context _context) {
        super(_context);

        mTitle = (TextView) findViewById(R.id.track);
        mUser = (TextView) findViewById(R.id.user);
        mDuration = (TextView) findViewById(R.id.duration);
        mIcon = (ImageView) findViewById(R.id.icon);
        mPlayIndicator = (ImageView) findViewById(R.id.play_indicator);
        mPrivateIndicator = (ImageView) findViewById(R.id.private_indicator);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.track_list_item;
    }

    public void display(Parcelable p, boolean selected, boolean isPlaying) {

        _isPlaying = isPlaying;
        display(p, selected);
    }

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(Parcelable p, boolean selected) {
        super.display(p, selected);

        mTrack = getTrackFromParcelable(p);
        if (mTrack == null)
            return;

        mTitle.setText(mTrack.title);
        mUser.setText(mTrack.user.username);

        if (!CloudUtils.isTrackPlayable(mTrack)) {
            mTitle.setTextAppearance(mContext, R.style.txt_list_main_inactive);
        } else {
            mTitle.setTextAppearance(mContext, R.style.txt_list_main);
        }

        if (mTrack.sharing.contentEquals("public")) {
            mPrivateIndicator.setVisibility(View.GONE);
        } else {
            mPrivateIndicator.setVisibility(View.VISIBLE);
        }
        if (mTrack.user_favorite) {
            _isFavorite = true;
        } else {
            _isFavorite = false;
        }
        
        if (_isPlaying) {
            mPlayIndicator.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.list_playing));
            mPlayIndicator.setVisibility(View.VISIBLE);
        } else if (_isFavorite) {
            mPlayIndicator.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.list_favorite));
            mPlayIndicator.setVisibility(View.VISIBLE);
        } else if ((mTrack.user_played == null ? false : mTrack.user_played) == false) {
            mPlayIndicator.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.list_unlistened));
            mPlayIndicator.setVisibility(View.VISIBLE);
        } else {
            mPlayIndicator.setVisibility(View.GONE);
        }

    }

    protected Track getTrackFromParcelable(Parcelable p) {
        return (Track) p;
    }

    @Override
    public ImageView getRowIcon() {
        if (getContext().getResources().getDisplayMetrics().density > 1) {
            mIcon.getLayoutParams().width = 67;
            mIcon.getLayoutParams().height = 67;
        }
        return mIcon;
    }

    @Override
    public String getIconRemoteUri() {
        if (getContext().getResources().getDisplayMetrics().density > 1) {
            return CloudUtils.formatGraphicsUrl(mTrack.artwork_url, GraphicsSizes.large);
        } else
            return CloudUtils.formatGraphicsUrl(mTrack.artwork_url, GraphicsSizes.badge);

    }


}
