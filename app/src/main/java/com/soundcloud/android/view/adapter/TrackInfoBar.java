package com.soundcloud.android.view.adapter;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.IScAdapter;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.act.TrackRepostActivity;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.view.quickaction.QuickTrackMenu;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;

public class TrackInfoBar extends LazyRow {
    public static final ImageLoader.Options ICON_OPTIONS = new ImageLoader.Options(true, true);

    private Playable mPlayable;

    private TextView mUser;
    private TextView mReposter;
    private TextView mTitle;
    private TextView mCreatedAt;
    private TextView mPrivateIndicator;

    private TextView mLikeCount;
    private TextView mPlayCount;
    private TextView mCommentCount;
    private TextView mRepostCount;

    private View mPlayCountSeparator, mCommentCountSeparator, mLikeCountSeparator;

    private Drawable mLikesDrawable;
    private Drawable mLikedDrawable;
    private Drawable mRepostsDrawable;
    private Drawable mRepostedDrawable;
    private Drawable mPrivateBgDrawable;
    private Drawable mVeryPrivateBgDrawable;
    private Drawable mPlayingDrawable;

    private boolean mShouldLoadIcon;

    protected ImageView mIcon;
    private ImageLoader.BindResult mCurrentIconBindResult;

    public TrackInfoBar(Context context, @Nullable IScAdapter adapter) {
        super(context,adapter);
        init();
    }

    public TrackInfoBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet, null);
        init();
    }

    public TrackInfoBar(Context context) {
        super(context, null);
        init();
    }

    @Override
    protected View addContent() {
        return View.inflate(getContext(), R.layout.track_info_bar, this);
    }

    private void init(){
        mIcon = (ImageView) findViewById(R.id.icon);
        mTitle = (TextView) findViewById(R.id.track);
        mUser = (TextView) findViewById(R.id.user);
        mReposter = (TextView) findViewById(R.id.reposter);
        mCreatedAt = (TextView) findViewById(R.id.track_created_at);

        mPrivateIndicator = (TextView) findViewById(R.id.private_indicator);
        mLikeCount = (TextView) findViewById(R.id.like_count);
        mPlayCount = (TextView) findViewById(R.id.play_count);
        mCommentCount = (TextView) findViewById(R.id.comment_count);
        mRepostCount = (TextView) findViewById(R.id.repost_count);

        mPlayCountSeparator = findViewById(R.id.vr_play_count);
        mCommentCountSeparator = findViewById(R.id.vr_comment_count);
        mLikeCountSeparator = findViewById(R.id.vr_like_count);

        if (mAdapter == null) {
            // player view, these need to be set
            setId(R.id.track_info_bar);
            setBackgroundResource(R.color.playerControlBackground);
        } else {
            if (mIcon != null && mAdapter.getQuickActionMenu() != null) {
                final QuickTrackMenu quickTrackMenu = (QuickTrackMenu) mAdapter.getQuickActionMenu();
                mIcon.setFocusable(false);
                mIcon.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mAdapter != null && mIcon != null && mPlayable != null && mPlayable.getTrack() != null){
                            quickTrackMenu.show(mIcon, mPlayable.getTrack());
                        }
                    }
                });
            }
        }
    }

    public void addTextShadows(){
        AndroidUtils.setTextShadowForGrayBg(mTitle);
        AndroidUtils.setTextShadowForGrayBg(mUser);
        AndroidUtils.setTextShadowForGrayBg(mCreatedAt);
        AndroidUtils.setTextShadowForGrayBg(mLikeCount);
        AndroidUtils.setTextShadowForGrayBg(mPlayCount);
        AndroidUtils.setTextShadowForGrayBg(mCommentCount);
    }

    private Drawable getPlayingDrawable() {
        if (mPlayingDrawable == null) {
            mPlayingDrawable = getResources().getDrawable(R.drawable.list_playing);
            mPlayingDrawable.setBounds(0, 0, mPlayingDrawable.getIntrinsicWidth(), mPlayingDrawable.getIntrinsicHeight());
        }
        return mPlayingDrawable;
    }

    private Drawable getLikedDrawable(){
          if (mLikedDrawable == null) {
              mLikedDrawable = getResources().getDrawable(R.drawable.ic_stats_liked_states);
              mLikedDrawable.setBounds(0, 0, mLikedDrawable.getIntrinsicWidth(), mLikedDrawable.getIntrinsicHeight());
          }
        return mLikedDrawable;
    }

    private Drawable getLikesDrawable(){
          if (mLikesDrawable == null) {
              mLikesDrawable = getResources().getDrawable(R.drawable.ic_stats_likes_states);
              mLikesDrawable.setBounds(0, 0, mLikesDrawable.getIntrinsicWidth(), mLikesDrawable.getIntrinsicHeight());
          }
        return mLikesDrawable;
    }

    private Drawable getRepostsDrawable() {
        if (mRepostsDrawable == null) {
            mRepostsDrawable = getResources().getDrawable(R.drawable.ic_stats_reposts_states);
            mRepostsDrawable.setBounds(0, 0, mRepostsDrawable.getIntrinsicWidth(), mRepostsDrawable.getIntrinsicHeight());
        }
        return mRepostsDrawable;
    }

    private Drawable getRepostedDrawable() {
        if (mRepostedDrawable == null) {
            mRepostedDrawable = getResources().getDrawable(R.drawable.ic_stats_reposted_states);
            mRepostedDrawable.setBounds(0, 0, mRepostedDrawable.getIntrinsicWidth(), mRepostedDrawable.getIntrinsicHeight());
        }
        return mRepostedDrawable;
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

        if (mPlayable instanceof TrackRepostActivity) {
            mReposter.setText(((TrackRepostActivity) mPlayable).user.username);
            mReposter.setVisibility(View.VISIBLE);
        } else {
            mReposter.setVisibility(View.GONE);
        }

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
                track.likes_count, mLikeCount,
                mLikeCountSeparator,
                track.reposts_count, mRepostCount,
                keepHeight);

        if (track.user_like) {
            mLikeCount.setCompoundDrawablesWithIntrinsicBounds(getLikedDrawable(), null, null, null);
        } else {
            mLikeCount.setCompoundDrawables(getLikesDrawable(), null, null, null);
        }

        if (track.user_repost) {
            mRepostCount.setCompoundDrawablesWithIntrinsicBounds(getRepostedDrawable(), null, null, null);
        } else {
            mRepostCount.setCompoundDrawables(getRepostsDrawable(), null, null, null);
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

        if (track.isProcessing()){
            if (findViewById(R.id.processing_progress) != null){
                findViewById(R.id.processing_progress).setVisibility(View.VISIBLE);
            } else {
                ((ViewStub) findViewById(R.id.processing_progress_stub)).inflate();
            }
        } else if (findViewById(R.id.processing_progress) != null){
            findViewById(R.id.processing_progress).setVisibility(View.GONE);
        }

        if (shouldLoadIcon) loadIcon();
    }

    /** List specific functions **/
    @Override
    public String getIconRemoteUri() {
        return mPlayable.getTrack() == null ? null : mPlayable.getTrack().getListArtworkUrl(getContext());
    }

    @Override
    public void display(Cursor cursor) {
        display(cursor.getPosition(), SoundCloudApplication.MODEL_MANAGER.getTrackFromCursor(cursor));
    }

    @Override
    public void display(int position, Parcelable p) {
        if (!(p instanceof Playable)) throw new IllegalArgumentException("Not a valid track " + p);

        // have to set the playable here for list icon loading purposes, it gets set again above for non-lists
        mPlayable = (Playable) p;

        super.display(position);

        if (mPlayable.getTrack().isStreamable()) {
            setStaticTransformationsEnabled(false);
        } else {
            setStaticTransformationsEnabled(true);
        }

        display(mPlayable, false, CloudPlaybackService.getCurrentTrackId(), false, getCurrentUserId());
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
                                View separator3,
                                int stat4, TextView statTextView4,
                                boolean maintainSize) {

        statTextView1.setText(String.valueOf(stat1));
        statTextView2.setText(String.valueOf(stat2));
        statTextView3.setText(String.valueOf(stat3));
        statTextView4.setText(String.valueOf(stat4));

        statTextView1.setVisibility(stat1 <= 0 ? View.GONE : View.VISIBLE);
        separator1.setVisibility(stat1 <= 0 || (stat2 <= 0 && stat3 <= 0 && stat4 <= 0) ? View.GONE : View.VISIBLE);

        statTextView2.setVisibility(stat2 == 0 ? View.GONE : View.VISIBLE);
        separator2.setVisibility(stat2 <= 0 || (stat3 <= 0 && stat4 <= 0) ? View.GONE : View.VISIBLE);

        statTextView3.setVisibility(stat3 <= 0 ? View.GONE : View.VISIBLE);
        separator3.setVisibility(stat3 <= 0 || stat4 <= 0 ? View.GONE : View.VISIBLE);

        statTextView4.setVisibility(stat4 <= 0 ? maintainSize ? View.INVISIBLE : View.GONE : View.VISIBLE);
    }

}
