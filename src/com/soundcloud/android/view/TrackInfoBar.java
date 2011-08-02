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
    protected Track mTrack;

    protected TextView mUser;
    protected TextView mTitle;
    protected TextView mCreatedAt;
    protected TextView mPrivateIndicator;

    protected TextView mFavoriteCount;
    protected TextView mPlayCount;
    protected TextView mCommentCount;

    protected View mPlayCountSeparator;
    protected View mCommentCountSeparator;

    private Drawable mFavoritesDrawable;
    private Drawable mFavoritedDrawable;
    private Drawable mPlayingDrawable;

    private ImageLoader.BindResult mCurrentIconBindResult;

    protected ImageView mIcon;

    protected Boolean _isFavorite = false;


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



     private Drawable getPlayingDrawable(){
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

    protected void setTrackTime(Parcelable p) {
        if (p instanceof Event) {
            if (((Event) p).created_at != null){
                mCreatedAt.setText(CloudUtils.getTimeElapsed(getContext().getResources(),
                    ((Event) p).created_at.getTime()));
            }
        } else {
            if (mTrack.created_at != null){
                mCreatedAt.setText(CloudUtils.getTimeElapsed(getContext().getResources(),
                    mTrack.created_at.getTime()));
            }
        }
    }

    /** update the views with the data corresponding to selection index */
    public void display(Parcelable p, boolean shouldLoadIcon, long playingId) {
        if (p == null)
            return;

        mTrack = p instanceof Event ? ((Event) p).getTrack() :
                 p instanceof Track ? (Track) p : null;

        if (mTrack == null) return;

        setTrackTime(p);

        mUser.setText(mTrack.user.username);


        if (!mTrack.streamable) {
            mTitle.setTextAppearance(getContext(), R.style.txt_list_main_inactive);
        } else {
            mTitle.setTextAppearance(getContext(), R.style.txt_list_main);
        }

        if (mTrack.sharing == null || mTrack.sharing.contentEquals("public")) {
            mPrivateIndicator.setVisibility(View.GONE);
        } else {
            if (mTrack.shared_to_count == 1){
                mPrivateIndicator.setText(getContext().getString(R.string.tracklist_item_shared_with_you));
            } else {
                mPrivateIndicator.setText(getContext().getString(R.string.tracklist_item_shared_with_x_people, mTrack.shared_to_count));
            }
            mPrivateIndicator.setVisibility(View.VISIBLE);
        }

        CloudUtils.setStats(mTrack.playback_count, mPlayCount, mPlayCountSeparator, mTrack.comment_count,
                mCommentCount, mCommentCountSeparator, mTrack.favoritings_count, mFavoriteCount);

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

        if (shouldLoadIcon){
             loadIcon();
        }
    }

    public void onConnected(){
        if (mCurrentIconBindResult == ImageLoader.BindResult.ERROR){
            loadIcon();
        }
    }

    private void loadIcon() {
        final String iconUrl = getTrackIcon();
        if (TextUtils.isEmpty(iconUrl)) {
            // no artwork
            ImageLoader.get(getContext()).unbind(mIcon);
        } else {
            mCurrentIconBindResult = ImageLoader.get(getContext()).bind(
                    mIcon,
                    ImageUtils.formatGraphicsUrlForList(getContext(), iconUrl),
                    new ImageLoader.ImageViewCallback() {
                        @Override
                        public void onImageError(ImageView view, String url, Throwable error) {
                            mCurrentIconBindResult = ImageLoader.BindResult.ERROR;
                        }

                        @Override
                        public void onImageLoaded(ImageView view, String url) {
                        }
                    });
        }
    }

    public String getTrackIcon() {
        if (mTrack == null || (mTrack.artwork_url == null && mTrack.user.avatar_url == null)){
           return "";
        }
        return ImageUtils.formatGraphicsUrlForList(getContext(),
                mTrack.artwork_url == null ? mTrack.user.avatar_url : mTrack.artwork_url);
    }




}
