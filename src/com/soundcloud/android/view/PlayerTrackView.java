package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.task.fetch.FetchModelTask;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.task.fetch.FetchTrackTask;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewStub;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.List;

public class PlayerTrackView extends LinearLayout implements
        View.OnTouchListener,
        FetchModelTask.FetchModelListener<Track>,
        LoadCommentsTask.LoadCommentsListener {

    private ScPlayer mPlayer;

    private ImageView mArtwork, mAvatar;
    private ImageButton mFavoriteButton, mCommentButton;
    private ImageLoader.BindResult mCurrentArtBindResult;

    private WaveformController mWaveformController;
    private FrameLayout mUnplayableLayout;

    private TrackInfoBar mTrackInfoBar;
    private ViewFlipper mTrackFlipper;
    private PlayerTrackInfo mTrackInfo;

    private boolean mDraggingLabel = false;
    private int mInitialX = -1;
    private int mLastX = -1;
    private int mTextWidth = 0;
    private int mViewWidth = 0;
    private int mTouchSlop;

    private ImageLoader.BindResult mCurrentAvatarBindResult;
    private Drawable mFavoriteDrawable, mFavoritedDrawable;

    public Track mTrack;
    private int mPlayPos;
    private long mDuration;
    private boolean mLandscape, mOnScreen;

    public PlayerTrackView(ScPlayer player) {
        super(player);

        LayoutInflater inflater = (LayoutInflater) player.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_track, this);

        setOrientation(LinearLayout.VERTICAL);

        mPlayer = player;

        mTrackInfoBar = (TrackInfoBar) findViewById(R.id.track_info_bar);
        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);

        mTrackInfoBar.addTextShadows();

        mArtwork = (ImageView) findViewById(R.id.artwork);
        if (mArtwork != null) {
            mArtwork.setVisibility(View.INVISIBLE);
            mArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            mLandscape = true;
        }

        mAvatar = (ImageView) findViewById(R.id.icon);
        mAvatar.setBackgroundDrawable(getResources().getDrawable(R.drawable.avatar_badge));
        mAvatar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mTrack != null) {
                    Intent intent = new Intent(getContext(), UserBrowser.class);
                    intent.putExtra("userId", mTrack.user_id);
                    getContext().startActivity(intent);
                }
            }
        });

        findViewById(R.id.private_indicator).setVisibility(View.GONE);

        if (findViewById(R.id.btn_info) != null) {
            findViewById(R.id.btn_info).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onTrackInfoFlip();
                }
            });
        }

        if (findViewById(R.id.btn_share) != null) {
            findViewById(R.id.btn_share).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mTrack != null) {
                        Intent intent = mTrack.getShareIntent();
                        if (intent != null) {
                            mPlayer.track(Page.Sounds_share, mTrack);

                            getContext().startActivity(Intent.createChooser(intent,
                                getContext().getString(R.string.share_track, mTrack.title)));
                        }
                    }
                }
            });
        }

        mCommentButton = (ImageButton) findViewById(R.id.btn_comment);
        if (mCommentButton != null) {
            mCommentButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleCommentMode();
                }
            });

            mFavoriteButton = (ImageButton) findViewById(R.id.btn_favorite);
            mFavoriteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleFavorite();
                }
            });
        }

        ((ProgressBar) findViewById(R.id.progress_bar)).setMax(1000);
        mWaveformController = (WaveformController) findViewById(R.id.waveform_controller);
        mWaveformController.setPlayerTrackView(this);

        ((View) findViewById(R.id.track).getParent()).setOnTouchListener(this);
        mTouchSlop = ViewConfiguration.get(mPlayer).getScaledTouchSlop();
    }

    public void setOnScreen(boolean onScreen){
        mOnScreen = onScreen;
        mWaveformController.setOnScreen(onScreen);
    }

    protected void toggleCommentMode() {
        mPlayer.toggleCommentMode(mPlayPos);
    }

    public void setTrack(Track track, int queuePosition, boolean forceUpdate, boolean priority) {
        mPlayPos = queuePosition;

        if (!forceUpdate && (mTrack != null && track != null && track.id == mTrack.id)) return;
        final boolean changed = mTrack == null ? track != null : !mTrack.equals(track);
        mTrack = track;
        if (mTrack == null) {
            mWaveformController.clearTrackComments();
            return;
        }

        if (changed && !mLandscape) updateArtwork(priority);
        mWaveformController.updateTrack(mTrack, priority);

        mTrackInfoBar.display(mTrack, false, -1, true, mPlayer.getCurrentUserId());
        if (mTrackInfo != null) mTrackInfo.setPlayingTrack(mTrack);
        updateAvatar(priority);

        if (mDuration != mTrack.duration) {
            mDuration = mTrack.duration;
        }

        setFavoriteStatus();

        if (!mTrack.full_track_info_loaded) {
            if (CloudUtils.isTaskFinished(mTrack.load_info_task)) {
                mTrack.load_info_task = new FetchTrackTask(mPlayer.getApp(), mTrack.id);
            }

            mTrack.load_info_task.addListener(this);
            if (CloudUtils.isTaskPending(mTrack.load_info_task)) {
                mTrack.load_info_task.execute(Request.to(Endpoints.TRACK_DETAILS, mTrack.id));
            }
        }

        if (mTrack.isStreamable() && mTrack.last_playback_error == -1) {
            hideUnplayable();
        } else {
            showUnplayable();
            mWaveformController.onBufferingStop();
        }

        if (changed) {
            mWaveformController.clearTrackComments();
            mWaveformController.setProgress(0);

            mPlayer.track(Page.Sounds_main, mTrack);

            if (mTrack.comments != null) {
                mWaveformController.setComments(mTrack.comments, true);
            } else {
                refreshComments();
            }

            if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
                onTrackInfoFlip();
            }
        }
    }

    void refreshComments() {
        if (mTrack == null) return;
        if (CloudUtils.isTaskFinished(mTrack.load_comments_task)) {
            mTrack.load_comments_task =
                    new LoadCommentsTask(mPlayer.getApp(), mTrack.id);
        }
        mTrack.load_comments_task.addListener(this);

        if (CloudUtils.isTaskPending(mTrack.load_comments_task)) {
            mTrack.load_comments_task.execute((Request) null);
        }
    }

    public void onCommentsLoaded(long track_id, List<Comment> comments){
        if (mTrack != null && mTrack.id == track_id){
            mTrack.comments = comments;
            mWaveformController.setComments(mTrack.comments, true);
        }
    }

    private void updateArtwork(boolean postAtFront) {
        if (TextUtils.isEmpty(mTrack.getArtwork())) {
            // no artwork
            ImageLoader.get(getContext()).unbind(mArtwork);
            mArtwork.setVisibility(View.INVISIBLE);
        } else {
            // executeAppendTask artwork as necessary
            if ((mCurrentArtBindResult = ImageUtils.loadImageSubstitute(
                    getContext(),
                    mArtwork,
                    mTrack.getArtwork(),
                    Consts.GraphicSize.T500, new ImageLoader.Callback() {
                @Override
                public void onImageError(ImageView view, String url, Throwable error) {
                    mCurrentArtBindResult = ImageLoader.BindResult.ERROR;
                    Log.e(getClass().getSimpleName(), "Error loading artwork " + error);
                }

                @Override
                public void onImageLoaded(ImageView view, String url) {
                    onArtworkSet(mOnScreen);
                }
            }, new ImageLoader.Options(true, postAtFront))) != ImageLoader.BindResult.OK) {
                mArtwork.setVisibility(View.INVISIBLE);
            } else {
                onArtworkSet(false);
            }
        }
    }

     private void onArtworkSet(boolean animate) {
        if (mArtwork.getVisibility() == View.INVISIBLE || mArtwork.getVisibility() == View.GONE) {
            if (animate) AnimUtils.runFadeInAnimationOn(getContext(), mArtwork);
            mArtwork.setVisibility(View.VISIBLE);
        }
    }

    private void updateAvatar(boolean postAtFront) {
        if (mTrack.hasAvatar()) {
            if ((mCurrentAvatarBindResult = ImageLoader.get(mPlayer).bind(
                    mAvatar,
                    Consts.GraphicSize.formatUriForList(mPlayer, mTrack.getAvatarUrl()),
                    new ImageLoader.Callback() {
                        @Override
                        public void onImageError(ImageView view, String url, Throwable error) {
                            mCurrentAvatarBindResult = ImageLoader.BindResult.ERROR;
                        }

                        @Override
                        public void onImageLoaded(ImageView view, String url) {
                        }
                    }, new ImageLoader.Options(true, postAtFront))) != ImageLoader.BindResult.OK) {
            }
        } else {
            ImageLoader.get(mPlayer).unbind(mAvatar);
        }
    }

    private void onTrackInfoFlip() {
        if (mTrackFlipper.getDisplayedChild() == 0) {
            if (mTrack != null) {
                mPlayer.track(Page.Sounds_info__main, mTrack);
            }

            mWaveformController.closeComment(false);

            if (mTrackInfo == null) {
                mTrackInfo = new PlayerTrackInfo(mPlayer);
                mTrackInfo.setPlayingTrack(mTrack);
                mTrackFlipper.addView(mTrackInfo);
            }

            if (!mTrackInfo.getIsTrackInfoFilled()) mTrackInfo.fillTrackDetails();

            mTrackFlipper.setInAnimation(AnimUtils.inFromRightAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.setOutAnimation(AnimUtils.outToLeftAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.showNext();
        } else {
            mTrackFlipper.setInAnimation(AnimUtils.inFromLeftAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.setOutAnimation(AnimUtils.outToRightAnimation(new AccelerateDecelerateInterpolator()));
            mTrackFlipper.showPrevious();
        }
    }

    public void onDataConnected() {
        if (mWaveformController.waveformResult == ImageLoader.BindResult.ERROR) {
            mWaveformController.updateTrack(mTrack, mOnScreen);
        }
        if (!mLandscape && mCurrentArtBindResult == ImageLoader.BindResult.ERROR) {
            updateArtwork(mOnScreen);
        }
        if (mCurrentAvatarBindResult == ImageLoader.BindResult.ERROR) {
            updateAvatar(mOnScreen);
        }
    }

    private void setFavoriteStatus() {
        if (mTrack == null || mFavoriteButton == null) {
            return;
        }

        if (mTrack.user_favorite) {
            if (mFavoritedDrawable == null) mFavoritedDrawable = getResources().getDrawable(R.drawable.ic_liked_states_v1);
            mFavoriteButton.setImageDrawable(mFavoritedDrawable);
        } else {
            if (mFavoriteDrawable == null) mFavoriteDrawable = getResources().getDrawable(R.drawable.ic_like_states_v1);
            mFavoriteButton.setImageDrawable(mFavoriteDrawable);
        }
    }

    private void toggleFavorite() {
        if (mTrack == null)
            return;

        mPlayer.toggleFavorite(mTrack);
    }



    /**
     * Handle text dragging for viewing of long track names
     */
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        TextView tv = textViewForContainer(v);
        if (tv == null) {
            return false;
        }
        if (action == MotionEvent.ACTION_DOWN) {
            mInitialX = mLastX = (int) event.getX();
            mDraggingLabel = false;
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mDraggingLabel) {
                Message msg = mLabelScroller.obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(msg, 1000);
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mDraggingLabel) {
                int scrollx = tv.getScrollX();
                int x = (int) event.getX();
                int delta = mLastX - x;
                if (delta != 0) {
                    mLastX = x;
                    scrollx += delta;
                    if (scrollx > mTextWidth) {
                        // scrolled the text completely off the view to the left
                        scrollx -= mTextWidth;
                        scrollx -= mViewWidth;
                    }
                    if (scrollx < -mViewWidth) {
                        // scrolled the text completely off the view to the right
                        scrollx += mViewWidth;
                        scrollx += mTextWidth;
                    }
                    tv.scrollTo(scrollx, 0);
                }
                return true;
            }
            int delta = mInitialX - (int) event.getX();
            if (Math.abs(delta) > mTouchSlop) {
                // start moving
                mLabelScroller.removeMessages(0, tv);

                // Only turn ellipsizing off when it's not already off, because it
                // causes the scroll position to be reset to 0.
                if (tv.getEllipsize() != null) {
                    tv.setEllipsize(null);
                }
                Layout ll = tv.getLayout();
                // layout might be null if the text just changed, or ellipsizing was just turned off
                if (ll == null) {
                    return false;
                }
                // get the non-ellipsized line width, to determine whether
                // scrolling should even be allowed
                mTextWidth = (int) tv.getLayout().getLineWidth(0);
                mViewWidth = tv.getWidth();
                if (mViewWidth > mTextWidth) {
                    tv.setEllipsize(TextUtils.TruncateAt.END);
                    v.cancelLongPress();
                    return false;
                }
                mDraggingLabel = true;
                tv.setHorizontalFadingEdgeEnabled(true);
                v.cancelLongPress();
                return true;
            }
        }
        return false;
    }

    private final Handler mLabelScroller = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tv = (TextView) msg.obj;
            int x = tv.getScrollX();
            x = x * 3 / 4;
            tv.scrollTo(x, 0);
            if (x == 0) {
                tv.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                Message newmsg = obtainMessage(0, tv);
                mLabelScroller.sendMessageDelayed(newmsg, 15);
            }
        }
    };

    private TextView textViewForContainer(View v) {
        View vv = v.findViewById(R.id.track);
        if (vv != null) {
            return (TextView) vv;
        }
        return null;
    }



    public void setCommentMode(boolean mIsCommenting) {
        getWaveformController().setCommentMode(mIsCommenting);
        if (mCommentButton != null) {
            if (mIsCommenting) {
                mCommentButton.setImageResource(R.drawable.ic_commenting_states_v1);
            } else {
                mCommentButton.setImageResource(R.drawable.ic_comment_states_v1);
            }
        }
    }

    public int getPlayPosition() {
        return mPlayPos;
    }

    public void onDestroy() {
        if (mWaveformController != null) {
            mWaveformController.onDestroy();
        }
    }

    public boolean waveformVisible(){
        return (mTrackFlipper == null || mTrackFlipper.getDisplayedChild() == 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mTrackFlipper != null && keyCode == KeyEvent.KEYCODE_BACK &&
             mTrackFlipper.getDisplayedChild() != 0) {
            onTrackInfoFlip();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    public WaveformController getWaveformController() {
        return mWaveformController;
    }

    private void showUnplayable() {
        if (mUnplayableLayout == null) {
            mUnplayableLayout = (FrameLayout) ((ViewStub) findViewById(R.id.stub_unplayable_layout)).inflate();
        }

        if (mUnplayableLayout != null){
            // sometimes inflation error results in text NPE
            final TextView unplayableText = (TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt);

            if (unplayableText != null)  {
                if (mTrack == null || mTrack.isStreamable()) {
                    unplayableText.setText(
                            mTrack != null && mTrack.last_playback_error == 0 ?
                            R.string.player_error : R.string.player_stream_error);
                } else {
                    unplayableText.setText(R.string.player_not_streamable);
                }
            }
        }

        mWaveformController.setVisibility(View.GONE);
        mUnplayableLayout.setVisibility(View.VISIBLE);

    }

    private void hideUnplayable() {
        mWaveformController.setVisibility(View.VISIBLE);
        if (mUnplayableLayout != null) mUnplayableLayout.setVisibility(View.GONE);
    }

    public void handleIdBasedIntent(Intent intent) {
        if (mTrack != null && mTrack.id == intent.getLongExtra("id", -1)) handleStatusIntent(intent);
    }

    public void handleStatusIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals(CloudPlaybackService.PLAYSTATE_CHANGED)) {

            if (intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isSupposedToBePlaying, false)) {
                hideUnplayable();
                if (mTrack != null) mTrack.last_playback_error = -1;
            } else {
                mWaveformController.setPlaybackStatus(false, intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
            }
        } else if (action.equals(CloudPlaybackService.FAVORITE_SET)) {
            if (mTrack != null && mTrack.id == intent.getLongExtra(CloudPlaybackService.BroadcastExtras.id, -1)) {
                mTrack.user_favorite = intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isFavorite, false);
                setFavoriteStatus();
            }
        } else if (action.equals(CloudPlaybackService.BUFFERING)) {
            onBuffering();
        } else if (action.equals(CloudPlaybackService.BUFFERING_COMPLETE)) {
            mWaveformController.onBufferingStop();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
        } else if (action.equals(CloudPlaybackService.PLAYBACK_ERROR)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.PLAYBACK_ERROR;
            mWaveformController.onBufferingStop();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
            showUnplayable();
        } else if (action.equals(CloudPlaybackService.STREAM_DIED)) {
            mTrack.last_playback_error = ScPlayer.PlayerError.STREAM_ERROR;
            mWaveformController.onBufferingStop();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isPlaying, false),
                    intent.getLongExtra(CloudPlaybackService.BroadcastExtras.position, 0));
            showUnplayable();
        } else if (action.equals(CloudPlaybackService.COMMENTS_LOADED)) {
            mWaveformController.setComments(mTrack.comments, true);
        } else if (action.equals(CloudPlaybackService.SEEK_COMPLETE)) {
            mWaveformController.onSeekComplete();
        }
    }

    public void onNewComment(Comment comment) {
        if (comment.track_id == mTrack.id) {
            if (mTrack.comments != null) mWaveformController.setComments(mTrack.comments, false, true);
            mWaveformController.showNewComment(comment);
        }
    }


    public void setProgress(long pos, int loadPercent, boolean showSmoothProgress) {
        if (pos >= 0 && mDuration > 0) {
            mWaveformController.setProgress(pos);
            mWaveformController.setSecondaryProgress(loadPercent * 10);
        } else {
            mWaveformController.setProgress(0);
            mWaveformController.setSecondaryProgress(0);
        }

        // Start showing smooth progress if we already aren't
        if (!mWaveformController.showingSmoothProgress() && showSmoothProgress){
            mWaveformController.startSmoothProgress();
        }
    }

    public void onRefresh() {
        if (mTrackInfo != null) {
            mTrackInfo.clearIsTrackInfoFilled();
            mTrackInfo.fillTrackDetails();
        }
        refreshComments();
    }

    public void onStop(boolean killLoading) {
        mWaveformController.onStop(killLoading);
    }

    public void onBuffering() {
        if (mTrack != null) {
            mTrack.last_playback_error = -1;
            hideUnplayable();
            mWaveformController.onBufferingStart();
            mWaveformController.stopSmoothProgress();
        }
    }

    public void setPlaybackStatus(boolean isPlaying, long position) {
        mWaveformController.setPlaybackStatus(isPlaying,position);
    }

    public long getTrackId() {
        return mTrack == null ? -1 : mTrack.id;
    }

    public void destroy() {
        // need a safer method of recycling, this crashes the app as the bitmaps are still stored in the imageloader
        /*if (mArtwork != null && mArtwork.getDrawable() instanceof BitmapDrawable) ((BitmapDrawable) mArtwork.getDrawable()).getBitmap().recycle();
        if (mAvatar.getDrawable() instanceof BitmapDrawable) ((BitmapDrawable) mAvatar.getDrawable()).getBitmap().recycle();*/
        clear();
        mWaveformController.onDestroy();
    }

    public void clear() {
        mOnScreen = false;
        onStop(true);
        if (mArtwork != null) mArtwork.setImageBitmap(null);
        mAvatar.setImageBitmap(null);
        mWaveformController.reset(true);
        mWaveformController.setOnScreen(false);
    }

    @Override
    public void onSuccess(Track t, String action) {
        if (t.id != mTrack.id) return;

        setTrack(t, mPlayPos, true, mOnScreen);
        if (mTrackInfo != null) {
            mTrackInfo.onInfoLoadSuccess();
        }
    }

    @Override
    public void onError(long trackId) {
        if (trackId != mTrack.id) return;
        if (mTrackInfo != null){
            mTrackInfo.onInfoLoadError();
        }
    }
}
