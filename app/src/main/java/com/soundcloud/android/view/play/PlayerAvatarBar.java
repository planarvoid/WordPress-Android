package com.soundcloud.android.view.play;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.utils.images.ImageSize;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.List;

public class PlayerAvatarBar extends View {
    private static final String TAG = "PlayerCommentBar";

    private static final int REFRESH_AVATARS = 0;
    private static final int AVATARS_REFRESHED = 1;
    private static final int AVATAR_WIDTH = 32;
    private static final int AVATAR_WIDTH_LARGE = 100;
    public static final Matrix DEFAULT_MATRIX = new Matrix();

    private long mDuration;

    private @Nullable List<Comment> mCurrentComments;
    private @Nullable Comment mCurrentComment;

    private Matrix mBgMatrix;
    private Matrix mActiveMatrix;
    private float mDefaultAvatarScale = 1f;
    private int mAvatarWidth;

    private Paint mImagePaint;
    private Paint mLinePaint;

    private Paint mActiveImagePaint;
    private Paint mActiveLinePaint;

    private ImageSize mTargetSize;

    private Thread mAvatarRefreshThread;

    private @Nullable Bitmap mCanvasBmp;
    private Bitmap mNextCanvasBmp;

    private Bitmap mDefaultAvatar;

    private ImageLoader mBitmapLoader;
    private ImageSize mAvatarGraphicsSize;

    private boolean mLandscape;

    public PlayerAvatarBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mBitmapLoader = ImageLoader.getInstance();

        if (ImageUtils.isScreenXL(context)) {
            mAvatarGraphicsSize= ImageSize.LARGE;
        } else {
            mAvatarGraphicsSize = context.getResources().getDisplayMetrics().density > 1 ?
                    ImageSize.BADGE :
                    ImageSize.SMALL;
        }

        mImagePaint = new Paint();
        mImagePaint.setAntiAlias(false);
        mImagePaint.setFilterBitmap(true);

        mActiveImagePaint = new Paint();
        mActiveImagePaint.setAntiAlias(false);
        mActiveImagePaint.setFilterBitmap(true);

        mLinePaint = new Paint();
        mLinePaint.setColor(getResources().getColor(R.color.commentLine));

        mActiveLinePaint = new Paint();
        mActiveLinePaint.setColor(getResources().getColor(com.soundcloud.android.R.color.activeCommentLine));

        float mDensity = getContext().getResources().getDisplayMetrics().density;

        mBgMatrix = new Matrix();
        mActiveMatrix = new Matrix();

        if (ImageUtils.isScreenXL(context)) {
            mTargetSize = ImageSize.LARGE;
            mAvatarWidth = (int) (AVATAR_WIDTH_LARGE * mDensity);
        } else {
            mTargetSize = ImageSize.BADGE;
            mAvatarWidth = (int) (AVATAR_WIDTH * mDensity);
        }
    }

    public int getAvatarWidth(){
        return mAvatarWidth;
    }

    public void onStop(){
        // TODO, reinstate avatar load killing with new imageloader
        if (mCurrentComments != null) {
//            for (Comment c : mCurrentComments) {
//                if (!TextUtils.isEmpty(c.user.avatar_url)){
//                    mBitmapLoader.cancelRequest(mTargetSize.formatUri(c.user.avatar_url));
//                }
//            }
        }
    }

    public void clearTrackData(){
        mUIHandler.removeMessages(REFRESH_AVATARS);
        mUIHandler.removeMessages(AVATARS_REFRESHED);

        if (mAvatarRefreshThread != null){
            mAvatarRefreshThread.interrupt();
            mAvatarRefreshThread = null;
        }

        if (mCurrentComments != null) {
            for (Comment c : mCurrentComments) {
                //mBitmapLoader.cancelRequest(mTargetSize.formatUri(c.user.avatar_url));
                c.avatar = null;
            }
        }

        mCurrentComment = null;
        mCurrentComments = null;
        mDuration = -1;

        if (mCanvasBmp != null) mCanvasBmp.recycle();
        if (mNextCanvasBmp != null) mNextCanvasBmp.recycle();

        mCanvasBmp = null;

        invalidate();

    }

    public void setTrackData(long duration, List<Comment> newItems){
        mDuration = duration;
        mCurrentComments = newItems;
        for (Comment c : newItems){
            loadAvatar(c);
        }
        invalidate();
    }

    public void setCurrentComment(@Nullable Comment c){
        mCurrentComment = c;
        if (c != null) loadAvatar(c);
        invalidate();
    }

    private void loadAvatar(final Comment c){
        if (c == null || !c.shouldLoadIcon()) return;

        mBitmapLoader.loadImage(mAvatarGraphicsSize.formatUri(c.user.avatar_url), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                c.avatar = loadedImage;
                if (c.topLevelComment) {
                    if (!mUIHandler.hasMessages(REFRESH_AVATARS)) {
                        Message msg = mUIHandler.obtainMessage(REFRESH_AVATARS);
                        PlayerAvatarBar.this.mUIHandler.sendMessageDelayed(msg, 100);
                    }
                }
            }
        });
    }

    private void refreshDefaultAvatar() {
        if (mAvatarWidth > 0 && (mDefaultAvatar == null || mDefaultAvatar.isRecycled())) {
            mDefaultAvatar = BitmapFactory.decodeResource(getContext().getResources(),
                    ImageUtils.isScreenXL(getContext()) ? R.drawable.avatar_badge_large : R.drawable.avatar_badge);
            mDefaultAvatarScale = ((float) mAvatarWidth) / mDefaultAvatar.getHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l,t,r,b);

        if (changed){

            float mDensity = getContext().getResources().getDisplayMetrics().density;
            if (mDensity > 1) {
                mAvatarWidth = ImageUtils.isScreenXL(getContext()) ? (int) (AVATAR_WIDTH_LARGE* mDensity) : getHeight() ;
            } else {
                mAvatarWidth = ImageUtils.isScreenXL(getContext())? AVATAR_WIDTH_LARGE : getHeight();
            }
            refreshDefaultAvatar();

            mUIHandler.removeMessages(REFRESH_AVATARS);
            Message msg = mUIHandler.obtainMessage(REFRESH_AVATARS);
            PlayerAvatarBar.this.mUIHandler.sendMessage(msg);
        }

    }

    public void setIsLandscape(boolean landscape) {
        mLandscape = landscape;
    }

    class AvatarRefresher implements Runnable {
        @Override
        public void run() {
            // XXX race condition with current comments
            final List<Comment> comments = mCurrentComments;
            final int width = getWidth();
            final int height = getHeight();
            if (comments != null && width > 0 && height > 0) {
                try {
                    mNextCanvasBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError e) {
                    // XXX really catch oom here?
                    Log.e(TAG, "Out of memory during avatar refresher bitmap creation");
                }

                if (mNextCanvasBmp != null && !mNextCanvasBmp.isRecycled()) {
                    Canvas canvas = new Canvas(mNextCanvasBmp);
                    for (Comment comment : comments) {
                        if (Thread.currentThread().isInterrupted()) break;
                        if (comment.timestamp == 0) continue;
                        drawCommentOnCanvas(comment, canvas, mLinePaint, mImagePaint, mBgMatrix);
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        mNextCanvasBmp.recycle();
                        mNextCanvasBmp = null;
                    } else {
                        if (!mUIHandler.hasMessages(AVATARS_REFRESHED)) {
                            Message msg = mUIHandler.obtainMessage(AVATARS_REFRESHED);
                            PlayerAvatarBar.this.mUIHandler.sendMessageDelayed(msg, 200);
                        }

                    }
                }
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

    private void drawCommentOnCanvas(Comment comment, Canvas canvas, Paint linePaint, Paint imagePaint, Matrix matrix){
        if (canvas == null) return;
        final Bitmap avatar = comment.avatar;

        if (avatar == null || avatar.isRecycled() || !comment.shouldLoadIcon()) {
            if (mLandscape) {
                refreshDefaultAvatar();
                matrix.setScale(mDefaultAvatarScale, mDefaultAvatarScale);
                matrix.postTranslate(comment.xPos, 0);
                canvas.drawBitmap(mDefaultAvatar, matrix, imagePaint);
            }
            canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), linePaint);

            if (avatar != null && avatar.isRecycled()) loadAvatar(comment);

        } else {
            final float drawScale = ((float) mAvatarWidth) / avatar.getHeight();
            matrix.setScale(drawScale, drawScale);
            matrix.postTranslate(comment.xPos, 0);
            canvas.drawBitmap(avatar, matrix, imagePaint);
            canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), linePaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mCanvasBmp != null && !mCanvasBmp.isRecycled()) {
            canvas.drawBitmap(mCanvasBmp, DEFAULT_MATRIX, mImagePaint);
        } else if (mCurrentComments != null) {
            for (Comment comment : mCurrentComments){
                canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), mLinePaint);
            }
        }

        if (mCurrentComment != null){
            drawCommentOnCanvas(mCurrentComment,canvas,mActiveLinePaint, mActiveImagePaint,mActiveMatrix);
            canvas.drawLine(mCurrentComment.xPos, 0, mCurrentComment.xPos, getHeight(), mActiveLinePaint);
        }
    }

    public void getHitRect(Rect outRect) {
        if (getAnimation() != null){
            final int offsetY = (int) ImageUtils.getCurrentTransformY(this);
            outRect.set(getLeft(), getTop() + offsetY, getRight(), getBottom() + offsetY);
        } else {
            super.getHitRect(outRect);
        }
    }
}
