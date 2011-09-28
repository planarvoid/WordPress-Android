package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.task.LoadCommentsTask;
import com.soundcloud.android.task.LoadTrackInfoTask;
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

public class PlayerTrackView extends LinearLayout implements View.OnTouchListener, LoadTrackInfoTask.LoadTrackInfoListener, LoadCommentsTask.LoadCommentsListener {

    private ScPlayer mPlayer;

    private ImageView mArtwork;
    private ImageLoader.BindResult mCurrentArtBindResult;
    private ImageButton mFavoriteButton;
    private ImageView mAvatar;
    private ImageButton mCommentButton;

    private WaveformController mWaveformController;
    private FrameLayout mUnplayableLayout;

    private TrackInfoBar mTrackInfoBar;
    private ViewFlipper mTrackFlipper;
    private PlayerTrackInfo mTrackInfo;
    private FlowLayout mTrackTags;

    private boolean mDraggingLabel = false;
    private int mInitialX = -1;
    private int mLastX = -1;
    private int mTextWidth = 0;
    private int mViewWidth = 0;
    private int mTouchSlop;

    private int mCurrentTrackError;
    private String mCurrentPath;
    private String mCurrentDurationString;
    private ImageLoader.BindResult mCurrentAvatarBindResult;
    private Drawable mFavoriteDrawable, mFavoritedDrawable;

    private Track mTrack;
    private int mPlayPos;
    private long mDuration;

    public PlayerTrackView(ScPlayer player) {
        super(player);

        LayoutInflater inflater = (LayoutInflater) player.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.player_track, this);

         setOrientation(LinearLayout.VERTICAL);

        mPlayer = player;

        mTrackInfoBar = (TrackInfoBar) findViewById(R.id.track_info_bar);
        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);

        mArtwork = new ImageView(player);
        mArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mArtwork.setVisibility(View.INVISIBLE);

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

        if (findViewById(R.id.btn_info) != null){
            findViewById(R.id.btn_info).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onTrackInfoFlip();
                }
            });
        }


        if (findViewById(R.id.btn_share) != null){
        findViewById(R.id.btn_share).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mTrack == null || !mTrack.sharing.contentEquals("public")) return;
                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mTrack.title + " by " + mTrack.user.username + " on SoundCloud");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, mTrack.permalink_url);
                getContext().startActivity(Intent.createChooser(shareIntent, "Share: " + mTrack.title));
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
        }

        ((View) findViewById(R.id.track).getParent()).setOnTouchListener(this);
        mWaveformController = (WaveformController) findViewById(R.id.waveform_controller);
        mWaveformController.setPlayerTrackView(this);

        ProgressBar mProgress = (ProgressBar) findViewById(R.id.progress_bar);
        mProgress.setMax(1000);
        mProgress.setInterpolator(new AccelerateDecelerateInterpolator());

        mTouchSlop = ViewConfiguration.get(mPlayer).getScaledTouchSlop();

    }

    protected void toggleCommentMode() {
        mPlayer.toggleCommentMode(mPlayPos);
    }

    public void setTrack(Track track, int queuePosition, boolean forceUpdate) {
        mPlayPos = queuePosition;

        if (forceUpdate || (mTrack == null || track == null || track.id != mTrack.id)) {

            boolean changed = mTrack != track;

            mTrack = track;

            if (mTrack == null) {
                mWaveformController.clearTrackComments();
                return;
            }

            mWaveformController.updateTrack(mTrack);

            mTrackInfoBar.display(mTrack, false, -1, true);
            if (mTrackInfo != null) mTrackInfo.setPlayingTrack(mTrack);
            updateAvatar();

            if (mDuration != mTrack.duration) {
                mDuration = mTrack.duration;
                if (mDuration != 0) {
                    mCurrentDurationString = CloudUtils.formatTimestamp(mDuration);
                }
            }

            setFavoriteStatus();

            if (!mTrack.info_loaded) {
                if (CloudUtils.isTaskFinished(mTrack.load_info_task)) {
                    mTrack.load_info_task = new LoadTrackInfoTask(mPlayer.getApp(), mTrack.id, true, true);
                }

                mTrack.load_info_task.setListener(this);
                if (CloudUtils.isTaskPending(mTrack.load_info_task)) {
                    mTrack.load_info_task.execute(Request.to(Endpoints.TRACK_DETAILS, mTrack.id));
                }
            }

            if (changed) {
                mWaveformController.clearTrackComments();

                if (mTrack.user != null && TextUtils.isEmpty(mTrack.user.username)) {
                    mPlayer.trackPage(mTrack.pageTrack());
                }


                if (mTrack.comments != null) {
                    setCurrentComments(true);
                } else {
                    refreshComments();
                }

                if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
                    onTrackInfoFlip();
                }

                if (mCurrentTrackError >= 0)
                    return;

                if (mTrack.isStreamable()) {
                    hideUnplayable();
                } else {
                    showUnplayable();
                    mWaveformController.hideConnectingLayout();
                }

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
            setCurrentComments(true);
        }
    }

    private void setCurrentComments(boolean animateIn){
        mWaveformController.setComments(mTrack.comments, animateIn);
        if (mTrackInfo != null) {
            mTrackInfo.clearIsTrackInfoCommentsFilled();
            if (mTrackFlipper != null && mTrackFlipper.getDisplayedChild() == 1) {
                mTrackInfo.fillTrackInfoComments();
            }
        }

    }

    private void updateArtwork() {
        if (TextUtils.isEmpty(mCurrentPath)) {
            // no artwork
            ImageLoader.get(getContext()).unbind(mArtwork);
            mArtwork.setVisibility(View.INVISIBLE);
        } else {
            // load artwork as necessary
            if ((mCurrentArtBindResult = ImageUtils.loadImageSubstitute(
                    getContext(),
                    mArtwork,
                    mCurrentPath,
                    Consts.GraphicSize.T500, new ImageLoader.ImageViewCallback() {
                @Override
                public void onImageError(ImageView view, String url, Throwable error) {
                    mCurrentArtBindResult = ImageLoader.BindResult.ERROR;
                    Log.e(getClass().getSimpleName(), "Error loading artwork " + error);
                }

                @Override
                public void onImageLoaded(ImageView view, String url) {
                    onArtworkSet();
                }
            }, null)) != ImageLoader.BindResult.OK) {
                mArtwork.setVisibility(View.INVISIBLE);
            } else {
                onArtworkSet();
            }
        }
    }

     private void onArtworkSet() {
        if (mArtwork.getVisibility() == View.INVISIBLE || mArtwork.getVisibility() == View.GONE) {
            AnimUtils.runFadeInAnimationOn(getContext(), mArtwork);
            mArtwork.setVisibility(View.VISIBLE);
        }

    }

    private void updateAvatar() {
        if (mTrack.hasAvatar()) {
            if ((mCurrentAvatarBindResult = ImageLoader.get(mPlayer).bind(
                    mAvatar,
                    ImageUtils.formatGraphicsUriForList(mPlayer, mTrack.user.avatar_url),
                    new ImageLoader.ImageViewCallback() {
                        @Override
                        public void onImageError(ImageView view, String url, Throwable error) {
                            mCurrentAvatarBindResult = ImageLoader.BindResult.ERROR;
                        }

                        @Override
                        public void onImageLoaded(ImageView view, String url) {
                        }
                    })) != ImageLoader.BindResult.OK) {
            }
        } else {
            ImageLoader.get(mPlayer).unbind(mAvatar);
        }
    }

    private void onTrackInfoFlip() {
        if (mTrackFlipper.getDisplayedChild() == 0) {
            mWaveformController.closeComment(false);

            if (mTrackInfo == null) {
                mTrackInfo = new PlayerTrackInfo(mPlayer);
                mTrackInfo.setPlayingTrack(mTrack);
                mTrackFlipper.addView(mTrackInfo);
            }

            if (!mTrackInfo.getIsTrackInfoFilled()) mTrackInfo.fillTrackDetails();
            if (!mTrackInfo.getIsTrackInfoCommentsFilled()) mTrackInfo.fillTrackInfoComments();


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
            mWaveformController.updateTrack(mTrack);
        }
        if (mCurrentArtBindResult == ImageLoader.BindResult.ERROR) {
            updateArtwork();
        }
        if (mCurrentAvatarBindResult == ImageLoader.BindResult.ERROR) {
                updateAvatar();
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

        if (mPlayer.toggleFavorite(mTrack)){
            mFavoriteButton.setEnabled(false);
        }
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

    @Override
    public void onTrackInfoLoaded(Track track, String action) {
        if (track.id != mTrack.id) return;

        setTrack(track, mPlayPos, true);
        if (mTrackInfo != null) {
            mTrackInfo.onInfoLoadSuccess();
        }
    }

    @Override
    public void onTrackInfoError(long trackId) {
        if (trackId != mTrack.id) return;
        if (mTrackInfo != null){
            mTrackInfo.onInfoLoadError();
        }
    }

    public void setCommentMode(boolean mIsCommenting) {
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

        if (mTrack == null || mTrack.isStreamable()) {
            ((TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt))
                    .setText(mCurrentTrackError == 0 ? R.string.player_error : R.string.player_stream_error);
        } else {
            ((TextView) mUnplayableLayout.findViewById(R.id.unplayable_txt))
                    .setText(R.string.player_not_streamable);
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
            if (intent.getBooleanExtra("isSupposedToBePlaying", false)) {
                hideUnplayable();
                mCurrentTrackError = -1;
            } else {
                mWaveformController.setPlaybackStatus(false, intent.getLongExtra("position", 0));
            }
        } else if (action.equals(CloudPlaybackService.FAVORITE_SET)) {
            if (mTrack != null && mTrack.id == intent.getLongExtra("id", -1)) {
                mTrack.user_favorite = intent.getBooleanExtra("isFavorite", false);
                if (mFavoriteButton != null) mFavoriteButton.setEnabled(true);
                setFavoriteStatus();
            }
        } else if (action.equals(CloudPlaybackService.INITIAL_BUFFERING)) {
            mCurrentTrackError = -1;
            hideUnplayable();
            mWaveformController.showConnectingLayout();
        } else if (action.equals(CloudPlaybackService.BUFFERING)) {
            hideUnplayable();
            mWaveformController.showConnectingLayout();
        } else if (action.equals(CloudPlaybackService.BUFFERING_COMPLETE)) {
            mWaveformController.hideConnectingLayout();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra("isPlaying", false), intent.getLongExtra("position", 0));
        } else if (action.equals(CloudPlaybackService.PLAYBACK_ERROR)) {
            mCurrentTrackError = ScPlayer.PlayerError.PLAYBACK_ERROR;
            mWaveformController.hideConnectingLayout();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra("isPlaying", false), intent.getLongExtra("position", 0));
            showUnplayable();
        } else if (action.equals(CloudPlaybackService.STREAM_DIED)) {
            mCurrentTrackError = ScPlayer.PlayerError.STREAM_ERROR;
            mWaveformController.hideConnectingLayout();
            mWaveformController.setPlaybackStatus(intent.getBooleanExtra("isPlaying", false), intent.getLongExtra("position", 0));
            showUnplayable();
        } else if (action.equals(CloudPlaybackService.COMMENTS_LOADED)) {
            setCurrentComments(true);
        } else if (action.equals(CloudPlaybackService.SEEK_COMPLETE)) {
            // setPauseButtonImage();
        } else if (action.equals(Consts.IntentActions.COMMENT_ADDED)) {
            final Comment c = intent.getParcelableExtra("comment");
            if (c.track_id == mTrack.id) {
                setCurrentComments(true);
                mWaveformController.showNewComment(c);
            }
        }
    }


    public void setProgress(long pos, int loadPercent) {
        if (pos >= 0 && mDuration > 0) {
                mWaveformController.setProgress(pos);
                mWaveformController.setSecondaryProgress(loadPercent * 10);
            } else {
                mWaveformController.setProgress(0);
                mWaveformController.setSecondaryProgress(0);
            }
    }

    public void onRefresh() {
        if (mTrackInfo != null) {
            mTrackInfo.clearIsTrackInfoFilled();
            mTrackInfo.clearIsTrackInfoCommentsFilled();
            mTrackInfo.fillTrackDetails();
            mTrackInfo.fillTrackInfoComments();
        }
        refreshComments();
    }

    public void onStop() {
        mWaveformController.onStop();
    }
}
