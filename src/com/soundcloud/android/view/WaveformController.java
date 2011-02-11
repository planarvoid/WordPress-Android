
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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
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
    
    private PlayerCommentBar mPlayerCommentBar;
    
    private ArrayList<Comment> mCurrentComments;
    
    private Comment mCurrentShowingComment;

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
    
    private CommentBubble mCommentBubble;
    
    // These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();

    Matrix savedMatrix = new Matrix();

    float oldDist;

    PointF start = new PointF();

    PointF fake = new PointF();

    PointF mid = new PointF();
    
    private int mAvatarOffsetY;
    
    private int mCommentBarOffsetY;

    static final int SEEK_TOLERANCE = 10;

    static final double TOUCH_MOVE_TOLERANCE = 2.0;

    static final int NONE = 0;

    static final int SEEK_DRAG = 1;
    
    static final int COMMENT_DRAG = 2;
    
    static final int AVATAR_DRAG = 3;
    
    boolean mBubbleAnimating = false;
    
    long mLastMoveTime = 0;
    int mTouchX;
    int mTouchY;

    int mode = NONE;
    

    SharedPreferences mPrefernces;

    public WaveformController(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        this.setWillNotDraw(false);

        mPlayer = (ScPlayer) context;

        mPrefernces = PreferenceManager.getDefaultSharedPreferences(mPlayer);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.waveformcontroller, this);

        mConnectingBar = (RelativeLayout) findViewById(R.id.connecting_bar);
        mWaveformFrame = (RelativeLayout) findViewById(R.id.waveform_frame);
        mWaveformHolder = (WaveformHolder) findViewById(R.id.waveform_holder);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        
        mTrackTouchBar = (RelativeLayout) findViewById(R.id.track_touch_bar);
        mTrackTouchBar.setOnTouchListener(this);
        mOverlay = (ImageView) findViewById(R.id.progress_overlay);

        mPlayerAvatarBar =(PlayerAvatarBar) findViewById(R.id.player_avatar_bar);
        mPlayerCommentBar =(PlayerCommentBar) findViewById(R.id.player_comment_bar);
        
        if (mPlayerAvatarBar != null) 
            mPlayerAvatarBar.setOnTouchListener(this);
        
        if (mPlayerCommentBar != null) 
            mPlayerCommentBar.setOnTouchListener(this);

        LightingColorFilter lcf = new LightingColorFilter(1, mPlayer.getResources().getColor(
                R.color.white));
        mOverlay.setBackgroundColor(Color.TRANSPARENT);
        mOverlay.setColorFilter(lcf);
        mOverlay.setScaleType(ScaleType.FIT_XY);
        
        File dirFile = new File(CloudUtils.getCacheDirPath(mPlayer) + "/waves/");
        dirFile.mkdirs();
        
        

        // mOverlay.setImageDrawable(context.getResources().getDrawable(R.drawable.wave));
    }

    public void showConnectingLayout() {
        mConnectingBar.setVisibility(View.VISIBLE);
    }

    public void hideConnectingLayout() {
        mConnectingBar.setVisibility(View.GONE);
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l,t,r,b);
        
        if (changed && mLandscape){
            if (mCommentBubble == null)
                mCommentBubble = new CommentBubble(mPlayer, this);
            
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
    }

    public void setSecondaryProgress(int percent) {
        mProgressBar.setSecondaryProgress(percent);
    }

    public void updateTrack(Track track) {
        if (mPlayingTrack != null) {
            if (mPlayingTrack.id.compareTo(track.id) == 0
                    && waveformResult != BindResult.ERROR) {
                return;
            }
        }

        mPlayingTrack = track;
        mDuration = mPlayingTrack.duration;

        if (waveformResult != BindResult.ERROR) { // clear loader errors so we
            // can try to reload
            ImageLoader.get(mPlayer).clearErrors();
        }

        waveformResult = ImageLoader.get(mPlayer).bind(mOverlay, track.waveform_url,
                new ImageLoader.Callback() {
                    @Override
                    public void onImageError(ImageView view, String url, Throwable error) {
                        waveformResult = BindResult.ERROR;
                    }

                    @Override
                    public void onImageLoaded(ImageView view, String url) {
                    }
                });

        if (waveformResult != BindResult.OK) { // otherwise, it succesfull
            // pulled it out of memory, so no
            // temp image necessary
            mOverlay.setImageDrawable(mPlayer.getResources()
                    .getDrawable(R.drawable.player_wave_bg));
        }
    }

    public BindResult currentWaveformResult() {
        return waveformResult;
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (mCommentBubble != null && mCommentBubble.getParent() == this) removeView(mCommentBubble);

                if (v == mPlayerAvatarBar){
                    if (mCurrentComments != null){
                        mode = AVATAR_DRAG;
                        calcAvatarHit(event.getX());    
                    }
                } else if (v == mPlayerCommentBar){
                    mode = COMMENT_DRAG;
                    mCommentBubble.onNewCommentMode(mPlayingTrack, stampFromPosition((int) event.getX()));
                    
                   showBubbleAt((int) event.getX(),mCommentBarOffsetY);
                } else {
                    mode = SEEK_DRAG;
                    if (mPlayer != null && mPlayer.isSeekable()) {
                            setProgress(mPlayer
                                    .setSeekMarker((int) event.getX() / mWaveformHolder.getWidth()));
                        mWaveformHolder.invalidate();
                    }
                    
                }
                
                
                break;

            case MotionEvent.ACTION_MOVE:
                if (mTouchX == (int) event.getX() && mTouchY == (int) event.getY() && System.currentTimeMillis() - mLastMoveTime > 50)
                    return true;
                    
                mLastMoveTime = System.currentTimeMillis();
                mTouchX = (int) event.getX();
                mTouchY = (int) event.getY();
                
                switch (mode){
                    case SEEK_DRAG :
                        if (mPlayer != null && mPlayer.isSeekable()) {
                            if (mPlayer != null)
                                setProgress(mPlayer.setSeekMarker(event.getX()
                                        / mWaveformHolder.getWidth()));
                            mWaveformHolder.invalidate();
                        }
                        break;
                    case COMMENT_DRAG :
                        mCommentBubble.updateNewCommentTime(stampFromPosition(mTouchX));
                        moveBubbleTo((int) mTouchX, mCommentBarOffsetY);
                        break;
                    case AVATAR_DRAG :
                        calcAvatarHit(mTouchX);
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
                switch (mode){
                    case SEEK_DRAG :
                        if (mPlayer != null) mPlayer.sendSeek();
                        break;
                    case COMMENT_DRAG :
                        if (!mBubbleAnimating){
                            removeBubble();
                            if (Math.abs(mPlayerCommentBar.getTop() - (int) event.getY()) < 200)
                                mPlayer.addNewComment(mPlayingTrack,stampFromPosition(mTouchX));
                        }
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
        return (long) (Math.min(Math.max(0, (((float)x)/getWidth())),1) * mPlayingTrack.duration);
    }
    
    private void showBubbleAt(int xPos, int yPos){
        float offsetX = mCommentBubble.setPosition(xPos,yPos,getWidth());
        
        if (mCommentBubble.getParent() != mPlayer.getCommentHolder()){
            mPlayer.getCommentHolder().addView(mCommentBubble);
        
            ScaleAnimation scale = new ScaleAnimation((float).6, (float)1.0, (float).6, (float)1.0, Animation.RELATIVE_TO_SELF, offsetX,Animation.RELATIVE_TO_SELF, (float) 1.0);
            scale.setFillAfter(true);
            scale.setDuration(100);
            scale.setAnimationListener(new AnimationListener(){

                @Override
                public void onAnimationEnd(Animation arg0) {
                    mBubbleAnimating = false;
                    if (mode == NONE && mCurrentShowingComment == null)
                        removeBubble();
                }

                @Override
                public void onAnimationRepeat(Animation arg0) { }

                @Override
                public void onAnimationStart(Animation arg0) {
                    mBubbleAnimating = true;
                    
                }});
            mCommentBubble.startAnimation(scale); 
        }
        
        
    }
    private void moveBubbleTo(int xPos, int yPos){
        if (mCommentBubble == null) return;
        mCommentBubble.setPosition(xPos,yPos,getWidth());
    };
    
    public void closeComment(){
        mCurrentShowingComment = null;
        removeBubble();
    }
    
    private void removeBubble(){
        if (mCommentBubble != null && mCommentBubble.getParent() != null) ((ViewGroup) mCommentBubble.getParent()).removeView(mCommentBubble);
    }
    
    
    private void calcAvatarHit(float xPos){
        if (mCurrentShowingComment != null){
            if (isHitting(mCurrentShowingComment,xPos))
                return;
            else {
               closeComment();
            }
        }
        
        mCurrentShowingComment = isHitting(xPos);
        if (mCurrentShowingComment != null){
            Log.i(TAG, mCurrentShowingComment.xPos+ " " +mPlayerAvatarBar.getAvatarWidth()/2);
            mCommentBubble.onShowCommentMode(mCurrentShowingComment);
            showBubbleAt(mCurrentShowingComment.xPos+mPlayerAvatarBar.getAvatarWidth()/2,mAvatarOffsetY);
        }
        
    }
    
    private boolean isHitting(Comment c, float xPos){
        return (c.xPos < xPos && c.xPos + mPlayerAvatarBar.getAvatarWidth() > xPos);
    }
    
    private Comment isHitting(float xPos){
      for (Comment comment : mCurrentComments)
          if (comment.xPos != -1 && isHitting(comment,xPos))
              return comment;

      return null;
    }
    
    public boolean onLongClick(View v) {
        return true;
    }
    
    public void setComments(ArrayList<Comment> comments) {
        mCurrentComments = comments;
        
        Collections.sort(comments, new Comment.CompareTimestamp());
        
        for (int i = 0; i < mCurrentComments.size(); i++){
            if (mCurrentComments.get(i).timestamp != -1 && (i == mCurrentComments.size()-1 || mCurrentComments.get(i).timestamp != mCurrentComments.get(i+1).timestamp))
                mCurrentComments.get(i).topLevelComment = true;
            
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
    }
    
    public void clearTrack(){
        if (mCurrentShowingComment != null){
            //mCurrentShowingComment
            mCurrentShowingComment = null;
        }
        
        if (mPlayerAvatarBar != null)
            mPlayerAvatarBar.clearTrackData();
        
        if (mCommentLines != null)
            mCommentLines.clearTrackData();
    }
    
    public void makeCommentAt(long timestamp){
        
    }

}
