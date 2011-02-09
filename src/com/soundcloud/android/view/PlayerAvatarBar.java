package com.soundcloud.android.view;

import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.CloudUtils.GraphicsSizes;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Comment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.ContentHandler;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class PlayerAvatarBar extends View {

    private static final String TAG = "PlayerCommentBar";

    private static final int SDK = Integer.parseInt(Build.VERSION.SDK);

    private static final int GINGERBREAD = 9;

    public static final String BITMAP_LOADER_SERVICE = "com.soundcloud.utils.bitmaploader";
    
    private static final int REFRESH_AVATARS = 0;
    
    private static final int AVATARS_REFRESHED = 1;
    
    private static final int AVATAR_WIDTH = 32;
    
    private long mDuration;
    
    private ArrayList<Comment> mCurrentComments;
    
    private Matrix mMatrix;
    private Float mAvatarScale = (float) 1;
    private Float mDefaultAvatarScale = (float) 1;
    
    private Paint mImagePaint;
    private Paint mLinePaint;
    
    private int mAvatarWidth;
    
    private Thread mAvatarRefreshThread;
    
    private Bitmap mCanvasBmp;
    private Bitmap mNextCanvasBmp;
    
    private Bitmap mDefaultAvatar;
    private Float mDensity;
    
    public PlayerAvatarBar(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        
        this.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.player_avatar_bg));
        
        mResultHandler = new Handler(Looper.getMainLooper());
        mBitmapContentHandler = new BitmapContentHandler();
        mTaskHandlers = new Handler[WORKER_COUNT];
        
        mImagePaint = new Paint();
        mImagePaint.setAntiAlias(false);
        mImagePaint.setFilterBitmap(true);
        
        
        mLinePaint= new Paint();
        mLinePaint.setColor(com.soundcloud.android.R.color.commentLine);
        
        mDefaultAvatar = BitmapFactory.decodeResource(context.getResources(), R.drawable.avatar_badge);

        mDensity = getContext().getResources().getDisplayMetrics().density;
        
        mMatrix = new Matrix();
        if (mDensity > 1) {
            mAvatarWidth = (int) (AVATAR_WIDTH*mDensity);
            Log.i(TAG,"Setting default height to " + mAvatarWidth);
            mAvatarScale = ((float)mAvatarWidth)/47;
            mDefaultAvatarScale = ((float)mAvatarWidth)/mDefaultAvatar.getHeight();
        }
    }
    
    public int getAvatarWidth(){
        return mAvatarWidth;
    }
    
    public void clearTrackData(){
        mUIHandler.removeMessages(REFRESH_AVATARS);
        mUIHandler.removeMessages(AVATARS_REFRESHED);
        
        stopLoading();
        
        if (mCurrentComments != null)
        for (Comment c : mCurrentComments){
            if (c.avatar != null)
                c.avatar.recycle();
            
            c.avatar = null;
        }
        
        mCurrentComments = null;
        mDuration = -1;
        
        if (mCanvasBmp != null)
            mCanvasBmp.recycle();
        
        if (mNextCanvasBmp != null)
            mNextCanvasBmp.recycle();
        
    }

    public void setTrackData(long duration, ArrayList<Comment> newItems){
        
        mDuration = duration;
        mCurrentComments = newItems;
        String avatarUrl;
        
        for (int i = 0; i < newItems.size(); i++){
            if (getContext().getResources().getDisplayMetrics().density > 1) {
                avatarUrl = CloudUtils.formatGraphicsUrl(newItems.get(i).user.avatar_url, GraphicsSizes.badge);
            } else
                avatarUrl = CloudUtils.formatGraphicsUrl(newItems.get(i).user.avatar_url, GraphicsSizes.small);
            
            if (mCurrentComments.get(i).timestamp != -1 && (i == mCurrentComments.size()-1 || mCurrentComments.get(i).timestamp != mCurrentComments.get(i+1).timestamp)) {
                mCurrentComments.get(i).topLevelComment = true;
            }
            
            Bitmap bitmap = getBitmap(avatarUrl);
            Throwable error = getError(avatarUrl);
            if (bitmap != null) {
                ((Comment) mCurrentComments.get(i)).avatar = bitmap;
                if (mCurrentComments.get(i).topLevelComment){
                    if (!mUIHandler.hasMessages(REFRESH_AVATARS)){
                        Message msg = mUIHandler.obtainMessage(REFRESH_AVATARS);
                        PlayerAvatarBar.this.mUIHandler.sendMessageDelayed(msg,100);
                    }
                }
                
            } else {
                if (error != null) {
                } else if (CloudUtils.checkIconShouldLoad(((Comment) mCurrentComments.get(i)).user.avatar_url)){
                    ImageTask task = new ImageTask(((Comment) mCurrentComments.get(i)), avatarUrl);
                    postTask(task);
                }
            }
        }
    }
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l,t,r,b);
        
        if (changed){
            mUIHandler.removeMessages(REFRESH_AVATARS);
            Message msg = mUIHandler.obtainMessage(REFRESH_AVATARS);
            PlayerAvatarBar.this.mUIHandler.sendMessage(msg);    
        }
        
    }
    
    class AvatarRefresher implements Runnable{

        @Override
        public void run() {
            if (mCurrentComments == null || getWidth() <= 0)
                return;
            
            mNextCanvasBmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            
            Canvas canvas = new Canvas(mNextCanvasBmp);
            
            for (Comment comment : mCurrentComments){
                if (Thread.currentThread().isInterrupted())
                    break;
                if (comment.timestamp == 0) continue;
                if (comment.xPos == -1) comment.calculateXPos(getWidth(), mDuration);
                if (comment.topLevelComment){
                    if (!CloudUtils.checkIconShouldLoad(comment.user.avatar_url) || comment.avatar == null){
                        mMatrix.setScale(mDefaultAvatarScale, mDefaultAvatarScale);
                        mMatrix.postTranslate(comment.xPos, 0);
                        canvas.drawBitmap(mDefaultAvatar, mMatrix, mImagePaint);
                        canvas.drawRect(comment.xPos-1, 0, comment.xPos+1, AVATAR_WIDTH*mDensity, mLinePaint);
                    } else if (comment.avatar != null){
                        mMatrix.setScale(mAvatarScale, mAvatarScale);
                        mMatrix.postTranslate(comment.xPos, 0);
                        canvas.drawBitmap(comment.avatar, mMatrix, mImagePaint);
                        canvas.drawRect(comment.xPos-1, 0, comment.xPos+1, AVATAR_WIDTH*mDensity, mLinePaint);    
                    } else {
                       // Log.i(TAG,"Bitmap not found " + comment.user.avatar_url);
                    }
                }
            }
            
            if (Thread.currentThread().isInterrupted())
                mNextCanvasBmp.recycle();
            else{
                mUIHandler.removeMessages(AVATARS_REFRESHED);
                Message msg = mUIHandler.obtainMessage(AVATARS_REFRESHED);
                PlayerAvatarBar.this.mUIHandler.sendMessage(msg);
            }
        }
    }
    
    Handler mUIHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what){
                
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
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (mCanvasBmp != null)
            canvas.drawBitmap(mCanvasBmp, new Matrix(), mImagePaint);
        
    }
    

    @SuppressWarnings({ "rawtypes" })
    private static final Class[] TYPE_BITMAP = {
        Bitmap.class
    };

    public interface Callback {
        /**
         * Notifies an observer that an image was loaded.
         * <p>
         * The bitmap will be assigned to the {@link ImageView} automatically.
         * <p>
         * Use this callback to dismiss any loading indicators.
         * 
         * @param mBitmap the {@link ImageView} that was loaded.
         * @param url the URL that was loaded.
         */
        void onImageLoaded(Bitmap mBitmap, String url);

        /**
         * Notifies an observer that an image could not be loaded.
         * 
         * @param url the URL that could not be loaded.
         * @param error the exception that was thrown.
         */
        void onImageError(String url, Throwable error);
    }

    public static enum LoadResult {
        /**
         * Returned when an image needs to be loaded asynchronously.
         * <p>
         * Callers may wish to assign a placeholder or show a progress spinner
         * while the image is being loaded whenever this value is returned.
         */
        LOADING,
        /**
         * Returned when an attempt to load the image has already been made and
         * it failed.
         * <p>
         * Callers may wish to show an error indicator when this value is
         * returned.
         *
         * @see ImageLoader.Callback
         */
        ERROR
    }

    /**
     * The number of worker threads for loading images.
     */
    private static final int WORKER_COUNT = 3;

    /**
     * Returns {@code true} if the {@link HandlerThread} for a {@link Handler}
     * is waiting.
     */
    private static boolean isWaiting(Handler handler) {
        Looper looper = handler.getLooper();
        Thread thread = looper.getThread();
        Thread.State state = thread.getState();
        return state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING;
    }

    /**
     * Returns the value that should be used for {@link Message#what} for
     * messages placed in the handlers referenced by {@link #mTaskHandlers}.
     *
     * @return the hash code of the URL. This makes it easy to guess if a
     *         {@link Handler} already contains a {@link ImageTask} for a URL to
     *         avoid loading the image twice.
     * @see Handler#hasMessages(int)
     */
    private static int getWhat(String url) {
        return url.hashCode();
    }

    private static String getProtocol(String url) {
        Uri uri = Uri.parse(url);
        return uri.getScheme();
    }

    /**
     * Handlers for loading images in the background (one for each worker).
     */
    private final Handler[] mTaskHandlers;
    
    private final ContentHandler mBitmapContentHandler;

    /**
     * Handler for processing task results (executed on UI thread).
     */
    private final Handler mResultHandler;
   
    /**
     * Opens a {@link URLConnection}, disabling {@code http.keepAlive} on
     * platforms where the feature does not work reliably.
     */
    private URLConnection openConnection(URL url) throws IOException {
        if (SDK < GINGERBREAD) {
            // Releases before Gingerbread do not
            // have reliable http.keepAlive support
            // (HttpURLConnection will often return -1).
            System.setProperty("http.keepAlive", "false");
        }
        return url.openConnection();
    }

    /**
     * Indicates that the given {@link URLConnection} is no longer needed.
     * <p>
     * If the {@link URLConnection} is an {@link HttpURLConnection} and HTTP
     * {@code Keep-Alive} is enabled, the {@link Socket} will be returned to the
     * connection pool. If HTTP {@code Keep-Alive} is disabled, the connection
     * to the {@link Socket} will be closed.
     */
    private static void disconnect(URLConnection connection) {
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) connection;
            http.disconnect();
        }
    }

    private Handler createTaskHandler() {
        HandlerThread thread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Looper looper = thread.getLooper();
        return new TaskHandler(looper);
    }

    /**
     * Gets a {@link Handler} for the given URL.
     * <p>
     * Requests are distributed among the workers using the hash code of the
     * URL. Using the hash code of the URL ensures that requests for the same
     * URL go to the same worker. This prevents multiple workers from
     * downloading the same images in parallel, which would be a waste of
     * resources.
     *
     * @param url the image URL.
     * @return a request handler.
     */
    private Handler getTaskHandler(String url) {
        int what = getWhat(url);

        // First, look for a Handler that may already be loading this URL.
        // If one is found, use the same Handler so that
        // the image is only loaded once.
        for (Handler handler : mTaskHandlers) {
            if (handler != null && handler.hasMessages(what)) {
                return handler;
            }
        }

        // Second, look for a Handler that is not busy.
        for (int index = 0; index < mTaskHandlers.length; index++) {
            Handler handler = mTaskHandlers[index];
            if (handler == null || isWaiting(handler)) {
                if (handler == null) {
                    handler = mTaskHandlers[index] = createTaskHandler();
                }
                return handler;
            }
        }

        // Finally, group requests by authority.
        // This grouping encourages connection re-use.
        //
        // It is important to first look for a Handler
        // that is not busy, otherwise all requests
        // will always go to the same handler if
        // they all have the same URL authority.
        Uri uri = Uri.parse(url);
        String authority = uri.getAuthority();
        if (authority == null) {
            throw new IllegalArgumentException(url);
        }
        int index = Math.abs(authority.hashCode()) % mTaskHandlers.length;
        Handler handler = mTaskHandlers[index];
        if (handler == null) {
            handler = mTaskHandlers[index] = createTaskHandler();
        }
        return handler;
    }

    private void postTask(ImageTask task) {
        String url = task.getUrl();
        int what = getWhat(url);
        Handler handler = getTaskHandler(url);
        Message msg = handler.obtainMessage(what, task);
        handler.sendMessageAtFrontOfQueue(msg);
    }


    private void postResult(ImageTask task) {
        mResultHandler.post(task);
    }

    private void putBitmap(String url, Bitmap bitmap) {
        SoundCloudApplication.mBitmaps.put(url, new SoftReference<Bitmap>(bitmap));
    }

    private void putError(String url, Throwable error) {
        SoundCloudApplication.mBitmapErrors.put(url, error);
    }

    private boolean hasError(String url) {
        return SoundCloudApplication.mBitmapErrors.containsKey(url);
    }

    private Bitmap getBitmap(String url) {
        SoftReference<Bitmap> reference = SoundCloudApplication.mBitmaps.get(url);
        return reference != null ? reference.get() : null;
    }

    private Throwable getError(String url) {
        return SoundCloudApplication.mBitmapErrors.get(url);
    }

    /**
     * Stops the worker threads and discards any image tasks that have not been
     * started.
     */
    public void stopLoading() {
        for (int i = 0; i < mTaskHandlers.length; i++) {
            Handler handler = mTaskHandlers[i];
            if (handler != null) {
                Looper looper = handler.getLooper();
                looper.quit();
                mTaskHandlers[i] = null;
            }
        }
    }

    private class ImageTask implements Runnable {

        /**
         * A {@link WeakReference} to the {@link ImageView} to be bound or
         * {@code null}.
         * <p>
         * Using a {@link WeakReference} allows the heavy-weight
         * {@link Activity}/{@link Context} object associated with the
         * {@link ImageView} to be freed before the tasks completes.
         */

        private final Comment mComment;

        private final String mUrl;

        private Bitmap mBitmap;

        private Throwable mError;

        private ImageTask(Comment comment, String formattedUrl) {
            mComment = comment;
            mUrl = formattedUrl;
        }

        /**
         * Returns the URL parameter passed to the constructor.
         */
        public String getUrl() {
            return mUrl;
        }

        /**
         * Executes the {@link ImageTask}.
         *
         * @return {@code true} if the result for this {@link ImageTask} should
         *         be posted, {@code false} otherwise.
         */
        public boolean execute() {
            try {

                // Check if the last attempt to load the URL had an error
                mError = getError(mUrl);
                if (mError != null) {
                    return true;
                }

                // Check if the Bitmap is already cached in memory
                mBitmap = getBitmap(mUrl);
                if (mBitmap != null) {
                    // Keep a hard reference until the view has been notified.
                    return true;
                }

                URL url = new URL(null, mUrl);
                URLConnection connection = openConnection(url);
                try {
                    mBitmap = (Bitmap) mBitmapContentHandler.getContent(connection, TYPE_BITMAP);
                    if (mBitmap == null) {
                        throw new NullPointerException();
                    }
                    return true;
                } finally {
                    disconnect(connection);
                }
            } catch (IOException e) {
                mError = e;
                return true;
            } catch (RuntimeException e) {
                mError = e;
                return true;
            } catch (Error e) {
                mError = e;
                return true;
            }
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            if (mBitmap != null) {
                putBitmap(mUrl, mBitmap);
                mComment.avatar = mBitmap;
                if (mComment.topLevelComment){
                    if (!mUIHandler.hasMessages(REFRESH_AVATARS)){
                        Message msg = mUIHandler.obtainMessage(REFRESH_AVATARS);
                        PlayerAvatarBar.this.mUIHandler.sendMessageDelayed(msg,100);
                    }
                }
                
            } else if (mError != null && !hasError(mUrl)) {
                putError(mUrl, mError);
            }
            
        }
    }

    private class TaskHandler extends Handler {
        public TaskHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ImageTask task = (ImageTask) msg.obj;
            if (task.execute()) {
                postResult(task);
            } else {
                // No result or the result is no longer needed.
            }
        }
    }

}
