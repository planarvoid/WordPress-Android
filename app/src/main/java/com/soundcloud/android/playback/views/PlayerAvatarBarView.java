package com.soundcloud.android.playback.views;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Comment;
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
import android.widget.ImageView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerAvatarBarView extends View {
    private static final String TAG = "PlayerCommentBar";

    private static final int REFRESH_AVATARS = 0;
    private static final int AVATARS_REFRESHED = 1;
    private static final int AVATAR_WIDTH = 32;
    private static final Matrix DEFAULT_MATRIX = new Matrix();

    private long duration;

    private @Nullable List<Comment> currentComments;
    private @Nullable Comment currentComment;

    private Matrix bgMatrix;
    private Matrix activeMatrix;
    private float defaultAvatarScale = 1f;
    private int avatarWidth;

    private Paint imagePaint;
    private Paint linePaint;

    private Paint activeImagePaint;
    private Paint mActiveLinePaint;

    private Thread avatarRefreshThread;

    private ImageOperations imageOperations;

    private @Nullable Bitmap canvasBmp;
    private Bitmap nextCanvasBmp;

    private Bitmap defaultAvatar;

    private ApiImageSize avatarGraphicsSize;

    private Set<ImageView> avatarLoadingViews;

    private boolean landscape;

    public PlayerAvatarBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        imageOperations = SoundCloudApplication.fromContext(context).getImageOperations();

        avatarGraphicsSize = context.getResources().getDisplayMetrics().density > 1 ?
                ApiImageSize.BADGE :
                ApiImageSize.SMALL;

        avatarLoadingViews = new HashSet<ImageView>(Consts.MAX_COMMENTS_TO_LOAD);

        imagePaint = new Paint();
        imagePaint.setAntiAlias(false);
        imagePaint.setFilterBitmap(true);

        activeImagePaint = new Paint();
        activeImagePaint.setAntiAlias(false);
        activeImagePaint.setFilterBitmap(true);

        linePaint = new Paint();
        linePaint.setColor(getResources().getColor(R.color.comment_line));

        mActiveLinePaint = new Paint();
        mActiveLinePaint.setColor(getResources().getColor(com.soundcloud.android.R.color.active_comment_line));

        float mDensity = getContext().getResources().getDisplayMetrics().density;

        bgMatrix = new Matrix();
        activeMatrix = new Matrix();
        avatarWidth = (int) (AVATAR_WIDTH * mDensity);
    }

    public int getAvatarWidth(){
        return avatarWidth;
    }

    public void stopAvatarLoading(){
        for (ImageView imageView : avatarLoadingViews) {
            if (imageView != null) {
                imageOperations.cancel(imageView);
            }
        }
        avatarLoadingViews.clear();
    }

    public void clearTrackData(){
        uIHandler.removeMessages(REFRESH_AVATARS);
        uIHandler.removeMessages(AVATARS_REFRESHED);

        if (avatarRefreshThread != null){
            avatarRefreshThread.interrupt();
            avatarRefreshThread = null;
        }

        if (currentComments != null) {
            for (Comment c : currentComments) {
                c.avatar = null;
            }
        }

        currentComment = null;
        currentComments = null;
        duration = -1;

        if (canvasBmp != null) canvasBmp.recycle();
        if (nextCanvasBmp != null) nextCanvasBmp.recycle();

        canvasBmp = null;

        invalidate();

    }

    public void setTrackData(long duration, List<Comment> newItems){
        this.duration = duration;
        currentComments = newItems;
        for (Comment c : newItems){
            loadAvatar(c);
        }
        invalidate();
    }

    public void setCurrentComment(@Nullable Comment c){
        currentComment = c;
        if (c != null) loadAvatar(c);
        invalidate();
    }

    private void loadAvatar(final Comment c){
        if (c == null || !c.shouldLoadIcon()) return;

         imageOperations.load(avatarGraphicsSize.formatUri(c.user.avatar_url), new ImageListener() {
             @Override
             public void onLoadingStarted(String imageUri, View view) {
                 avatarLoadingViews.add((ImageView) view);
             }

             @Override
             public void onLoadingFailed(String imageUri, View view, String failedReason) {
             }

             @Override
             public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                 avatarLoadingViews.remove(view);
                 c.avatar = loadedImage;
                 if (c.topLevelComment) {
                     if (!uIHandler.hasMessages(REFRESH_AVATARS)) {
                         Message msg = uIHandler.obtainMessage(REFRESH_AVATARS);
                         PlayerAvatarBarView.this.uIHandler.sendMessageDelayed(msg, 100);
                     }
                 }
             }
         });
    }

    private void refreshDefaultAvatar() {
        if (avatarWidth > 0 && (defaultAvatar == null || defaultAvatar.isRecycled())) {
            defaultAvatar = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.avatar_badge);
            defaultAvatarScale = ((float) avatarWidth) / defaultAvatar.getHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l,t,r,b);

        if (changed){

            avatarWidth = getHeight() ;
            refreshDefaultAvatar();

            uIHandler.removeMessages(REFRESH_AVATARS);
            Message msg = uIHandler.obtainMessage(REFRESH_AVATARS);
            PlayerAvatarBarView.this.uIHandler.sendMessage(msg);
        }

    }

    public void setIsLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    class AvatarRefresher implements Runnable {
        @Override
        public void run() {
            // XXX race condition with current comments
            final List<Comment> comments = currentComments;
            final int width = getWidth();
            final int height = getHeight();
            if (comments != null && width > 0 && height > 0) {
                try {
                    nextCanvasBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError e) {
                    // XXX really catch oom here?
                    Log.e(TAG, "Out of memory during avatar refresher bitmap creation");
                }

                if (nextCanvasBmp != null && !nextCanvasBmp.isRecycled()) {
                    Canvas canvas = new Canvas(nextCanvasBmp);
                    for (Comment comment : comments) {
                        if (Thread.currentThread().isInterrupted()) break;
                        if (comment.timestamp == 0) continue;
                        drawCommentOnCanvas(comment, canvas, linePaint, imagePaint, bgMatrix);
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        nextCanvasBmp.recycle();
                        nextCanvasBmp = null;
                    } else {
                        if (!uIHandler.hasMessages(AVATARS_REFRESHED)) {
                            Message msg = uIHandler.obtainMessage(AVATARS_REFRESHED);
                            PlayerAvatarBarView.this.uIHandler.sendMessageDelayed(msg, 200);
                        }

                    }
                }
            }
        }
    }

    private final Handler uIHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_AVATARS:
                    if (currentComments == null || duration == -1)
                        return;

                    if (avatarRefreshThread != null)
                        avatarRefreshThread.interrupt();

                    avatarRefreshThread = new Thread(new AvatarRefresher());
                    avatarRefreshThread.start();
                    break;

                case AVATARS_REFRESHED:
                    if (canvasBmp != null)
                        canvasBmp.recycle();

                    canvasBmp = nextCanvasBmp;
                    invalidate();
                    break;
            }
        }
    };

    private void drawCommentOnCanvas(Comment comment, Canvas canvas, Paint linePaint, Paint imagePaint, Matrix matrix){
        if (canvas == null) return;
        final Bitmap avatar = comment.avatar;

        if (avatar == null || avatar.isRecycled() || !comment.shouldLoadIcon()) {
            if (landscape) {
                refreshDefaultAvatar();
                matrix.setScale(defaultAvatarScale, defaultAvatarScale);
                matrix.postTranslate(comment.xPos, 0);
                canvas.drawBitmap(defaultAvatar, matrix, imagePaint);
            }
            canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), linePaint);

            if (avatar != null && avatar.isRecycled()) loadAvatar(comment);

        } else {
            final float drawScale = ((float) avatarWidth) / avatar.getHeight();
            matrix.setScale(drawScale, drawScale);
            matrix.postTranslate(comment.xPos, 0);
            canvas.drawBitmap(avatar, matrix, imagePaint);
            canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), linePaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (canvasBmp != null && !canvasBmp.isRecycled()) {
            canvas.drawBitmap(canvasBmp, DEFAULT_MATRIX, imagePaint);
        } else if (currentComments != null) {
            for (Comment comment : currentComments){
                canvas.drawLine(comment.xPos, 0, comment.xPos, getHeight(), linePaint);
            }
        }

        if (currentComment != null){
            drawCommentOnCanvas(currentComment,canvas,mActiveLinePaint, activeImagePaint, activeMatrix);
            canvas.drawLine(currentComment.xPos, 0, currentComment.xPos, getHeight(), mActiveLinePaint);
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
