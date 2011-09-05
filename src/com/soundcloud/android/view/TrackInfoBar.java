package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TrackInfoBar extends RelativeLayout {
    private Track mTrack;

    private TextView mUser;
    private TextView mTitle;
    private TextView mCreatedAt;
    private TextView mPrivateIndicator;

    private TextView mFavoriteCount;
    private TextView mPlayCount;
    private TextView mCommentCount;

    private View mPlayCountSeparator;
    private View mCommentCountSeparator;

    private Drawable mFavoritesDrawable;
    private Drawable mFavoritedDrawable;
    private Drawable mPrivateBgDrawable;
    private Drawable mVeryPrivateBgDrawable;
    private Drawable mPlayingDrawable;

    private boolean mInactive;

    private ImageView mIcon;

    public TrackInfoBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.track_info_bar, this);

        mIcon = (ImageView) findViewById(R.id.icon);
        mTitle = (TextView) findViewById(R.id.track);
        mUser = (TextView) findViewById(R.id.user);
        mCreatedAt = (TextView) findViewById(R.id.track_created_at);

        mPrivateIndicator = (TextView) findViewById(R.id.private_indicator);
        mFavoriteCount = (TextView) findViewById(R.id.favorite_count);
        mPlayCount = (TextView) findViewById(R.id.play_count);
        mCommentCount = (TextView) findViewById(R.id.comment_count);

        mPlayCountSeparator = findViewById(R.id.vr_play_count);
        mCommentCountSeparator = findViewById(R.id.vr_comment_count);
    }

    private Drawable getPlayingDrawable() {
        if (mPlayingDrawable == null) {
            mPlayingDrawable = getResources().getDrawable(R.drawable.list_playing);
            mPlayingDrawable.setBounds(0, 0, mPlayingDrawable.getIntrinsicWidth(), mPlayingDrawable.getIntrinsicHeight());
        }
        return mPlayingDrawable;
    }

    private Drawable getFavoritedDrawable(){
          if (mFavoritedDrawable == null) {
              mFavoritedDrawable = getResources().getDrawable(R.drawable.ic_stats_favorited_states);
              mFavoritedDrawable.setBounds(0, 0, mFavoritedDrawable.getIntrinsicWidth(), mFavoritedDrawable.getIntrinsicHeight());
          }
        return mFavoritedDrawable;
    }

    private Drawable getFavoritesDrawable(){
          if (mFavoritesDrawable == null) {
              mFavoritesDrawable = getResources().getDrawable(R.drawable.ic_stats_favorites_states);
              mFavoritesDrawable.setBounds(0, 0, mFavoritesDrawable.getIntrinsicWidth(), mFavoritesDrawable.getIntrinsicHeight());
          }
        return mFavoritesDrawable;
    }

    private Drawable getPrivateBgDrawable(){
          if (mPrivateBgDrawable == null) {
              mPrivateBgDrawable = getResources().getDrawable(R.drawable.round_rect_gray);
              mPrivateBgDrawable.setBounds(0, 0, mPrivateBgDrawable.getIntrinsicWidth(), mPrivateBgDrawable.getIntrinsicHeight());
          }
        return mPrivateBgDrawable;
    }

    private Drawable getVeryPrivateBgDrawable(){
          if (mVeryPrivateBgDrawable == null) {
              mVeryPrivateBgDrawable = getResources().getDrawable(R.drawable.round_rect_orange);
              mVeryPrivateBgDrawable.setBounds(0, 0, mVeryPrivateBgDrawable.getIntrinsicWidth(), mVeryPrivateBgDrawable.getIntrinsicHeight());
          }
        return mVeryPrivateBgDrawable;
    }

    protected void setTrackTime(Parcelable p) {
        if (p instanceof Event) {
            if (((Event) p).created_at != null){
                mCreatedAt.setText(((Event) p).getElapsedTime(getContext()));
            }
        } else {
            if (mTrack.created_at != null){
                mCreatedAt.setText(mTrack.getElapsedTime(getContext()));
            }
        }
    }

    /** update the views with the data corresponding to selection index */
    public void display(Parcelable p, boolean shouldLoadIcon, long playingId, boolean keepHeight) {
        mTrack = p instanceof Event ? ((Event) p).getTrack() :
                 p instanceof Track ? (Track) p : null;
        if (mTrack == null) return;

        setTrackTime(p);
        if (mTrack.user != null) mUser.setText(mTrack.user.username);

        if (!mTrack.isStreamable() && !mInactive) {
            mTitle.setTextAppearance(getContext(), R.style.txt_list_main_inactive);
            mInactive = true;
        } else if (mInactive){
            mTitle.setTextAppearance(getContext(), R.style.txt_list_main);
            mInactive = false;
        }

        if (mTrack.sharing == null || mTrack.sharing.contentEquals("public")) {
            mPrivateIndicator.setVisibility(View.GONE);
        } else {
            if (mTrack.shared_to_count == 1){
                mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                mPrivateIndicator.setText(getContext().getString(R.string.tracklist_item_shared_with_you));
            } else {
                if (mTrack.shared_to_count < 8){
                    mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                } else {
                    mPrivateIndicator.setBackgroundDrawable(getPrivateBgDrawable());
                }
                mPrivateIndicator.setText(getContext().getString(R.string.tracklist_item_shared_with_x_people, mTrack.shared_to_count));
            }
            mPrivateIndicator.setVisibility(View.VISIBLE);
        }

        CloudUtils.setStats(mTrack.playback_count, mPlayCount, mPlayCountSeparator, mTrack.comment_count,
                mCommentCount, mCommentCountSeparator, mTrack.favoritings_count, mFavoriteCount, keepHeight);

        if (mTrack.user_favorite) {
            mFavoriteCount.setCompoundDrawablesWithIntrinsicBounds(getFavoritedDrawable(),null, null, null);
        } else {
            mFavoriteCount.setCompoundDrawables(getFavoritesDrawable(),null, null, null);
        }

        if (mTrack.id == playingId) {
            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append("  ");
            sb.setSpan(new ImageSpan(getPlayingDrawable(), ImageSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(mTrack.title);
            mTitle.setText(sb);
        } else {
            mTitle.setText(mTrack.title);
        }
        if (shouldLoadIcon) loadIcon();
    }

    private void loadIcon() {
        final String iconUrl = getTrackIcon();
        if (TextUtils.isEmpty(iconUrl)) {
            ImageLoader.get(getContext()).unbind(mIcon); // no artwork
        }
    }

    public String getTrackIcon() {
        if (mTrack == null || (mTrack.artwork_url == null && (mTrack.user == null || mTrack.user.avatar_url == null))){
           return "";
        }
        return ImageUtils.formatGraphicsUriForList(getContext(),
                mTrack.artwork_url == null ? mTrack.user.avatar_url : mTrack.artwork_url);
    }
}
