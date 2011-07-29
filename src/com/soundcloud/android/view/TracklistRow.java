
package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.TracklistAdapter;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class TracklistRow extends LazyRow {
    protected Track mTrack;

    protected ImageButton mPlayBtn;
    protected ImageButton mFavoriteBtn;
    protected ImageButton mProfileBtn;
    protected ImageButton mCommentBtn;
    protected ImageButton mShareBtn;

    private TrackInfoBar mTrackInfoBar;

    protected ImageView mCloseIcon;

    protected Boolean _isFavorite = false;


    public TracklistRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);

        mCloseIcon = (ImageView) findViewById(R.id.close_icon);
        mTrackInfoBar = (TrackInfoBar) findViewById(R.id.track_info_bar);
    }

    @Override
    protected int getRowResourceId() {
        return R.layout.track_list_row;
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

        mTrackInfoBar.setOnClickListener(new View.OnClickListener() {
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
        mTrackInfoBar.setFocusable(true);
        mTrackInfoBar.setClickable(true);
        mTrackInfoBar.setBackgroundResource(R.drawable.list_item_submenu_top);

    }


    @Override
    protected void onNoSubmenu() {
        mTrackInfoBar.setFocusable(false);
        mTrackInfoBar.setClickable(false);
        mTrackInfoBar.setBackgroundColor(Color.TRANSPARENT);
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
        final Parcelable p = (Parcelable) mAdapter.getItem(position);
        mTrack = getTrackFromParcelable(p);
        super.display(position);
        mTrackInfoBar.display(mTrack, false, ((TracklistAdapter) mAdapter).playingId);

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
        return ImageUtils.formatGraphicsUrlForList(mActivity,
                mTrack.artwork_url == null ? mTrack.user.avatar_url : mTrack.artwork_url);
    }




}
