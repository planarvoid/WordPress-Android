
package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class WaveformController extends RelativeLayout implements OnTouchListener, OnLongClickListener {
    private static final String TAG = "WaveformController";

    private Track mPlayingTrack;

    private PlayerAvatarBar mPlayerAvatarBar;

    private RelativeLayout mPlayerCommentBar;

    private ArrayList<Comment> mCurrentComments;

    private ArrayList<Comment> mCurrentTopComments;

    private Comment mCurrentShowingComment;

    private boolean mShowingComments;

    private ImageView mOverlay;

    private ProgressBar mProgressBar;

    private RelativeLayout mTrackTouchBar;

    private WaveformHolder mWaveformHolder;

    private RelativeLayout mWaveformFrame;

    private RelativeLayout mConnectingBar;

    private WaveformCommentLines mCommentLines;

    private ScPlayer mPlayer;

    private int mDuration;

    private Boolean mLandscape = false;

    private ImageLoader.BindResult waveformResult;

    final Handler mHandler = new Handler();

    private Comment mLastAutoComment;

    private CommentBubble mCommentBubble;

    private Animation mBubbleAnimation;

    private Animation mConnectingAnimation;

    // These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();

    Matrix savedMatrix = new Matrix();

    float oldDist;

    PointF start = new PointF();

    PointF fake = new PointF();

    PointF mid = new PointF();

    private int mAvatarOffsetY;

    private int mCommentBarOffsetY;

    private Comment mNextComment;

    static final int SEEK_TOLERANCE = 10;

    static final double TOUCH_MOVE_TOLERANCE = 2.0;

    static final int NONE = 0;

    static final int SEEK_DRAG = 1;

    static final int COMMENT_DRAG = 2;

    static final int AVATAR_DRAG = 3;

    long mLastMoveTime = 0;
    int mTouchX;
    int mTouchY;

    int mode = NONE;


    SharedPreferences mPreferences;

    public WaveformController(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setWillNotDraw(false);

        mPlayer = (ScPlayer) context;

        mPreferences = PreferenceManager.getDefaultSharedPreferences(mPlayer);

        mShowingComments = mPreferences.getBoolean("playerShowingCOmments", true);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.waveformcontroller, this);

        mConnectingBar = (RelativeLayout) findViewById(R.id.connecting_bar);
        mWaveformFrame = (RelativeLayout) findViewById(R.id.waveform_frame);
        mWaveformHolder = (WaveformHolder) findViewById(R.id.waveform_holder);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mOverlay = (ImageView) findViewById(R.id.progress_overlay);

        mPlayerAvatarBar =(PlayerAvatarBar) findViewById(R.id.player_avatar_bar);
        mPlayerCommentBar =(RelativeLayout) findViewById(R.id.new_comment_bar);
        ImageButton mToggleComments = (ImageButton) findViewById(R.id.btn_toggle);

        mTrackTouchBar = (RelativeLayout) findViewById(R.id.track_touch_bar);
        mTrackTouchBar.setOnTouchListener(this);

        if (mPlayerAvatarBar != null){
            mPlayerAvatarBar.setOnTouchListener(this);
            mPlayerAvatarBar.setVisibility(mShowingComments ? View.INVISIBLE : View.GONE);
        }
        if (mPlayerCommentBar != null){
            mPlayerCommentBar.setOnTouchListener(this);
        }

        if (mToggleComments != null)
            mToggleComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleComments();
                }
            });

        LightingColorFilter lcf = new LightingColorFilter(1, mPlayer.getResources().getColor(
                R.color.white));
        mOverlay.setBackgroundColor(Color.TRANSPARENT);
        mOverlay.setColorFilter(lcf);
        mOverlay.setScaleType(ScaleType.FIT_XY);

        File dirFile = new File(CloudUtils.getCacheDirPath(mPlayer) + "/waves/");
        if (!dirFile.mkdirs()) Log.w(TAG, "error creating " + dirFile);
    }

    public void showConnectingLayout() {
        if (mConnectingAnimation != null)
            mConnectingAnimation.cancel();
        mConnectingBar.setVisibility(View.VISIBLE);
    }

    public void hideConnectingLayout() {
        mConnectingBar.setVisibility(View.GONE);


    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l,t,r,b);

        if (changed && mLandscape){

            int[] calc = new int[2];

            mPlayer.getCommentHolder().getLocationInWindow(calc);
            int topOffset = calc[1];

            if (mPlayerAvatarBar != null){
                mPlayerAvatarBar.getLocationInWindow(calc);
                mAvatarOffsetY = calc[1] - topOffset;
            }

            if (mPlayerCommentBar != null){
                mPlayerCommentBar.getLocationInWindow(calc);
                mCommentBarOffsetY = calc[1] - topOffset;
            }
        }
    }


    public void setLandscape(boolean isLandscape) {
        mLandscape = isLandscape;

        if (!mLandscape){
            this.setStaticTransformationsEnabled(true);
        } else {
            if (mCommentLines == null){
                mCommentLines = new WaveformCommentLines(mPlayer, null);
                mCommentLines.setVisibility(mShowingComments ? View.INVISIBLE : View.GONE);
                mWaveformHolder.addView(mCommentLines);
            }
        }
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        boolean ret = super.getChildStaticTransformation(child, t);
        if (child == mWaveformFrame) {
            t.setAlpha((float) 0.7);
            return true;
        }
        return ret;
    }

    public void setProgress(long pos) {
        if (mDuration == 0)
            return;

        mProgressBar.setProgress((int) (1000 * pos / mDuration));

        if (mLandscape && mode == NONE && mCurrentTopComments != null){
            Comment last = lastCommentBeforeTimestamp(pos);
            if (last != null){
                if (mLastAutoComment != last && pos - last.timestamp < 2000){
                    mLastAutoComment = last;
                    if (mCurrentShowingComment == null && mPlayer.waveformVisible()){
                        mCurrentShowingComment = last;
                        showCurrentComment(true);
                        mHandler.postDelayed(mAutoCloseBubble, 3000);
                    }
                }
            }
        }
    }

    public void setSecondaryProgress(int percent) {
        mProgressBar.setSecondaryProgress(percent);
    }

    public void updateTrack(Track track) {
        if (mPlayingTrack != null &&
                mPlayingTrack.id == track.id
                && waveformResult != BindResult.ERROR) {
            return;
        }

        mPlayingTrack = track;
        mDuration = mPlayingTrack.duration;

        if (waveformResult != BindResult.ERROR) {
            // clear loader errors so we can try to reload
            ImageLoader.get(mPlayer).clearErrors();
        }
        waveformResult = ImageLoader.get(mPlayer).bind(mOverlay, track.waveform_url,
                new ImageLoader.ImageViewCallback() {
                    @Override
                    public void onImageError(ImageView view, String url, Throwable error) {
                        waveformResult = BindResult.ERROR;
                        mOverlay.setImageDrawable(mPlayer.getResources()
                                .getDrawable(R.drawable.player_wave_bg));
                    }

                    @Override
                    public void onImageLoaded(ImageView view, String url) {
                        showWaveform();
                    }
                });

        switch (waveformResult) {
            case OK:      showWaveform(); break;
            case LOADING: mOverlay.setVisibility(View.INVISIBLE); break;
            case ERROR:
                mOverlay.setImageDrawable(mPlayer.getResources()
                        .getDrawable(R.drawable.player_wave_bg));
                break;
        }
    }

    public void onStop() {
        if (mPlayerAvatarBar != null) mPlayerAvatarBar.onStop(); //stops avatar loading
    }

    private void showWaveform(){
        if (mOverlay.getVisibility() == View.INVISIBLE){
            AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
            aa.setDuration(500);

            mOverlay.startAnimation(aa);
            mOverlay.setVisibility(View.VISIBLE);
        }
    }

    public BindResult currentWaveformResult() {
        return waveformResult;
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (v == mPlayerAvatarBar) {
                    if (mCommentBubble == null) {
                        mCommentBubble = new CommentBubble(mPlayer, this);
                    }

                    if (mCurrentComments != null) {
                        mode = AVATAR_DRAG;
                        calcAvatarHit(event.getX(), true);
                    }
                } else if (v == mPlayerCommentBar){
                    if (mCommentBubble == null) {
                        mCommentBubble = new CommentBubble(mPlayer, this);
                    }

                    mode = COMMENT_DRAG;

                    mCurrentShowingComment = null;
                    mCommentBubble.onNewCommentMode(mPlayingTrack, stampFromPosition((int) event.getX()));
                   showBubbleAt((int) event.getX(),mCommentBarOffsetY, true);

                } else {
                    Log.i(TAG,"SSSSEEK DRAG");
                    mode = SEEK_DRAG;
                    if (mPlayer != null && mPlayer.isSeekable()) {
                        mLastAutoComment = null; //reset auto comment in case they seek backward

                            setProgress(mPlayer
                                    .setSeekMarker(event.getX() / mWaveformHolder.getWidth()));
                        mWaveformHolder.invalidate();
                    }

                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mTouchX == (int) event.getX() && mTouchY == (int) event.getY() && System.currentTimeMillis() - mLastMoveTime > 100)
                    return true;

                mLastMoveTime = System.currentTimeMillis();
                mTouchX = (int) event.getX();
                mTouchY = (int) event.getY();

                switch (mode){
                    case SEEK_DRAG :
                        Log.i(TAG,"SSSSEEK DRAG " + mPlayer.isSeekable());
                        if (mPlayer != null && mPlayer.isSeekable()) {
                            if (mPlayer != null)
                                setProgress(mPlayer.setSeekMarker(event.getX()
                                        / mWaveformHolder.getWidth()));
                            mWaveformHolder.invalidate();
                        }
                        break;
                    case COMMENT_DRAG :
                        mCommentBubble.updateNewCommentTime(stampFromPosition(mTouchX));
                        moveBubbleTo(mTouchX, mCommentBarOffsetY);
                        break;
                    case AVATAR_DRAG :
                        calcAvatarHit(mTouchX, false);
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                switch (mode){
                    case SEEK_DRAG :
                        if (mPlayer != null) mPlayer.sendSeek();
                        break;
                    case COMMENT_DRAG :
                        if (mBubbleAnimation == null){
                            if (Math.abs(mPlayerCommentBar.getTop() - (int) event.getY()) < 200)
                                mPlayer.addNewComment(mPlayingTrack,stampFromPosition(mTouchX), mPlayer.addCommentListener);
                        }
                        removeBubble();
                        break;
                    case AVATAR_DRAG :
                        //mCommentBubble.setPosition(event.getX(), event.getY());
                        break;
                }
                    mode = NONE;
                break;
        }

        return true; // indicate event was handled
    }


    private long stampFromPosition(int x){
        return (long) (Math.min(Math.max(.001, (((float)x)/getWidth())),1) * mPlayingTrack.duration);
    }

    private void showBubbleAt(int xPos, int yPos){
        showBubbleAt(xPos,yPos,false);
    }

    private void showBubbleAt(int xPos, int yPos, boolean forceAnimation){
        if (mCommentBubble == null) return;

        float offsetX = mCommentBubble.setPosition(xPos,yPos,getWidth());
        mCommentBubble.closing = false;

        if (mCommentBubble.getParent() != mPlayer.getCommentHolder() || forceAnimation){
            if (mCommentBubble.getParent() != mPlayer.getCommentHolder())
                    mPlayer.getCommentHolder().addView(mCommentBubble);

            if (mBubbleAnimation != null)
                mBubbleAnimation.cancel();

            mBubbleAnimation = new ScaleAnimation((float).6, (float)1.0, (float).6, (float)1.0, Animation.RELATIVE_TO_SELF, offsetX,Animation.RELATIVE_TO_SELF, (float) 1.0);
            mBubbleAnimation.setFillAfter(true);
            mBubbleAnimation.setDuration(100);
            mBubbleAnimation.setAnimationListener(new AnimationListener(){

                @Override
                public void onAnimationEnd(Animation arg0) {
                    mBubbleAnimation = null;
                    if (mode == NONE && mCurrentShowingComment == null)
                        removeBubble();
                }

                @Override
                public void onAnimationRepeat(Animation arg0) { }

                @Override
                public void onAnimationStart(Animation arg0) {

                }});
            mCommentBubble.startAnimation(mBubbleAnimation);
        }


    }
    private void moveBubbleTo(int xPos, int yPos){
        if (mCommentBubble == null) return;
        mCommentBubble.setPosition(xPos,yPos,getWidth());
    }

    public void closeComment(){
        mCurrentShowingComment = null;
        if (mPlayerAvatarBar != null) mPlayerAvatarBar.setCurrentComment(null);
        if (mCommentLines != null) mCommentLines.setCurrentComment(null);
        removeBubble();
    }

    private void removeBubble(){
        if (mCommentBubble != null && mCommentBubble.getParent() != null){
            ((ViewGroup) mCommentBubble.getParent()).removeView(mCommentBubble);
        }
    }


    private void calcAvatarHit(float xPos, boolean down){
        Comment skipComment = null;
        if (mCurrentShowingComment != null){
            if (isHitting(mCurrentShowingComment,xPos)){
                if (down)
                    skipComment = mCurrentShowingComment;
                else
                    return;
            } else {
                if (!mCommentBubble.closing){
                    mCommentBubble.interacted = false;
                    mHandler.postDelayed(mAutoCloseBubble, 500);
                }
            }
        }

        Comment c =isHitting(xPos, skipComment);
        if (c != null){
            mCurrentShowingComment = c;
            showCurrentComment();
        }

    }

    private void showCurrentComment(){
        showCurrentComment(false);
    }

    private void showCurrentComment(boolean waitForInteraction){
        if (mCommentBubble == null) return;

        if (mCurrentShowingComment != null){
            mCommentBubble.onShowCommentMode(mCurrentShowingComment);
            mCommentBubble.interacted = !waitForInteraction;
            mPlayerAvatarBar.setCurrentComment(mCurrentShowingComment);
            mCommentLines.setCurrentComment(mCurrentShowingComment);
            showBubbleAt(mCurrentShowingComment.xPos+mPlayerAvatarBar.getAvatarWidth()/2,mAvatarOffsetY);

        }
    }

    final Runnable mAutoCloseBubble = new Runnable() {
        public void run() {
            if (mCommentBubble != null && mCurrentShowingComment != null)
            if (mCurrentShowingComment == mCommentBubble.mComment && !mCommentBubble.interacted){
                closeComment();
            }
        }
    };

    private boolean isHitting(Comment c, float xPos){
        return (c.xPos < xPos && c.xPos + mPlayerAvatarBar.getAvatarWidth() > xPos);
    }

    private Comment isHitting(float xPos, Comment skipComment){
       for (int i = mCurrentTopComments.size()-1; i >= 0; i-- )
          if (mCurrentTopComments.get(i).xPos > 0 && isHitting(mCurrentTopComments.get(i),xPos) && (skipComment == null || skipComment.xPos < mCurrentTopComments.get(i).xPos)){
                  return mCurrentTopComments.get(i);
          } else if (mCurrentTopComments.get(i).xPos > xPos)
              break;

      return null;
    }


    private Comment lastCommentBeforeTimestamp(long timestamp){
        for (Comment comment : mCurrentTopComments)
            if (comment.timestamp < timestamp)
                return comment;

        return null;
    }


    public boolean onLongClick(View v) {
        return true;
    }

    public void setComments(ArrayList<Comment> comments, boolean animatIn) {
        mCurrentComments = comments;

        if (!mShowingComments || mCurrentComments == null)
            return;

        mCurrentTopComments = new ArrayList<Comment>();

        Collections.sort(comments, new Comment.CompareTimestamp());

        for (int i = 0; i < mCurrentComments.size(); i++){
            if (mCurrentComments.get(i).timestamp > 0 && (i == mCurrentComments.size()-1 || mCurrentComments.get(i).timestamp != mCurrentComments.get(i+1).timestamp)){
                mCurrentComments.get(i).topLevelComment = true;
                mCurrentTopComments.add(mCurrentComments.get(i));
            }else if (mCurrentComments.get(i).timestamp > 0)
                mCurrentComments.get(i+1).nextComment = mCurrentComments.get(i);

            if (getWidth() > 0 &&  mCurrentComments.get(i).xPos == -1)
                mCurrentComments.get(i).calculateXPos(getWidth(), mDuration);
        }

        if (mPlayerAvatarBar != null){
            mPlayerAvatarBar.setTrackData(mDuration, comments);
            mPlayerAvatarBar.invalidate();
        }

        if (mCommentLines != null){
            mCommentLines.setTrackData(mDuration,comments);
            mCommentLines.invalidate();
        }

        if (mPlayerAvatarBar != null && mPlayerAvatarBar.getVisibility() == View.INVISIBLE){
            if (animatIn){
                AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
                aa.setStartOffset(500);
                aa.setDuration(500);

                mPlayerAvatarBar.startAnimation(aa);
                mCommentLines.startAnimation(aa);
            }

            mPlayerAvatarBar.setVisibility(View.VISIBLE);
            mCommentLines.setVisibility(View.VISIBLE);
        }
    }

    public void nextCommentInThread(){
        if (mCurrentShowingComment.nextComment != null){
            mCurrentShowingComment = mCurrentShowingComment.nextComment;
            showCurrentComment();
        }
    }

    public void clearTrack(){
        if (mPlayerAvatarBar != null){
            if (mShowingComments) mPlayerAvatarBar.setVisibility(View.INVISIBLE);
            mPlayerAvatarBar.clearTrackData();
        }
        if (mCommentLines != null){
            if (mShowingComments) mCommentLines.setVisibility(View.INVISIBLE);
            mCommentLines.clearTrackData();
        }


        mCurrentComments = null;
        mCurrentTopComments = null;

        mHandler.removeCallbacks(mAutoCloseBubble);
        if (mCurrentShowingComment != null){
            mCurrentShowingComment = null;
        }

        mode = NONE;
        removeBubble();

    }

    private void toggleComments(){
        mShowingComments = !mShowingComments;
        mPreferences.edit().putBoolean("playerShowingCOmments", mShowingComments).commit();
        if (mShowingComments){
            mToggleComments.setImageDrawable(mPlayer.getResources().getDrawable(R.drawable.ic_hide_comments_states));
            mPlayerAvatarBar.setVisibility(View.INVISIBLE);
            mCommentLines.setVisibility(View.INVISIBLE);
            setComments(mCurrentComments,true);
        } else {
            mToggleComments.setImageDrawable(mPlayer.getResources().getDrawable(R.drawable.ic_hide_comments_states));
            mPlayerAvatarBar.setVisibility(View.GONE);
            mCommentLines.setVisibility(View.GONE);
        }

    }


}
