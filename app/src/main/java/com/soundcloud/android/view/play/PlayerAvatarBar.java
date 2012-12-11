package com.soundcloud.android.view.play;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.imageloader.ImageLoader.BitmapCallback;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.utils.ImageUtils;
import org.jetbrains.annotations.Nullable;

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

    private Consts.GraphicSize mTargetSize;

    private Thread mAvatarRefreshThread;

    private @Nullable Bitmap mCanvasBmp;
    private Bitmap mNextCanvasBmp;

    private Bitmap mDefaultAvatar;

    private ImageLoader mBitmapLoader;
    private Consts.GraphicSize mAvatarGraphicsSize;

    private Context mContext;
    private boolean mLandscape;

    public PlayerAvatarBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mContext = context;
        mBitmapLoader = ImageLoader.get(context.getApplicationContext());

        if (ImageUtils.isScreenXL(mContext)) {
            mAvatarGraphicsSize= Consts.GraphicSize.LARGE;
        } else {
            mAvatarGraphicsSize = mContext.getResources().getDisplayMetrics().density > 1 ?
                    Consts.GraphicSize.BADGE :
                    Consts.GraphicSize.SMALL;
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

        if (ImageUtils.isScreenXL(mContext)) {
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
                    mBitmapLoader.cancelRequest(mTargetSize.formatUri(c.user.avatar_url));
                }
            }
        }
    }

    public void clearTrackData(){
        mUIHandler.removeMessages(REFRESH_AVATARS);
        mUIHandler.removeMessages(AVATARS_REFRESHED);

        if (mCurrentComments != null) {
            for (Comment c : mCurrentComments) {
                mBitmapLoader.cancelRequest(mTargetSize.formatUri(c.user.avatar_url));
                c.avatar = null;
            }
        }

        mCurrentComment = null;
        mCurrentComments = null;
        mDuration = -1;

        if (mCanvasBmp != null) mCanvasBmp.recycle();
        if (mNextCanvasBmp != null) mNextCanvasBmp.recycle();

        mCanvasBmp = mNextCanvasBmp = null;

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

    public void setCurrentComment(@Nullable Comment c){
        mCurrentComment = c;
        if (c != null) loadAvatar(c);
        invalidate();
    }

    private void loadAvatar(final Comment c){
        if (c == null || !c.shouldLoadIcon()) return;

        ImageLoader.get(mContext).getBitmap(mAvatarGraphicsSize.formatUri(c.user.avatar_url), new BitmapCallback() {
            @Override
            public void onImageLoaded(Bitmap bitmap, String uri) {
                c.avatar = bitmap;
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
                    ImageUtils.isScreenXL(mContext) ? R.drawable.avatar_badge_large : R.drawable.avatar_badge);
            mDefaultAvatarScale = ((float) mAvatarWidth) / mDefaultAvatar.getHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l,t,r,b);

        if (changed){

            float mDensity = getContext().getResources().getDisplayMetrics().density;
            if (mDensity > 1) {
                mAvatarWidth = ImageUtils.isScreenXL(mContext) ? (int) (AVATAR_WIDTH_LARGE* mDensity) : getHeight() ;
            } else {
                mAvatarWidth = ImageUtils.isScreenXL(mContext)? AVATAR_WIDTH_LARGE : getHeight();
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
            if (comments == null || getWidth() <= 0) return;

            try {
                mNextCanvasBmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e){
                // XXX really catch oom here?
                Log.e(TAG,"Out of memory during avatar refresher bitmap creation");
            }

            if (mNextCanvasBmp != null && !mNextCanvasBmp.isRecycled()) {
                Canvas canvas = new Canvas(mNextCanvasBmp);
                for (Comment comment : comments){
                    if (Thread.currentThread().isInterrupted()) break;
                    if (comment.timestamp == 0) continue;
                    drawCommentOnCanvas(comment, canvas, mLinePaint, mImagePaint, mBgMatrix);
                }

                if (Thread.currentThread().isInterrupted()) {
                    mNextCanvasBmp.recycle();
                } else {
                    if (!mUIHandler.hasMessages(AVATARS_REFRESHED)) {
                        Message msg = mUIHandler.obtainMessage(AVATARS_REFRESHED);
                        PlayerAvatarBar.this.mUIHandler.sendMessageDelayed(msg, 200);
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
