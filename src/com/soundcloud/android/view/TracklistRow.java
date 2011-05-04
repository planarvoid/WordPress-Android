
package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.CloudUtils.GraphicsSizes;

import android.content.Intent;
import android.graphics.Color;
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

    protected TextView mDuration;

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
        mDuration = (TextView) findViewById(R.id.duration);
        mCreatedAt = (TextView) findViewById(R.id.track_created_at);
        mIcon = (ImageView) findViewById(R.id.icon);
        mCloseIcon = (ImageView) findViewById(R.id.close_icon);
        mPlayIndicator = (ImageView) findViewById(R.id.play_indicator);
        mPrivateIndicator = (ImageView) findViewById(R.id.private_indicator);
        mTrackInfoRow = (RelativeLayout) findViewById(R.id.track_info_row);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.track_list_item;
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
                        mActivity.getUserId(),
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


    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        mTrack = getTrackFromParcelable((Parcelable) mAdapter.getItem(position));

        super.display(position);

        if (mTrack == null)
            return;

        mTitle.setText(mTrack.title);
        if (mUser != null){
            // normal tracklist row
            mUser.setText(mTrack.user.username);
        } else {
            // my tracklist row
            mCloseIcon.setVisibility(View.GONE);
            mCreatedAt.setTextColor(mActivity.getResources().getColor(R.color.listTxtSecondaryDark));
        }

        mCreatedAt.setText(CloudUtils.getTimeElapsed(mActivity.getResources(), mTrack.created_at.getTime()));

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

        if (mTrack.id == ((TracklistAdapter) mAdapter).playingId) {
            mPlayIndicator.setImageDrawable(mActivity.getResources().getDrawable(
                    R.drawable.list_playing));
            mPlayIndicator.setVisibility(View.VISIBLE);
        } else if (_isFavorite) {
            mPlayIndicator.setImageDrawable(mActivity.getResources().getDrawable(
                    R.drawable.list_favorite));
            mPlayIndicator.setVisibility(View.VISIBLE);
        } else if (!mTrack.user_played) {
            mPlayIndicator.setImageDrawable(mActivity.getResources().getDrawable(
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
        if (mTrack == null || mTrack.artwork_url == null)
            return "";
        if (getContext().getResources().getDisplayMetrics().density > 1) {
            return CloudUtils.formatGraphicsUrl(mTrack.artwork_url, GraphicsSizes.LARGE);
        } else
            return CloudUtils.formatGraphicsUrl(mTrack.artwork_url, GraphicsSizes.BADGE);

    }


}
