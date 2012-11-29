package com.soundcloud.android.imageloader;

import com.soundcloud.android.cache.FileCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.CacheResponse;
import java.net.ContentHandler;
import java.net.ResponseCache;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A helper class to executeAppendTask images asynchronously.
 * Original code Copyright (C) 2010 Google Inc.
 *
 * @see <a href="http://code.google.com/p/libs-for-android/">libs-for-android</a>
 */
public class ImageLoader {
    public static final String TAG = "ImageLoader";
    public static final String IMAGE_LOADER_SERVICE = "com.google.android.imageloader";
    public static final int DEFAULT_TASK_LIMIT = 3;

    public interface LoadBlocker {
        // time to wait before releasing locks
        int TIMEOUT = 3000;
    }

    private WeakHashMap<LoadBlocker, Long> mLoadBlockers = new WeakHashMap<LoadBlocker, Long>();

    /**
     * The default cache size (in bytes). 1/5 of available memory, up to a maximum of 16MB
     */
    public static final long DEFAULT_CACHE_SIZE = Math.min(Runtime.getRuntime().maxMemory() / 5, 16 * 1024 * 1024);

    private List<ImageCallback> mPendingCallbacks = new ArrayList<ImageCallback>();
    private final LinkedList<ImageRequest> mRequests;
    private final Set<ImageRequest> mAllRequests = new HashSet<ImageRequest>();

    private final ContentHandler mBitmapContentHandler;
    private final ContentHandler mPrefetchContentHandler;
    private final BitmapCache<String> mBitmaps;
    private final LruCache<String,ImageError> mErrors;

    private final int mMaxTaskCount;
    private int mActiveTaskCount;

    /**
     * Tracks the last URL that was bound to an {@link ImageView}.
     * <p/>
     * This ensures that the right image is shown in the case where a new URL is
     * assigned to an {@link ImageView} before the previous asynchronous task
     * completes.
     * <p/>
     * This <em>does not</em> ensure that an image assigned with
     * {@link ImageView#setImageBitmap(Bitmap)},
     * {@link ImageView#setImageDrawable(android.graphics.drawable.Drawable)},
     * {@link ImageView#setImageResource(int)}, or
     * {@link ImageView#setImageURI(android.net.Uri)} is not replaced. This
     * behavior is important because callers may invoke these methods to assign
     * a placeholder when a bind method returns {@link BindResult#LOADING} or
     * {@link BindResult#ERROR}.
     */
    private final Map<ImageView, String> mImageViewBinding;

    public ImageLoader() {
        this(null, null, DEFAULT_CACHE_SIZE, DEFAULT_TASK_LIMIT);
    }

    public ImageLoader(@Nullable ContentHandler bitmapHandler,
                       @Nullable ContentHandler prefetchHandler,
                       long cacheSize, int taskLimit) {
        if (taskLimit < 1) throw new IllegalArgumentException("Task limit must be positive");
        if (cacheSize < 1) throw new IllegalArgumentException("Cache size must be positive");

        mMaxTaskCount = taskLimit;
        mBitmapContentHandler = bitmapHandler != null ? bitmapHandler : new DownloadBitmapContentHandler();
        mPrefetchContentHandler = prefetchHandler;
        mImageViewBinding = new WeakHashMap<ImageView, String>();

        mRequests = new LinkedList<ImageRequest>();
        mBitmaps = new BitmapCache<String>((int)cacheSize);
        mErrors =  new LruCache<String, ImageError>(256);
    }

    public static ImageLoader get(Context context) {
        ImageLoader loader = (ImageLoader) context.getSystemService(IMAGE_LOADER_SERVICE);
        if (loader == null) {
            context = context.getApplicationContext();
            loader = (ImageLoader) context.getSystemService(IMAGE_LOADER_SERVICE);
        }
        if (loader == null) {
            throw new IllegalStateException("ImageLoader not available");
        }
        return loader;
    }

    public void block(LoadBlocker blocker) {
        mLoadBlockers.put(blocker, System.currentTimeMillis());
    }

    public void unblock(LoadBlocker blocker) {
        for (Iterator<LoadBlocker> it = mLoadBlockers.keySet().iterator(); it.hasNext(); ) {
            if (it.next() == blocker) it.remove();
        }
        if (mLoadBlockers.isEmpty()) {
            flushRequests();
            for (ImageCallback imageCallback : mPendingCallbacks) {
                imageCallback.send();
            }
            mPendingCallbacks.clear();
        }
    }


    public void cancelRequest(String url) {
        ImageRequest toRemove = null;
        for (ImageRequest request : mRequests) {
            if (request.getUrl().equals(url)) toRemove = request;
        }
        if (toRemove != null) mRequests.remove(toRemove);
    }


    private boolean isBlocked() {
        boolean blocked = !mLoadBlockers.isEmpty();
        if (blocked) {
            for (Map.Entry<LoadBlocker, Long> entry : new HashMap<LoadBlocker, Long>(mLoadBlockers).entrySet()) {
                if (System.currentTimeMillis() - entry.getValue() > LoadBlocker.TIMEOUT) unblock(entry.getKey());
            }
            blocked = !mLoadBlockers.isEmpty();
        }
        return blocked;
    }

    private void flushRequests() {
        if (!isBlocked()) {
            while (mActiveTaskCount < mMaxTaskCount && !mRequests.isEmpty()) {
                new ImageTask().executeOnThreadPool(mRequests.poll());
            }
        }
    }


    public Bitmap getBitmap(String uri, BitmapCallback callback) {
        return getBitmap(uri, callback, new Options());
    }

    public Bitmap getBitmap(String uri, @Nullable BitmapCallback callback, Options options) {
        if (options == null) options = new Options();
        final Bitmap memoryBmp = getBitmap(uri);
        if (memoryBmp != null) {
            if (callback != null) {
                callback.setResult(uri, memoryBmp, null);
                callback.send();
            }
            return memoryBmp;
        } else if (options.loadRemotelyIfNecessary) {
            queueRequest(uri, callback, options);
        }
        return null;
    }

    /**
     * Binds an image at the given URL to an {@link ImageView}.
     * <p/>
     * If the image needs to be loaded asynchronously, it will be assigned at a
     * later time, replacing any existing {@link Drawable} unless
     * {@link #unbind(ImageView)} is called or this method is called with the same
     * {@link ImageView}, but a different URL.
     * <p/>
     * Use {@link #bind(BaseAdapter, ImageView, String, Options)} instead of this method
     * when the {@link ImageView} is in an {@link android.widget.AdapterView} so that the image
     * will be bound correctly in the case where it has been assigned to a
     * different position since the asynchronous request was started.
     *
     * @param view     the {@link ImageView} to bind.
     * @param url      the image URL.s
     * @param callback invoked when there is an error loading the image. This
     *                 parameter can be {@code null} if a callback is not required.
     * @param options  additional options
     * @return a {@link BindResult}.
     * @throws IllegalArgumentException if a required argument is {@code null}
     */
    public BindResult bind(ImageView view, String url, Callback callback, Options options) {
        if (view == null) throw new IllegalArgumentException("ImageView is null");
        if (url == null)  throw new IllegalArgumentException("URL is null");

        mImageViewBinding.put(view, url);

        Bitmap bitmap = getBitmap(url);
        ImageError error = getError(url);
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            return BindResult.OK;
        } else {
            // Clear the ImageView by default. The caller can set their own placeholder based on the return value.
            final Bitmap temporaryBitmap = options.temporaryBitmapRef != null ? options.temporaryBitmapRef.get() : null;
            if (temporaryBitmap != null) {
                view.setImageBitmap(temporaryBitmap);
            } else {
                view.setImageDrawable(null);
            }

            if (error != null) {
                return BindResult.ERROR;
            } else {
                return queueRequest(url, new ImageViewCallback(view, callback), options);
            }
        }
    }

    private BindResult queueRequest(String url, @Nullable ImageCallback callback, Options options) {
        for (ImageRequest r : mAllRequests) {
            if (r.getUrl().equals(url)) {
                // already been queued, add our callback
                r.add(callback);
                return BindResult.LOADING;
            }
        }
        ImageRequest request = new ImageRequest(url, callback,  options);
        if (options.postAtFront) {
            insertRequestAtFrontOfQueue(request);
        } else {
            enqueueRequest(request);
        }
        return BindResult.LOADING;
    }

    private void enqueueRequest(ImageRequest request) {
        mRequests.add(request);
        mAllRequests.add(request);
        flushRequests();
    }

    private void insertRequestAtFrontOfQueue(ImageRequest request) {
        mRequests.add(0, request);
        mAllRequests.add(request);
        flushRequests();
    }

    public BindResult bind(ImageView view, String url, @Nullable Callback callback) {
        return bind(view, url, callback, new Options());
    }

    public BindResult bind(BaseAdapter adapter, ImageView view, String url, @Nullable Options options) {
        if (adapter == null) throw new NullPointerException("Adapter is null");
        if (view == null) throw new NullPointerException("ImageView is null");
        if (url == null) throw new NullPointerException("URL is null");
        if (options == null) options = new Options();

        Bitmap bitmap = getBitmap(url);
        ImageError error = getError(url);
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            return BindResult.OK;
        } else {
            // Clear the ImageView by default. The caller can set their own placeholder based on the return value.
            view.setImageDrawable(null);
            if (error != null) {
                return BindResult.ERROR;
            } else {
                // For adapters, post the latest requests
                // at the front of the queue in case the user
                // has already scrolled past most of the images
                // that are currently in the queue.
                options.postAtFront = true;
                return queueRequest(url, new BaseAdapterCallback(adapter), options);
            }
        }
    }

    public BindResult bind(BaseAdapter adapter, ImageView view, String url) {
        return bind(adapter, view, url, new Options());
    }

    /**
     * Cancels an asynchronous request to bind an image URL to an
     * {@link ImageView} and clears the {@link ImageView}.
     *
     * @see #bind(ImageView, String, Callback, Options)
     */
    public void unbind(ImageView view) {
        mImageViewBinding.remove(view);
        view.setImageDrawable(null);
    }

    /**
     * Clears any cached errors.
     * <p/>
     * Call this method when a network connection is restored, or the user
     * invokes a manual executeRefreshTask of the screen.
     */
    public void clearErrors() {
        mErrors.evictAll();
    }

    /**
     * Pre-fetches the binary content for an image and stores it in a file-based
     * cache (if it is not already cached locally) without loading the image
     * data into memory.
     * <p/>
     * Pre-fetching should not be used unless a {@link ContentHandler} with
     * support for persistent caching was passed to the constructor.
     *
     * @param url the URL to pre-fetch.
     * @throws NullPointerException if the URL is {@code null}
     */
    public void prefetch(String url) {
        prefetch(url, new Options());
    }

    public void prefetch(String url, Options options) {
        if (url == null) throw new NullPointerException();
        if (getBitmap(url) != null) {
            // The image is already loaded, therefore it does not need to be prefetched.
            return;
        }
        if (getError(url) != null) {
            // A recent attempt to executeAppendTask or prefetch the image failed,
            // therefore this attempt is likely to fail as well.
            return;
        }
        if (options == null)  options = new Options();
        options.loadBitmap = false;
        queueRequest(url, null, options);
    }

    private void putBitmap(String url, Bitmap bitmap) {
        mBitmaps.put(url, bitmap);
    }

    private void putError(String url, ImageError error) {
        mErrors.put(url, error);
    }

    private Bitmap getBitmap(String url) {
        final Bitmap bmp = mBitmaps.get(url);
        return bmp == null || bmp.isRecycled() ? null : bmp;
    }

    private ImageError getError(String url) {
        ImageError error = mErrors.get(url);
        return error != null && !error.isExpired() ? error : null;
    }

    /**
     * Returns {@code true} if there was an error the last time the given URL
     * was accessed and the error is not expired, {@code false} otherwise.
     */
    private boolean hasError(String url) {
        return getError(url) != null;
    }

    private class ImageRequest {
        private @NotNull final String mUrl;
        private final Set<ImageCallback> mImageCallbacks = new HashSet<ImageCallback>();
        private final Options mOptions;
        private Bitmap mBitmap;
        private ImageError mError;

        private ImageRequest(String url, @Nullable ImageCallback callback, Options options) {
            if (url == null) throw new IllegalArgumentException("URL cannot be null");
            add(callback);
            mUrl = url;
            mOptions = options;
        }

        public void add(ImageCallback cb) {
            if (cb != null) mImageCallbacks.add(cb);
        }
        /**
         * @return {@code true} if the result for this {@link ImageTask} should be posted, {@code false} otherwise.
         */
        public boolean execute() {
            try {
                if (allUnwanted()) return false;

                // Check if the last attempt to executeAppendTask the URL had an error
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
                if (mOptions.loadBitmap) {
                    try {
                        mBitmap = loadImage(url);
                    } catch (OutOfMemoryError e) {
                        // The VM does not always free-up memory as it should,
                        // so manually invoke the garbage collector
                        // and try loading the image again.
                        System.gc();
                        mBitmap = loadImage(url);
                    }
                    if (mBitmap == null) {
                        throw new NullPointerException("ContentHandler returned null");
                    }
                    return true;
                } else {
                    if (mPrefetchContentHandler != null) {
                        // Cache the URL without loading a Bitmap into memory.
                        URLConnection connection = url.openConnection();
                        mPrefetchContentHandler.getContent(connection);
                    }
                    mBitmap = null;
                    return false;
                }
            } catch (IOException e) {
                mError = new ImageError(e);
                return true;
            } catch (RuntimeException e) {
                mError = new ImageError(e);
                return true;
            } catch (Error e) {
                mError = new ImageError(e);
                return true;
            }
        }

        private boolean allUnwanted() {
            if (mImageCallbacks.isEmpty()) {
                return false;
            } else {
                boolean somebodyWants = false;
                for (ImageCallback cb : mImageCallbacks) {
                    if (!cb.unwanted()) {
                        somebodyWants = true;
                        break;
                    }
                }
                return !somebodyWants;
            }
        }

        public void publishResult() {
            if (mBitmap != null) {
                putBitmap(mUrl, mBitmap);
            } else if (mError != null && !hasError(mUrl)) {
                putError(mUrl, mError);
            }

            handleCallbacks();
        }

        private void handleCallbacks() {
            for (ImageCallback cb : mImageCallbacks)  {
                cb.setResult(mUrl, mBitmap, mError);
            }
            if (isBlocked()) {
                mPendingCallbacks.addAll(mImageCallbacks);
            } else {
                for (ImageCallback cb : mImageCallbacks) {
                    cb.send();
                }
            }
            mImageCallbacks.clear();
        }

        public String getUrl() {
            return mUrl;
        }

        private Bitmap loadImage(URL url) throws IOException {
            // check cache first if we have a standard FileCache installed
            // needed because cache doesn't work if connection drops on older devices
            // TODO: staleness checks ?
            ResponseCache cache = ResponseCache.getDefault();
            if (cache instanceof FileCache) {
                CacheResponse response = ((FileCache) cache).getCacheResponse(url.toString());
                if (response != null) {
                    InputStream body = response.getBody();
                    try {
                        return BitmapFactory.decodeStream(body);
                    } finally {
                        body.close();
                    }
                }
            }
            // fallback - open connection and use whatever is provided by the system
            return (Bitmap) mBitmapContentHandler.getContent(url.openConnection());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImageRequest that = (ImageRequest) o;
            return mUrl.equals(that.mUrl);
        }

        @Override
        public int hashCode() {
            return mUrl.hashCode();
        }
    }

    private interface ImageCallback {
        boolean unwanted();
        void setResult(String url, Bitmap bitmap, ImageError imageError);
        void send();
    }

    public static class BitmapCallback implements ImageCallback {
        private String mUrl;
        private Bitmap mBitmap;
        private ImageError mError;

        @Override
        public boolean unwanted() {
            return false;
        }

        @Override
        public void setResult(String url, Bitmap bitmap, @Nullable ImageError imageError) {
            mUrl = url;
            mBitmap = bitmap;
            mError = imageError;
        }

        public void send() {
            if (mError == null) {
                onImageLoaded(mBitmap, mUrl);
            } else {
                onImageError(mUrl, mError.getCause());
            }
        }

        public void onImageLoaded(Bitmap mBitmap, String uri) {
        }

        public void onImageError(String uri, Throwable error) {
        }
    }

    public final class ImageViewCallback implements ImageCallback {
        private final WeakReference<ImageView> mImageView;
        private final Callback mCallback;

        private String mUrl;
        private Bitmap mBitmap;
        private ImageError mError;

        public ImageViewCallback(ImageView imageView, Callback callback) {
            mImageView = new WeakReference<ImageView>(imageView);
            mCallback = callback;
        }

        public boolean unwanted() {
            // Always complete the callback
            return false;
        }

        @Override
        public void setResult(String url, Bitmap bitmap, ImageError imageError) {
            mUrl = url;
            mBitmap = bitmap;
            mError = imageError;
        }

        public void send() {
            ImageView imageView = mImageView.get();
            if (imageView == null) return;
            String binding = mImageViewBinding.get(imageView);
            if (!TextUtils.equals(binding, mUrl)) {
                // The ImageView has been unbound or bound to a different URL since the task was started.
                return;
            }

            Context context = imageView.getContext();
            if (context instanceof Activity) {
                Activity activity = (Activity) context;
                if (activity.isFinishing()) {
                    return;
                }
            }

            if (mBitmap != null) {
                imageView.setImageBitmap(mBitmap);
                if (mCallback != null) {
                    mCallback.onImageLoaded(imageView, mUrl);
                }
            } else if (mError != null && mCallback != null) {
                mCallback.onImageError(imageView, mUrl, mError.getCause());
            }
        }
    }

    private static final class BaseAdapterCallback implements ImageCallback {
        private final WeakReference<BaseAdapter> mAdapter;
        public BaseAdapterCallback(BaseAdapter adapter) {
            mAdapter = new WeakReference<BaseAdapter>(adapter);
        }

        public boolean unwanted() {
            return mAdapter.get() == null;
        }

        @Override
        public void setResult(String url, Bitmap bitmap, ImageError imageError) {
        }

        public void send() {
            BaseAdapter adapter = mAdapter.get();
            if (adapter == null) {
                // The adapter is no longer in use
                return;
            }
            if (!adapter.isEmpty()) {
                adapter.notifyDataSetChanged();
            } else {
                // The adapter is empty or no longer in use.
                // It is important that BaseAdapter#notifyDataSetChanged()
                // is not called when the adapter is empty because this
                // may indicate that the data is valid when it is not.
                // For example: when the adapter cursor is deactivated.
            }
        }
    }

    private class ImageTask extends AsyncTask<ImageRequest, ImageRequest, ImageRequest> {
        public final AsyncTask<ImageRequest, ImageRequest, ImageRequest> executeOnThreadPool(ImageRequest... params) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                return execute(params);
            } else {
                return executeOnExecutor(THREAD_POOL_EXECUTOR, params);
            }
        }

        @Override
        protected void onPreExecute() {
            mActiveTaskCount++;
        }

        @Override
        protected ImageRequest doInBackground(ImageRequest... requests) {
            if (requests != null && requests.length > 0) {
                for (ImageRequest request : requests) {
                    //Log.d(TAG, "startExecute("+IOUtils.md5(request.getUrl()) +")");
                    if (request.execute()) {
                        publishProgress(request);
                    }
                }
            }
            return requests == null ? null : requests[0];
        }

        @Override
        protected void onProgressUpdate(ImageRequest... values) {
            for (ImageRequest request : values) {
                request.publishResult();
            }
        }

        @Override
        protected void onPostExecute(ImageRequest result) {
            mActiveTaskCount--;
            //Log.d(TAG, "stop("+IOUtils.md5(result.getUrl()) +")");
            mAllRequests.remove(result);
            flushRequests();

        }
    }

    public static class Options {
        public boolean loadRemotelyIfNecessary;
        public boolean postAtFront;
        public boolean loadBitmap = true;

        public int decodeInSampleSize = 1;
        public WeakReference<Bitmap> temporaryBitmapRef;

        public Options() {
            loadRemotelyIfNecessary = true;
        }

        public Options(boolean postAtFront) {
            this();
            this.postAtFront = postAtFront;
        }
    }

    private static class ImageError {
        private static final int TIMEOUT = 2 * 60 * 1000;

        private final Throwable mCause;
        private final long mTimestamp;

        public ImageError(Throwable cause) {
            if (cause == null) {
                throw new NullPointerException();
            }
            mCause = cause;
            mTimestamp = now();
        }

        public boolean isExpired() {
            return (now() - mTimestamp) > TIMEOUT;
        }

        public Throwable getCause() {
            return mCause;
        }

        private static long now() {
            return SystemClock.elapsedRealtime();
        }
    }

    public interface Callback {
        /**
         * Notifies an observer that an image was loaded.
         * <p/>
         * The bitmap will be assigned to the {@link ImageView} automatically.
         * <p/>
         * Use this callback to dismiss any loading indicators.
         *
         * @param view the {@link ImageView} that was loaded.
         * @param url  the URL that was loaded.
         */
        void onImageLoaded(ImageView view, String url);

        /**
         * Notifies an observer that an image could not be loaded.
         *
         * @param view  the {@link ImageView} that could not be loaded.
         * @param url   the URL that could not be loaded.
         * @param error the exception that was thrown.
         */
        void onImageError(ImageView view, String url, Throwable error);
    }

    public static enum BindResult {
        /**
         * Returned when an image is bound to an {@link ImageView} immediately
         * because it was already loaded.
         */
        OK,
        /**
         * Returned when an image needs to be loaded asynchronously.
         */
        LOADING,
        /**
         * Returned when an attempt to executeAppendTask the image has already been made and
         * it failed.
         */
        ERROR
    }

    public static class BitmapCache<K> extends LruCache<K, Bitmap> {
        // Assume a 32-bit image
        private static final int BYTES_PER_PIXEL = 4;

        /**
         * @param maxSize in bytes
         */
        public BitmapCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(K key, Bitmap b) {
            return b.getWidth() * b.getHeight() * BYTES_PER_PIXEL;
        }
    }
}