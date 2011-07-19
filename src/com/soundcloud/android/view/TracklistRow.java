
package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class TracklistRow extends LazyRow {
    protected Track mTrack;

    protected ImageView mPlayIndicator;
    protected ImageView mPrivateIndicator;

    protected TextView mUser;
    protected TextView mTitle;
    protected TextView mCreatedAt;

    protected ImageButton mPlayBtn;
    protected ImageButton mFavoriteBtn;
    protected ImageButton mProfileBtn;
    protected ImageButton mCommentBtn;
    protected ImageButton mShareBtn;

    protected RelativeLayout mTrackInfoRow;

    protected ImageView mCloseIcon;

    protected Boolean _isFavorite = false;

    public TracklistRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mTitle = (TextView) findViewById(R.id.track);
        mUser = (TextView) findViewById(R.id.user);
        mCreatedAt = (TextView) findViewById(R.id.track_created_at);
        mCloseIcon = (ImageView) findViewById(R.id.close_icon);
        mPlayIndicator = (ImageView) findViewById(R.id.play_indicator);
        mPrivateIndicator = (ImageView) findViewById(R.id.private_indicator);
        mTrackInfoRow = (RelativeLayout) findViewById(R.id.track_info_row);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.track_list_row;
    }

    @Override
    protected Drawable getIconBgResourceId() {
        return getResources().getDrawable(R.drawable.artwork_badge);
    }

    @Override
    protected void initSubmenu() {
        mProfileBtn = (ImageButton) findViewById(R.id.btn_profile);
        mProfileBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, UserBrowser.class);
                intent.putExtra("userId", mTrack.user.id);
                mActivity.startActivity(intent);
            }
        });

        mPlayBtn = (ImageButton) findViewById(R.id.btn_play);
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mTrack.id == ((TracklistAdapter) mAdapter).playingId && ((TracklistAdapter) mAdapter).isPlaying) {
                    mActivity.pause(false);
                } else {
                    mActivity.playTrack(mTrack.id, (ArrayList<Parcelable>) mAdapter.getData(),mCurrentPosition, false);
                }
            }
        });


        mFavoriteBtn = (ImageButton) findViewById(R.id.btn_favorite);
        mFavoriteBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFavorite();
            }
        });

        mCommentBtn = (ImageButton) findViewById(R.id.btn_comment);
        mCommentBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mActivity.addNewComment(CloudUtils.buildComment(
                        mActivity,
                        mActivity.getCurrentUserId(),
                        mTrack.id, -1, "",
                        0), null);
            }
        });

        mShareBtn = (ImageButton) findViewById(R.id.btn_share);
        if (mTrack.sharing.contentEquals("public")) {
            mShareBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mTrack.title + " by " + mTrack.user.username + " on #SoundCloud");
                    shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mTrack.permalink_url);
                    mActivity.startActivity(Intent.createChooser(shareIntent, "Share: " + mTrack.title));
                }
            });
            mShareBtn.setVisibility(View.VISIBLE);
        } else {
            mShareBtn.setVisibility(View.GONE);
        }

        mTrackInfoRow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mAdapter.submenuIndex = -1;
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onSubmenu() {
        if (mTrack.id == ((TracklistAdapter) mAdapter).playingId && ((TracklistAdapter) mAdapter).isPlaying) {
            mPlayBtn.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.ic_submenu_pause_states));
        } else {
            mPlayBtn.setImageDrawable(mActivity.getResources().getDrawable(R.drawable.ic_submenu_play_states));
        }

        setFavoriteStatus();
        mFavoriteBtn.setEnabled(true);
        mTrackInfoRow.setFocusable(true);
        mTrackInfoRow.setClickable(true);
        mTrackInfoRow.setBackgroundResource(R.drawable.list_item_submenu_top);

    }


    @Override
    protected void onNoSubmenu() {
        mTrackInfoRow.setFocusable(false);
        mTrackInfoRow.setClickable(false);
        mTrackInfoRow.setBackgroundColor(Color.TRANSPARENT);
    }

    private void setFavoriteStatus() {

        if (mTrack == null) {
            return;
        }

        if (mTrack.user_favorite) {
            mFavoriteBtn.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_favorited_states));
        } else {
            mFavoriteBtn.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_favorite_states));
        }
    }

    private void toggleFavorite() {

        if (mTrack == null)
            return;

        mFavoriteBtn.setEnabled(false);

        if (mTrack.user_favorite) {
            mTrack.user_favorite = false;
            ((TracklistAdapter) mAdapter).removeFavorite(mTrack);
        } else {
            mTrack.user_favorite = true;
            ((TracklistAdapter) mAdapter).addFavorite(mTrack);
        }
        setFavoriteStatus();
    }


    protected long getTrackTime(Parcelable p) {
        return getTrackFromParcelable(p).created_at.getTime();
    }

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        final Parcelable p = (Parcelable) mAdapter.getItem(position);

        mTrack = getTrackFromParcelable(p);

        super.display(position);

        if (mTrack == null)
            return;

        mTitle.setText(mTrack.title);
        mUser.setText(mTrack.user.username);

        mCreatedAt.setText(CloudUtils.getTimeElapsed(mActivity.getResources(), getTrackTime(p)));

        if (!mTrack.streamable) {
            mTitle.setTextAppearance(mActivity, R.style.txt_list_main_inactive);
        } else {
            mTitle.setTextAppearance(mActivity, R.style.txt_list_main);
        }

        if (mTrack.sharing.contentEquals("public")) {
            mPrivateIndicator.setVisibility(View.GONE);
        } else {
            mPrivateIndicator.setVisibility(View.VISIBLE);
        }

        _isFavorite = mTrack.user_favorite;
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
        if (mTrack == null || mTrack.artwork_url == null)
            return "";

        if (CloudUtils.isScreenXL(mActivity)) {
            return CloudUtils.formatGraphicsUrl(mTrack.artwork_url, Consts.GraphicsSizes.LARGE);
        } else {
            if (getContext().getResources().getDisplayMetrics().density > 1) {
                return CloudUtils.formatGraphicsUrl(mTrack.artwork_url, Consts.GraphicsSizes.LARGE);
            } else {
                return CloudUtils.formatGraphicsUrl(mTrack.artwork_url, Consts.GraphicsSizes.BADGE);
            }
        }

    }


}
