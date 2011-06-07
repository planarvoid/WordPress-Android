
package com.soundcloud.android.view;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScPlayer;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.InputObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
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
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class WaveformController extends RelativeLayout implements OnTouchListener {
    private static final String TAG = "WaveformController";

    private PlayerAvatarBar mPlayerAvatarBar;
    private RelativeLayout mPlayerCommentBar;

    private ImageView mOverlay;
    private ProgressBar mProgressBar;
    private RelativeLayout mTrackTouchBar;
    private WaveformHolder mWaveformHolder;
    private RelativeLayout mWaveformFrame;
    private WaveformCommentLines mCommentLines;
    private ImageButton mToggleComments;

    private ScPlayer mPlayer;
    private Track mPlayingTrack;
    private boolean mShowingComments;
    private List<Comment> mCurrentComments, mCurrentTopComments;
    private Comment mCurrentShowingComment;
    public ImageLoader.BindResult waveformResult;

    private Comment mAddComment, mLastAutoComment;
    private CommentBubble mCommentBubble;
    private Animation mBubbleAnimation;

    private ArrayBlockingQueue<InputObject> mInputObjectPool;
    private TouchThread mTouchThread;

    private int mWaveformErrorCount, mAvatarOffsetY, mCommentBarOffsetY, mDuration;
    private boolean mShowBubble, mShowBubbleForceAnimation;

    private float mSeekPercent;

    private SharedPreferences mPreferences;
    private final Handler mHandler = new Handler();

    private static final int MAX_WAVEFORM_RETRIES = 2;
    private static final int INPUT_QUEUE_SIZE = 20;

    private static final int UI_UPDATE_SEEK = 1;
    private static final int UI_SEND_SEEK = 2;
    private static final int UI_UPDATE_BUBBLE = 3;
    private static final int UI_UPDATE_BUBBLE_NEW_COMMENT = 4;
    private static final int UI_SHOW_CURRENT_COMMENT = 5;
    private static final int UI_ADD_COMMENT = 6;
    private static final int UI_TOGGLE_COMMENTS = 7;
    static final int TOUCH_MODE_NONE = 0;
    static final int TOUCH_MODE_SEEK_DRAG = 1;
    static final int TOUCH_MODE_COMMENT_DRAG = 2;
    static final int TOUCH_MODE_AVATAR_DRAG = 3;
    int mode = TOUCH_MODE_NONE;

    public WaveformController(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setWillNotDraw(false);

        mPlayer = (ScPlayer) context;

        mPreferences = PreferenceManager.getDefaultSharedPreferences(mPlayer);

        mShowingComments = mPreferences.getBoolean("playerShowingCOmments", true);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.waveformcontroller, this);

        mInputObjectPool = new ArrayBlockingQueue<InputObject>(INPUT_QUEUE_SIZE);
        for (int i = 0; i < INPUT_QUEUE_SIZE; i++) {
            mInputObjectPool.add(new InputObject(mInputObjectPool));
        }

        mTouchThread = new TouchThread();
        mTouchThread.start();

        mWaveformFrame = (RelativeLayout) findViewById(R.id.waveform_frame);
        mWaveformHolder = (WaveformHolder) findViewById(R.id.waveform_holder);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mOverlay = (ImageView) findViewById(R.id.progress_overlay);

        mTrackTouchBar = (RelativeLayout) findViewById(R.id.track_touch_bar);
        mTrackTouchBar.setOnTouchListener(this);

        if (isLandscape()){

            mPlayerAvatarBar =(PlayerAvatarBar) findViewById(R.id.player_avatar_bar);

            mPlayerAvatarBar.setOnTouchListener(this);
            mPlayerAvatarBar.setVisibility(mShowingComments ? View.INVISIBLE : View.GONE);

            mPlayerCommentBar =(RelativeLayout) findViewById(R.id.new_comment_bar);
            mPlayerCommentBar.setOnTouchListener(this);
            if (!mShowingComments){
                ((TextView)mPlayerCommentBar.findViewById(R.id.txt_instructions))
                        .setText(getResources().getString(R.string.player_touch_bar_disabled));
            }

            mToggleComments = (ImageButton) findViewById(R.id.btn_toggle);
            mToggleComments.setImageDrawable((mShowingComments) ? mPlayer.getResources()
                    .getDrawable(R.drawable.ic_hide_comments_states) : mPlayer.getResources()
                    .getDrawable(R.drawable.ic_show_comments_states));
            mToggleComments.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleComments();
                }
            });

            mCommentLines = new WaveformCommentLines(mPlayer, null);
            mCommentLines.setVisibility(mShowingComments ? View.INVISIBLE : View.GONE);
            mWaveformHolder.addView(mCommentLines);

        } else {
            // not landscape
            // this will allow transparency for the progress bar
            this.setStaticTransformationsEnabled(true);
        }

        mOverlay.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);

        LightingColorFilter lcf = new LightingColorFilter(1, mPlayer.getResources().getColor(
                R.color.white));
        mOverlay.setBackgroundColor(Color.TRANSPARENT);
        mOverlay.setColorFilter(lcf);
        mOverlay.setScaleType(ScaleType.FIT_XY);

        File dirFile = new File(CloudUtils.getCacheDirPath(mPlayer) + "/waves/");

        mkdirs(dirFile);
    }

    private boolean isLandscape(){
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public void onStop() {
        if (mPlayerAvatarBar != null) mPlayerAvatarBar.onStop(); //stops avatar loading
    }

    public void showConnectingLayout() {
       mWaveformHolder.showConnectingLayout();
    }

    public void hideConnectingLayout() {
        mWaveformHolder.hideConnectingLayout();


    }

    public void setProgress(long pos) {
        if (mDuration == 0)
            return;

        mProgressBar.setProgress((int) (1000 * pos / mDuration));

        if (isLandscape() && mode == TOUCH_MODE_NONE && mCurrentTopComments != null){
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

        if (mPlayingTrack != track){
            ImageLoader.get(mPlayer).unbind(mOverlay);
        }

        mPlayingTrack = track;
        mDuration = mPlayingTrack.duration;

        if (waveformResult == BindResult.ERROR) {
            // clear loader errors so we can try to reload
            ImageLoader.get(mPlayer).clearErrors();
        } else {
            mWaveformErrorCount = 0;
        }
        waveformResult = ImageLoader.get(mPlayer).bind(mOverlay, track.waveform_url,
                new ImageLoader.ImageViewCallback() {
                    @Override
                    public void onImageError(ImageView view, String url, Throwable error) {
                        waveformResult = BindResult.ERROR;
                        onWaveformError();
                    }

                    @Override
                    public void onImageLoaded(ImageView view, String url) {
                        showWaveform();
                    }
                });



        switch (waveformResult) {
            case OK:      showWaveform(); break;
            case LOADING:
            case ERROR:
                mOverlay.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
                break;
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        try {
            // history first
            int hist = event.getHistorySize();
            if (hist > 0) {
                // add from oldest to newest
                for (int i = 0; i < hist; i++) {
                    InputObject input = mInputObjectPool.take();
                    input.useEventHistory(v, event, i);
                    mTouchThread.feedInput(input);
                }
            }
            // current last
            InputObject input = mInputObjectPool.take();
            input.useEvent(v, event);
            mTouchThread.feedInput(input);
            synchronized (mTouchThread.eventMutex) {
                mTouchThread.eventMutex.notify();
            }
        } catch (InterruptedException e) {
        }
        return true; // indicate event was handled
    }

    public void closeComment(){
        mCurrentShowingComment = null;
        if (mPlayerAvatarBar != null) mPlayerAvatarBar.setCurrentComment(null);
        if (mCommentLines != null) mCommentLines.setCurrentComment(null);
        removeBubble();
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

        mode = TOUCH_MODE_NONE;
        removeBubble();

    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l,t,r,b);

        if (changed && isLandscape()){

            int[] calc = new int[2];

            mPlayer.getCommentHolder().getLocationInWindow(calc);
            int topOffset = calc[1];

            if (mCommentBubble == null) mCommentBubble = new CommentBubble(mPlayer, this);
            mCommentBubble.parentWidth = getWidth();

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


    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        boolean ret = super.getChildStaticTransformation(child, t);
        if (child == mWaveformFrame) {
            t.setAlpha((float) 0.7);
            return true;
        }
        return ret;
    }



    private void onWaveformError(){
        mWaveformErrorCount++;
        if (mWaveformErrorCount < MAX_WAVEFORM_RETRIES){
            updateTrack(mPlayingTrack);
        } else {
            mOverlay.setImageDrawable(mPlayer.getResources()
                    .getDrawable(R.drawable.player_wave_bg));
            showWaveform();
        }
    }


    private void showWaveform(){
        mPlayer.onWaveformLoaded();
        if (mOverlay.getVisibility() == View.INVISIBLE){
            AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
            aa.setDuration(500);

            mOverlay.startAnimation(aa);
            mOverlay.setVisibility(View.VISIBLE);

            mProgressBar.startAnimation(aa);
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }


    private long stampFromPosition(int x){
        return (long) (Math.min(Math.max(.001, (((float)x)/getWidth())),1) * mPlayingTrack.duration);
    }

    private void showBubble(float offsetX){

        if (mCommentBubble == null) {
            return;
        }

        mCommentBubble.closing = false;

        if (mCommentBubble.getParent() != mPlayer.getCommentHolder() || mShowBubbleForceAnimation){
            if (mCommentBubble.getParent() != mPlayer.getCommentHolder()){
                    mPlayer.getCommentHolder().addView(mCommentBubble);
                    offsetX = mCommentBubble.update();
            }

            if (mBubbleAnimation != null && Build.VERSION.SDK_INT > 7)
                mBubbleAnimation.cancel();

            mBubbleAnimation = new ScaleAnimation((float).6, (float)1.0, (float).6, (float)1.0, Animation.RELATIVE_TO_SELF, offsetX,Animation.RELATIVE_TO_SELF, (float) 1.0);
            mBubbleAnimation.setFillAfter(true);
            mBubbleAnimation.setDuration(100);
            mBubbleAnimation.setAnimationListener(new AnimationListener(){

                @Override
                public void onAnimationEnd(Animation arg0) {
                    mBubbleAnimation = null;
                    if (mode == TOUCH_MODE_NONE && mCurrentShowingComment == null)
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

    private void removeBubble(){
        if (mCommentBubble != null && mCommentBubble.getParent() != null){
            ((ViewGroup) mCommentBubble.getParent()).removeView(mCommentBubble);
        }
    }


    private void showCurrentComment(){
        showCurrentComment(false);
    }

    private void showCurrentComment(boolean waitForInteraction){
        if (mCommentBubble == null) return;

        if (mCurrentShowingComment != null){

            mPlayerAvatarBar.setCurrentComment(mCurrentShowingComment);
            mCommentLines.setCurrentComment(mCurrentShowingComment);

            mCommentBubble.comment_mode = CommentBubble.MODE_SHOW_COMMENT;
            mCommentBubble.show_comment = mCurrentShowingComment;
            mCommentBubble.interacted = !waitForInteraction;
            mCommentBubble.xPos = mCurrentShowingComment.xPos;
            mCommentBubble.yPos = mAvatarOffsetY;

            mShowBubbleForceAnimation = false;
            showBubble(mCommentBubble.update());

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
       for (int i = mCurrentTopComments.size()-1; i >= 0; i-- ){
          if (mCurrentTopComments.get(i).xPos > 0 && isHitting(mCurrentTopComments.get(i),xPos) && (skipComment == null || skipComment.xPos < mCurrentTopComments.get(i).xPos)){
                  return mCurrentTopComments.get(i);
          } else if (mCurrentTopComments.get(i).xPos > xPos)
              break;
       }

      return null;
    }


    private Comment lastCommentBeforeTimestamp(long timestamp){
        for (Comment comment : mCurrentTopComments)
            if (comment.timestamp < timestamp)
                return comment;

        return null;
    }


    public void setComments(List<Comment> comments, boolean animateIn) {
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

            if (getWidth() > 0 &&  mDuration > 0 && mCurrentComments.get(i).xPos == -1)
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
            if (animateIn){
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

    private void toggleComments(){
        mShowingComments = !mShowingComments;
        mPreferences.edit().putBoolean("playerShowingCOmments", mShowingComments).commit();
        if (mShowingComments){
            mToggleComments.setImageDrawable(mPlayer.getResources().getDrawable(R.drawable.ic_hide_comments_states));
            mPlayerAvatarBar.setVisibility(View.INVISIBLE);
            mCommentLines.setVisibility(View.INVISIBLE);
            setComments(mCurrentComments,true);
            ((TextView) mPlayerCommentBar.findViewById(R.id.txt_instructions)).setText(getResources().getString(R.string.player_touch_bar));
        } else {
            mToggleComments.setImageDrawable(mPlayer.getResources().getDrawable(R.drawable.ic_show_comments_states));
            mPlayerAvatarBar.setVisibility(View.GONE);
            mCommentLines.setVisibility(View.GONE);
            ((TextView) mPlayerCommentBar.findViewById(R.id.txt_instructions)).setText(getResources().getString(R.string.player_touch_bar_disabled));
        }

    }

    private void processInputObject(InputObject input){
        switch (input.action) {
            case InputObject.ACTION_TOUCH_DOWN:
                if (input.view == mPlayerAvatarBar) {
                    if (mCommentBubble == null) return;
                    if (mCurrentComments != null) {
                        mode = TOUCH_MODE_AVATAR_DRAG;
                        mCommentBubble.comment_mode = CommentBubble.MODE_SHOW_COMMENT;
                        calcAvatarHit(input.x, true);
                    }
                } else if (input.view == mPlayerCommentBar){
                    if (!mShowingComments){
                        queueUnique(UI_TOGGLE_COMMENTS);
                        return;
                    }

                    if (mCommentBubble == null) return;

                    mode = TOUCH_MODE_COMMENT_DRAG;
                    mCurrentShowingComment = null;
                    mCommentBubble.new_comment_track = mPlayingTrack;
                    mCommentBubble.new_comment_timestamp = stampFromPosition(input.x);
                    mCommentBubble.comment_mode = CommentBubble.MODE_NEW_COMMENT;
                    mCommentBubble.xPos = input.x;
                    mCommentBubble.yPos = mCommentBarOffsetY;

                    mShowBubble = true;
                    mShowBubbleForceAnimation = true;

                    queueUnique(UI_UPDATE_BUBBLE);

                } else {
                    mode = TOUCH_MODE_SEEK_DRAG;
                    if (mPlayer != null && mPlayer.isSeekable()) {
                        mLastAutoComment = null; //reset auto comment in case they seek backward
                        mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
                        queueUnique(UI_UPDATE_SEEK);
                    }

                }
                break;

            case InputObject.ACTION_TOUCH_MOVE:
                switch (mode){
                    case TOUCH_MODE_SEEK_DRAG :
                        if (mPlayer != null && mPlayer.isSeekable()) {
                            mLastAutoComment = null; //reset auto comment in case they seek backward
                            mSeekPercent = ((float) input.x) / mWaveformHolder.getWidth();
                            queueUnique(UI_UPDATE_SEEK);
                        }
                        break;
                    case TOUCH_MODE_COMMENT_DRAG :
                        mCommentBubble.new_comment_timestamp = stampFromPosition(input.x);
                        mCommentBubble.xPos = input.x;
                        queueUnique(UI_UPDATE_BUBBLE_NEW_COMMENT);
                        break;

                    case TOUCH_MODE_AVATAR_DRAG :
                        calcAvatarHit(input.x, false);
                        break;
                }
                break;
            case InputObject.ACTION_TOUCH_UP:
                switch (mode){
                    case TOUCH_MODE_SEEK_DRAG :
                        queueUnique(UI_SEND_SEEK);
                        break;
                    case TOUCH_MODE_COMMENT_DRAG :
                        if (mBubbleAnimation == null){
                            if (Math.abs(mPlayerCommentBar.getTop() - input.y) < 200){
                                mAddComment = CloudUtils.buildComment(mPlayer, mPlayer.getUserId(),
                                        mPlayingTrack.id, stampFromPosition(input.x), "", 0);
                                queueUnique(UI_ADD_COMMENT);
                            }
                        }
                        mShowBubble = false;
                        queueUnique(UI_UPDATE_BUBBLE);
                        break;
                }
                    mode = TOUCH_MODE_NONE;
                break;
        }
    }

    private void queueUnique(int what){
        if (!mTouchHandler.hasMessages(what)) mTouchHandler.sendEmptyMessage(what);
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
            mCommentBubble.show_comment = mCurrentShowingComment;
            mShowBubble = true;
            queueUnique(UI_SHOW_CURRENT_COMMENT);
            return;
        }

        if (skipComment == null){
            mCommentBubble.show_comment = null;
            mShowBubble = false;
            queueUnique(UI_UPDATE_BUBBLE);
        }

    }



    Handler mTouchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_UPDATE_SEEK:
                    setProgress(mPlayer.setSeekMarker(mSeekPercent));
                    mWaveformHolder.invalidate();
                    break;

                case UI_SEND_SEEK:
                    if (mPlayer != null)
                        mPlayer.sendSeek(mSeekPercent);
                    break;

                case UI_UPDATE_BUBBLE:
                    if (mShowBubble)
                        showBubble(mCommentBubble.update());
                    else
                        removeBubble();
                    break;

                case UI_UPDATE_BUBBLE_NEW_COMMENT:
                    mCommentBubble.updatePosition();
                    mCommentBubble.updateNewCommentTime();
                    break;
                case UI_SHOW_CURRENT_COMMENT:
                    showCurrentComment();
                    break;

                case UI_ADD_COMMENT:
                    mPlayer.addNewComment(mAddComment, mPlayer.addCommentListener);
                    break;

                case UI_TOGGLE_COMMENTS:
                    toggleComments();
                    break;

            }
        }
    };

    private class TouchThread extends Thread {
        private ArrayBlockingQueue<InputObject> inputQueue = new ArrayBlockingQueue<InputObject>(
                INPUT_QUEUE_SIZE);

        private Object inputQueueMutex = new Object();

        public Object eventMutex = new Object();

        public boolean stopped = false;

        public void feedInput(InputObject input) {
            synchronized (inputQueueMutex) {
                try {
                    inputQueue.put(input);
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }

        private void processInput() {
            synchronized (inputQueueMutex) {
                ArrayBlockingQueue<InputObject> inputQueue = this.inputQueue;
                while (!inputQueue.isEmpty()) {
                    try {
                        InputObject input = inputQueue.take();
                        if (input.eventType == InputObject.EVENT_TYPE_TOUCH) {
                            processInputObject(input);
                        }
                        input.returnToPool();
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            }
        }

        @Override
        public void run() {
            while (!stopped) {
                synchronized (eventMutex) {
                    try {
                        eventMutex.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    processInput();
                }
            }
        }
    }

}