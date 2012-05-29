package com.soundcloud.android.view;

import android.database.Cursor;
import android.os.Parcelable;
import android.util.Log;
import android.view.animation.Transformation;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.adapter.ITracklistAdapter;
import com.soundcloud.android.model.*;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
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

public class TrackInfoBar extends LazyRow {
    public static final ImageLoader.Options ICON_OPTIONS = new ImageLoader.Options(true, true);

    private Playable mPlayable;

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

    private boolean mShouldLoadIcon;

    protected ImageView mIcon;
    private ImageLoader.BindResult mCurrentIconBindResult;

    public TrackInfoBar(Context context, IScAdapter adapter) {
        super(context,adapter);
        init();
    }

    public TrackInfoBar(Context context, AttributeSet attrs) {
        super(context, null);
        init();
    }

    private void init(){
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

        if (mAdapter == null) {
            // player view, these need to be set
            setId(R.id.track_info_bar);
            setBackgroundResource(R.color.playerControlBackground);
        } else {
            if (mIcon != null && ((ITracklistAdapter) mAdapter).getQuickTrackMenu() != null) {
                mIcon.setFocusable(false);
                mIcon.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mAdapter != null && mIcon != null && mPlayable != null && mPlayable.getTrack() != null){
                            ((ITracklistAdapter) mAdapter).getQuickTrackMenu().show(mIcon, mPlayable.getTrack(), mCurrentPosition);
                        }
                    }
                });
            }
        }
    }

    public void addTextShadows(){
        CloudUtils.setTextShadowForGrayBg(mTitle);
        CloudUtils.setTextShadowForGrayBg(mUser);
        CloudUtils.setTextShadowForGrayBg(mCreatedAt);
        CloudUtils.setTextShadowForGrayBg(mFavoriteCount);
        CloudUtils.setTextShadowForGrayBg(mPlayCount);
        CloudUtils.setTextShadowForGrayBg(mCommentCount);
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

    /** update the views with the data corresponding to selection index */
    public void display(Playable p, boolean shouldLoadIcon, long playingId, boolean keepHeight, long currentUserId) {
        mPlayable = p;
        mShouldLoadIcon = shouldLoadIcon;

        final Track track = mPlayable.getTrack();
        if (track == null) return;

        final Context context = getContext();

        mUser.setText(track.user != null ? track.user.username : "");
        mCreatedAt.setText(p.getTimeSinceCreated(context));

        if (track.isPublic()) {
            mPrivateIndicator.setVisibility(View.GONE);
        } else {
            if (track.shared_to_count == 0){
                mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                mPrivateIndicator.setText(R.string.tracklist_item_shared_count_unavailable);
            } else if (track.shared_to_count == 1){
                mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                mPrivateIndicator.setText(track.user_id == currentUserId ? R.string.tracklist_item_shared_with_1_person : R.string.tracklist_item_shared_with_you);
            } else {
                if (track.shared_to_count < 8){
                    mPrivateIndicator.setBackgroundDrawable(getVeryPrivateBgDrawable());
                } else {
                    mPrivateIndicator.setBackgroundDrawable(getPrivateBgDrawable());
                }
                mPrivateIndicator.setText(context.getString(R.string.tracklist_item_shared_with_x_people, track.shared_to_count));
            }
            mPrivateIndicator.setVisibility(View.VISIBLE);
        }


        setStats(track.playback_count, mPlayCount,
                mPlayCountSeparator,
                track.comment_count, mCommentCount,
                mCommentCountSeparator,
                track.favoritings_count, mFavoriteCount,
                keepHeight);

        if (track.user_favorite) {
            mFavoriteCount.setCompoundDrawablesWithIntrinsicBounds(getFavoritedDrawable(),null, null, null);
        } else {
            mFavoriteCount.setCompoundDrawables(getFavoritesDrawable(),null, null, null);
        }


        if (track.id == playingId) {
            SpannableStringBuilder sb = new SpannableStringBuilder();
            sb.append("  ");
            sb.setSpan(new ImageSpan(getPlayingDrawable(), ImageSpan.ALIGN_BASELINE), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(track.title);
            mTitle.setText(sb);
        } else {
            mTitle.setText(track.title);
        }

        if (shouldLoadIcon) loadIcon();
    }

    /** List specific functions **/

    @Override
    protected int getRowResourceId() {
        return R.layout.track_info_bar;
    }

    @Override
    public String getIconRemoteUri() {
        return mPlayable.getTrack() == null ? null : mPlayable.getTrack().getListArtworkUrl(getContext());
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), new Track(cursor));
    }

    @Override
    public void display(int position, Parcelable p) {
        if (!(p instanceof Playable)) throw new IllegalArgumentException("Not a valid track");

        // have to set the playable here for list icon loading purposes, it gets set again above for non-lists
        mPlayable = (Playable) p;

        super.display(position);

        if (mPlayable.getTrack().isStreamable()) {
            setStaticTransformationsEnabled(false);
        } else {
            setStaticTransformationsEnabled(true);
        }

        display(mPlayable, false, ((ITracklistAdapter) mAdapter).getPlayingId(), false, mCurrentUserId);
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
         super.getChildStaticTransformation(child, t);
         t.setAlpha((float) 0.4);
         return true;

     }


    /** Non-list, Icon functions **/

    public void onConnected(){
        if (mCurrentIconBindResult == ImageLoader.BindResult.ERROR && mShouldLoadIcon){
            loadIcon();
        }
    }

    private void loadIcon() {
        final String iconUrl = mPlayable.getTrack() == null ? null : Consts.GraphicSize.formatUriForList(getContext(), mPlayable.getTrack().getArtwork());
        if (TextUtils.isEmpty(iconUrl)) {
            mCurrentIconBindResult = ImageLoader.BindResult.OK;
            ImageLoader.get(getContext()).unbind(mIcon); // no artwork
        } else {
            mCurrentIconBindResult = ImageLoader.get(getContext()).bind(mIcon,
                    iconUrl, mImageLoaderCallback, ICON_OPTIONS);
        }
    }

    private ImageLoader.Callback mImageLoaderCallback = new ImageLoader.Callback() {
        @Override
        public void onImageError(ImageView view, String url, Throwable error) {
            mCurrentIconBindResult = ImageLoader.BindResult.ERROR;
        }

        @Override
        public void onImageLoaded(ImageView view, String url) {
            mCurrentIconBindResult = ImageLoader.BindResult.OK;
        }

    };

    public static void setStats(int stat1, TextView statTextView1,
                                View separator1,
                                int stat2, TextView statTextView2,
                                View separator2,
                                int stat3, TextView statTextView3,
                                boolean maintainSize) {

        statTextView1.setText(String.valueOf(stat1));
        statTextView2.setText(String.valueOf(stat2));
        statTextView3.setText(String.valueOf(stat3));

        statTextView1.setVisibility(stat1 == 0 ? View.GONE : View.VISIBLE);
        separator1.setVisibility(stat1 == 0 || (stat2 == 0 && stat3 == 0) ? View.GONE : View.VISIBLE);

        statTextView2.setVisibility(stat2 == 0 ? View.GONE : View.VISIBLE);
        separator2.setVisibility(stat2 == 0 || stat3 == 0 ? View.GONE : View.VISIBLE);
        statTextView3.setVisibility(stat3 == 0 ? maintainSize ? View.INVISIBLE : View.GONE : View.VISIBLE);
    }

}
