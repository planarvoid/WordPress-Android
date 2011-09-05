package com.soundcloud.android.view;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Transformation;
import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BitmapCallback;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.ImageUtils;

import java.util.List;

public class PlayerAvatarBar extends View {
    private static final String TAG = "PlayerCommentBar";

    private static final int REFRESH_AVATARS = 0;
    private static final int AVATARS_REFRESHED = 1;
    private static final int AVATAR_WIDTH = 32;
    private static final int AVATAR_WIDTH_LARGE = 100;

    private long mDuration;

    private List<Comment> mCurrentComments;
    private Comment mCurrentComment;

    private Matrix mMatrix;
    private Float mDefaultAvatarScale = (float) 1;
    private int mAvatarWidth;

    private Paint mImagePaint;
    private Paint mLinePaint;
    private Paint mActiveLinePaint;

    private Consts.GraphicSize mTargetSize;

    private Thread mAvatarRefreshThread;

    private Bitmap mCanvasBmp;
    private Bitmap mNextCanvasBmp;

    private Bitmap mDefaultAvatar;

    private ImageLoader mBitmapLoader;

    private Context mContext;

    public PlayerAvatarBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mContext = context;
        mBitmapLoader = ImageLoader.get(context.getApplicationContext());

        mImagePaint = new Paint();
        mImagePaint.setAntiAlias(false);
        mImagePaint.setFilterBitmap(true);

        mLinePaint = new Paint();
        mLinePaint.setColor(getResources().getColor(R.color.commentLine));

        mActiveLinePaint = new Paint();
        mActiveLinePaint.setColor(getResources().getColor(com.soundcloud.android.R.color.activeCommentLine));

        float mDensity = getContext().getResources().getDisplayMetrics().density;

        mMatrix = new Matrix();
        if (CloudUtils.isScreenXL(mContext)) {
            mTargetSize = Consts.GraphicSize.LARGE;
            mAvatarWidth = (int) (AVATAR_WIDTH_LARGE * mDensity);
        } else {
            mTargetSize = Consts.GraphicSize.BADGE;
            mAvatarWidth = (int) (AVATAR_WIDTH * mDensity);
        }
    }

    public int getAvatarWidth(){
        return mAvatarWidth;
    }

    public void onStop(){
        if (mCurrentComments != null) {
            for (Comment c : mCurrentComments) {
                if (!TextUtils.isEmpty(c.user.avatar_url)){
                    mBitmapLoader.cancelLoading(ImageUtils.formatGraphicsUri(c.user.avatar_url, mTargetSize));
                }
            }
        }
    }

    public void clearTrackData(){
        mUIHandler.removeMessages(REFRESH_AVATARS);
        mUIHandler.removeMessages(AVATARS_REFRESHED);

        if (mCurrentComments != null) {
            for (Comment c : mCurrentComments) {
                mBitmapLoader.cancelLoading(ImageUtils.formatGraphicsUri(c.user.avatar_url, mTargetSize));
                if (c.avatar != null) c.avatar.recycle();
                c.avatar = null;
            }
        }

        mCurrentComment = null;
        mCurrentComments = null;
        mDuration = -1;

        if (mCanvasBmp != null) mCanvasBmp.recycle();
        if (mNextCanvasBmp != null) mNextCanvasBmp.recycle();

        invalidate();

    }

    public void setTrackData(long duration, List<Comment> newItems){
        ImageLoader.get(mContext).clearErrors();
        mDuration = duration;
        mCurrentComments = newItems;
        for (Comment c : newItems){
            loadAvatar(c);
        }
        invalidate();
    }

    public void setCurrentComment(Comment c){
        mCurrentComment = c;
        if (c != null) loadAvatar(c);
        invalidate();
    }

    private void loadAvatar(final Comment c){
        if (!CloudUtils.checkIconShouldLoad(c.user.avatar_url))
            return;


        ImageUtils.getBitmapSubstitute(mContext, c.user.avatar_url, c.getAvatarBarGraphicSize(mContext), new BitmapCallback() {
            @Override
            public void onImageLoaded(Bitmap mBitmap, String uri) {
                c.avatar = mBitmap;
                if (c.topLevelComment) {
                    if (!mUIHandler.hasMessages(REFRESH_AVATARS)) {
                        Message msg = mUIHandler.obtainMessage(REFRESH_AVATARS);
                        PlayerAvatarBar.this.mUIHandler.sendMessageDelayed(msg, 100);
                    }
                }
            }

            @Override
            public void onImageError(String uri, Throwable error) {
                Log.i(TAG, "Avatar Loading Error " + uri + " " + error.toString());
            }
        }, new ImageLoader.Options());
    }

    private void refreshDefaultAvatar() {
        if (mAvatarWidth > 0 && (mDefaultAvatar == null || mDefaultAvatar.isRecycled())) {
            mDefaultAvatar = BitmapFactory.decodeResource(mContext.getResources(),
                    CloudUtils.isScreenXL(mContext) ? R.drawable.avatar_badge_large : R.drawable.avatar_badge);
            mDefaultAvatarScale = ((float) mAvatarWidth) / mDefaultAvatar.getHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l,t,r,b);

        if (changed){

            float mDensity = getContext().getResources().getDisplayMetrics().density;
            if (mDensity > 1) {
                mAvatarWidth = CloudUtils.isScreenXL(mContext) ? (int) (AVATAR_WIDTH_LARGE* mDensity) : getHeight() ;
            } else {
                mAvatarWidth = CloudUtils.isScreenXL(mContext)? AVATAR_WIDTH_LARGE : getHeight();
            }
            refreshDefaultAvatar();

            mUIHandler.removeMessages(REFRESH_AVATARS);
            Message msg = mUIHandler.obtainMessage(REFRESH_AVATARS);
            PlayerAvatarBar.this.mUIHandler.sendMessage(msg);
        }

    }

    class AvatarRefresher implements Runnable {
        @Override
        public void run() {
            // XXX race condition with current comments
            final List<Comment> comments = mCurrentComments;
            if (comments == null || getWidth() <= 0) return;

            try {
                mNextCanvasBmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e){
                // XXX really catch oom here?
                Log.e(TAG,"Out of memory during avatar refresher bitmap creation");
                return; // just don't show the updated bar, acceptable failure
            }

            Canvas canvas = new Canvas(mNextCanvasBmp);

            for (Comment comment : comments){
                if (Thread.currentThread().isInterrupted()) break;
                if (comment.timestamp == 0) continue;
                drawCommentOnCanvas(comment, canvas, mLinePaint);
            }

            if (Thread.currentThread().isInterrupted()) {
                mNextCanvasBmp.recycle();
            } else {
                mUIHandler.removeMessages(AVATARS_REFRESHED);
                Message msg = mUIHandler.obtainMessage(AVATARS_REFRESHED);
                PlayerAvatarBar.this.mUIHandler.sendMessage(msg);
            }
        }
    }

    Handler mUIHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_AVATARS:
                    if (mCurrentComments == null || mDuration == -1)
                        return;

                    if (mAvatarRefreshThread != null)
                        mAvatarRefreshThread.interrupt();

                    mAvatarRefreshThread = new Thread(new AvatarRefresher());
                    mAvatarRefreshThread.start();
                    break;

                case AVATARS_REFRESHED:
                    if (mCanvasBmp != null)
                        mCanvasBmp.recycle();

                    mCanvasBmp = mNextCanvasBmp;
                    invalidate();
                    break;
            }
        }
    };

    private void drawCommentOnCanvas(Comment comment, Canvas canvas, Paint linePaint){
        if (!CloudUtils.checkIconShouldLoad(comment.user.avatar_url) || comment.avatar == null || comment.avatar.isRecycled()){
            refreshDefaultAvatar();
            mMatrix.setScale(mDefaultAvatarScale, mDefaultAvatarScale);
            mMatrix.postTranslate(comment.xPos, 0);
            canvas.drawBitmap(mDefaultAvatar, mMatrix, mImagePaint);
            canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), linePaint);

            if (comment.avatar != null && comment.avatar.isRecycled()) loadAvatar(comment);

        } else if (comment.avatar != null) {
            final float drawScale = ((float) mAvatarWidth)/comment.avatar.getHeight();
            mMatrix.setScale(drawScale, drawScale);
            mMatrix.postTranslate(comment.xPos, 0);
            canvas.drawBitmap(comment.avatar, mMatrix, mImagePaint);
            canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), linePaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mCanvasBmp != null) {
            canvas.drawBitmap(mCanvasBmp, new Matrix(), mImagePaint);
        } else if (mCurrentComments != null) {
            for (Comment comment : mCurrentComments){
                canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), mLinePaint);
            }
        }

        if (mCurrentComment != null){
            drawCommentOnCanvas(mCurrentComment,canvas,mActiveLinePaint);
            canvas.drawLine(mCurrentComment.xPos, 0, mCurrentComment.xPos, getHeight(), mActiveLinePaint);
        }
    }

    public float getCurrentTransformY(){
        if (getAnimation() == null) return 0f;
        Transformation t = new Transformation();
        float[] values = new float[9];
        getAnimation().getTransformation(getDrawingTime(), t);
        t.getMatrix().getValues(values);
        return values[5];
    }

    public void getHitRect(Rect outRect) {
        if (getAnimation() != null){
            final int offsetY = (int) CloudUtils.getCurrentTransformY(this);
            outRect.set(getLeft(), getTop() + offsetY, getRight(), getBottom() + offsetY);
        } else {
            super.getHitRect(outRect);
        }
    }
}
