package com.google.android.imageloader;


import com.soundcloud.android.SoundCloudApplication;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.ContentHandler;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class ImageLoader {

    private static final String TAG = "ImageLoader";


    private static final int GINGERBREAD = 9;

    public static final String IMAGE_LOADER_SERVICE = "com.soundcloud.utils.imageloader";

    private boolean mPaused;

    /**
     * Gets the {@link ImageLoader} from a {@link Context}.
     *
     * @throws IllegalStateException if the {@link Application} does not have an
     *             {@link ImageLoader}.
     * @see #IMAGE_LOADER_SERVICE
     */
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

    /**
     * Creates an {@link ImageLoader}.
     *
     * @param streamFactory a {@link URLStreamHandlerFactory} for creating
     *            connections to special URLs such as {@code content://} URIs.
     *            This parameter can be {@code null} if the {@link ImageLoader}
     *            only needs to load images over HTTP or if a custom
     *            {@link URLStreamHandlerFactory} has already been passed to
     *            {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)}
     * @param bitmapHandler a {@link ContentHandler} for loading images.
     *            {@link ContentHandler#getContent(URLConnection)} must either
     *            return a {@link Bitmap} or throw an {@link IOException}. This
     *            parameter can be {@code null} to use the default
     *            {@link BitmapContentHandler}.
     * @param prefetchHandler a {@link ContentHandler} for caching a remote URL
     *            as a file, without parsing it or loading it into memory.
     *            {@link ContentHandler#getContent(URLConnection)} should always
     *            return {@code null}. If the URL passed to the
     *            {@link ContentHandler} is already local (for example,
     *            {@code file://}), this {@link ContentHandler} should do
     *            nothing. The {@link ContentHandler} can be {@code null} if
     *            pre-fetching is not required.
     * @param handler a {@link Handler} identifying the callback thread, or
     *            {@code} null for the main thread.
     * @throws NullPointerException if the factory is {@code null}.
     */
    public ImageLoader(URLStreamHandlerFactory streamFactory, ContentHandler bitmapHandler,
            ContentHandler prefetchHandler, Handler handler) {
        mURLStreamHandlerFactory = streamFactory;
        mStreamHandlers = streamFactory != null ? new HashMap<String, URLStreamHandler>() : null;
        mBitmapContentHandler = bitmapHandler != null ? bitmapHandler : new BitmapContentHandler();
        mPrefetchContentHandler = prefetchHandler;
        mResultHandler = handler != null ? handler : new Handler(Looper.getMainLooper());
        mImageViewBinding = new WeakHashMap<ImageView, String>();

        mTaskHandlers = new Handler[WORKER_COUNT];
    }

    /**
     * Creates a basic {@link ImageLoader} with support for HTTP URLs and
     * in-memory caching.
     * <p>
     * Persistent caching and content:// URIs are not supported when this
     * constructor is used.
     */
    public ImageLoader() {
        this(null, null, null, null);
    }

    /**
     * Creates an {@link ImageLoader} with support for pre-fetching.
     *
     * @param bitmapHandler a {@link ContentHandler} that reads, caches, and
     *            returns a {@link Bitmap}.
     * @param prefetchHandler a {@link ContentHandler} for caching a remote URL
     *            as a file, without parsing it or loading it into memory.
     *            {@link ContentHandler#getContent(URLConnection)} should always
     *            return {@code null}. If the URL passed to the
     *            {@link ContentHandler} is already local (for example,
     *            {@code file://}), this {@link ContentHandler} should return
     *            {@code null} immediately.
     */
    public ImageLoader(ContentHandler bitmapHandler, ContentHandler prefetchHandler) {
        this(null, bitmapHandler, prefetchHandler, null);
    }

    /**
     * Creates an {@link ImageLoader} with support for http:// and content://
     * URIs.
     * <p>
     * Prefetching is not supported when this constructor is used.
     *
     * @param resolver a {@link ContentResolver} for accessing content:// URIs.
     */
    public ImageLoader(ContentResolver resolver) {
        this(new ContentURLStreamHandlerFactory(resolver), null, null, null);
    }

    /**
     * Creates an {@link ImageLoader} with a custom
     * {@link URLStreamHandlerFactory}.
     * <p>
     * Use this constructor when loading images with protocols other than
     * {@code http://} and when a custom {@link URLStreamHandlerFactory} has not
     * already been installed with
     * {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)}. If the
     * only additional protocol support required is for {@code content://} URIs,
     * consider using {@link #BitmapLoader(ContentResolver)}.
     * <p>
     * Prefetching is not supported when this constructor is used.
     */
    public ImageLoader(URLStreamHandlerFactory factory) {
        this(factory, null, null, null);
    }


    public Bitmap getBitmap(String uri, boolean loadIfNecessary, BitmapCallback callback) {
        Bitmap memoryBmp = getBitmap(uri);
        if (getBitmap(uri) != null){
            if (callback != null){
                callback.onImageLoaded(memoryBmp, uri);
            }
            return memoryBmp;
        } else if (loadIfNecessary){
            ImageTask task = new ImageTask(uri, callback);
            postTask(task);
        }
        return null;
    }

    public void pause(){
        mPaused = true;
    }

    public void unpause(){
        Log.i("asdf","Unpausing");
        mPaused = false;
        for (ImageTask pendingTask : mPendingTasks){
            postResult(pendingTask);
        }
        mPendingTasks.clear();
    }



    @SuppressWarnings({ "rawtypes" })
    private static final Class[] TYPE_BITMAP = {
        Bitmap.class
    };

    public interface BitmapCallback {
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
        void onImageLoaded(Bitmap mBitmap, String uri);

        /**
         * Notifies an observer that an image could not be loaded.
         *
         * @param url the URL that could not be loaded.
         * @param error the exception that was thrown.
         */
        void onImageError(String uri, Throwable error);
    }

    /**
     * Callback interface for load and error events.
     * <p>
     * This interface is only applicable when binding a stand-alone
     * {@link ImageView}. When the target {@link ImageView} is in an
     * {@link AdapterView},
     * {@link ImageLoader#bind(BaseAdapter, ImageView, String)} will be called
     * implicitly by {@link BaseAdapter#notifyDataSetChanged()}.
     */
    public interface ImageViewCallback {
        /**
         * Notifies an observer that an image was loaded.
         * <p>
         * The bitmap will be assigned to the {@link ImageView} automatically.
         * <p>
         * Use this callback to dismiss any loading indicators.
         *
         * @param view the {@link ImageView} that was loaded.
         * @param url the URL that was loaded.
         */
        void onImageLoaded(ImageView view, String url);

        /**
         * Notifies an observer that an image could not be loaded.
         *
         * @param view the {@link ImageView} that could not be loaded.
         * @param url the URL that could not be loaded.
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
        ERROR,
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

    private final ContentHandler mBitmapContentHandler;

    private final ContentHandler mPrefetchContentHandler;

    private final URLStreamHandlerFactory mURLStreamHandlerFactory;

    private final HashMap<String, URLStreamHandler> mStreamHandlers;

    /**
     * The number of worker threads for loading images.
     */
    private static final int WORKER_COUNT = 3;

    /**
     * Tracks the last URL that was bound to an {@link ImageView}.
     * <p>
     * This ensures that the right image is shown in the case where a new URL is
     * assigned to an {@link ImageView} before the previous asynchronous task
     * completes.
     * <p>
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


    /**
     * Handlers for loading images in the background (one for each worker).
     */
    private final Handler[] mTaskHandlers;

    /**
     * Handler for processing task results (executed on UI thread).
     */
    private final Handler mResultHandler;

    /**
     * Opens a {@link URLConnection}, disabling {@code http.keepAlive} on
     * platforms where the feature does not work reliably.
     */
    private URLConnection openConnection(URL url) throws IOException {
        if (Build.VERSION.SDK_INT < GINGERBREAD) {
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

    private void postTaskAtFrontOfQueue(ImageTask task) {
        String url = task.getUrl();
        int what = getWhat(url);
        Handler handler = getTaskHandler(url);
        Message msg = handler.obtainMessage(what, task);
        handler.sendMessageAtFrontOfQueue(msg);
    }

    private void postResult(ImageTask task) {
        mResultHandler.post(task);
    }

    /**
     * Binds a URL to an {@link ImageView} within an {@link android.widget.AdapterView}.
     *
     * @param adapter the adapter for the {@link android.widget.AdapterView}.
     * @param view the {@link ImageView}.
     * @param url the image URL.
     * @return a {@link BindResult}.
     * @throws NullPointerException if any of the arguments are {@code null}.
     */
    public BindResult bind(BaseAdapter adapter, ImageView view, String url) {
        if (adapter == null) {
            throw new NullPointerException();
        }
        if (view == null) {
            throw new NullPointerException();
        }
        if (url == null) {
            throw new NullPointerException();
        }
        Bitmap bitmap = getBitmap(url);
        Throwable error = getError(url);
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            return BindResult.OK;
        } else {
            // Clear the ImageView by default.
            // The caller can set their own placeholder
            // based on the return value.
            view.setImageDrawable(null);

            if (error != null) {
                return BindResult.ERROR;
            } else {
                ImageTask task = new ImageTask(adapter, url);

                // For adapters, post the latest requests
                // at the front of the queue in case the user
                // has already scrolled past most of the images
                // that are currently in the queue.
                postTaskAtFrontOfQueue(task);
                return BindResult.LOADING;
            }
        }
    }

    /**
     * Binds an image at the given URL to an {@link ImageView}.
     * <p>
     * If the image needs to be loaded asynchronously, it will be assigned at a
     * later time, replacing any existing {@link Drawable} unless
     * {@link #unbind(ImageView)} is called or
     * {@link #bind(ImageView, String, Callback)} is called with the same
     * {@link ImageView}, but a different URL.
     * <p>
     * Use {@link #bind(BaseAdapter, ImageView, String)} instead of this method
     * when the {@link ImageView} is in an {@link android.widget.AdapterView} so that the image
     * will be bound correctly in the case where it has been assigned to a
     * different position since the asynchronous request was started.
     *
     * @param view the {@link ImageView} to bind.
     * @param url the image URL.s
     * @param callback invoked when there is an error loading the image. This
     *            parameter can be {@code null} if a callback is not required.
     * @return a {@link BindResult}.
     * @throws NullPointerException if a required argument is {@code null}
     */
    public BindResult bind(ImageView view, String url, ImageViewCallback callback) {
        if (view == null) {
            throw new NullPointerException();
        }
        if (url == null) {
            throw new NullPointerException();
        }
        mImageViewBinding.put(view, url);
        Bitmap bitmap = getBitmap(url);
        Throwable error = getError(url);
        if (bitmap != null) {
            view.setImageBitmap(bitmap);
            return BindResult.OK;
        } else {
            // Clear the ImageView by default.
            // The caller can set their own placeholder
            // based on the return value.
            view.setImageDrawable(null);

            if (error != null) {
                return BindResult.ERROR;
            } else {
                ImageTask task = new ImageTask(view, url, callback);
                postTask(task);
                return BindResult.LOADING;
            }
        }
    }

    /**
     * Cancels an asynchronous request to bind an image URL to an
     * {@link ImageView} and clears the {@link ImageView}.
     *
     * @see #bind(ImageView, String, Callback)
     */
    public void unbind(ImageView view) {
        mImageViewBinding.remove(view);

        // Clear the ImageView by default.
        // The caller can set their own placeholder
        // based on the return value.
        view.setImageDrawable(null);
    }

    /**
     * Clears any cached errors.
     * <p>
     * Call this method when a network connection is restored, or the user
     * invokes a manual refresh of the screen.
     */
    public void clearErrors() {
        SoundCloudApplication.bitmapErrors.clear();
    }

    /**
     * Pre-loads an image into memory.
     * <p>
     * The image may be unloaded if memory is low. Use {@link #prefetch(String)}
     * and a file-based cache to pre-load more images.
     *
     * @param url the image URL
     * @throws NullPointerException if the URL is {@code null}
     */
    public void preload(String url) {
        if (url == null) {
            throw new NullPointerException();
        }
        if (null != getBitmap(url)) {
            // The image is already loaded
            return;
        }
        if (null != getError(url)) {
            // A recent attempt to load the image failed,
            // therefore this attempt is likely to fail as well.
            return;
        }
        boolean loadBitmap = true;
        ImageTask task = new ImageTask(url, loadBitmap);
        postTask(task);
    }

    /**
     * Pre-loads a range of images into memory from a {@link Cursor}.
     * <p>
     * Typically, an {@link Activity} would register a {@link DataSetObserver}
     * and an {@link android.widget.AdapterView.OnItemSelectedListener}, then
     * call this method to prime the in-memory cache with images adjacent to the
     * current selection whenever the selection or data changes.
     * <p>
     * Any invalid positions in the specified range will be silently ignored.
     *
     * @param cursor a {@link Cursor} containing the image URLs.
     * @param columnIndex the column index of the image URL. The column value
     *            may be {@code NULL}.
     * @param start the first position to load. For example, {@code
     *            selectedPosition - 5}.
     * @param end the first position not to load. For example, {@code
     *            selectedPosition + 5}.
     * @see #preload(String)
     */
    public void preload(Cursor cursor, int columnIndex, int start, int end) {
        for (int position = start; position < end; position++) {
            if (cursor.moveToPosition(position)) {
                String url = cursor.getString(columnIndex);
                if (!TextUtils.isEmpty(url)) {
                    preload(url);
                }
            }
        }
    }

    /**
     * Pre-fetches the binary content for an image and stores it in a file-based
     * cache (if it is not already cached locally) without loading the image
     * data into memory.
     * <p>
     * Pre-fetching should not be used unless a {@link ContentHandler} with
     * support for persistent caching was passed to the constructor.
     *
     * @param url the URL to pre-fetch.
     * @throws NullPointerException if the URL is {@code null}
     */
    public void prefetch(String url) {
        if (url == null) {
            throw new NullPointerException();
        }
        if (null != getBitmap(url)) {
            // The image is already loaded, therefore
            // it does not need to be prefetched.
            return;
        }
        if (null != getError(url)) {
            // A recent attempt to load or prefetch the image failed,
            // therefore this attempt is likely to fail as well.
            return;
        }
        boolean loadBitmap = false;
        ImageTask task = new ImageTask(url, loadBitmap);
        postTask(task);
    }

    /**
     * Pre-fetches the binary content for images referenced by a {@link Cursor},
     * without loading the image data into memory.
     * <p>
     * Pre-fetching should not be used unless a {@link ContentHandler} with
     * support for persistent caching was passed to the constructor.
     * <p>
     * Typically, an {@link Activity} would register a {@link DataSetObserver}
     * and call this method from {@link DataSetObserver#onChanged()} to load
     * off-screen images into a file-based cache when they are not already
     * present in the cache.
     *
     * @param cursor the {@link Cursor} containing the image URLs.
     * @param columnIndex the column index of the image URL. The column value
     *            may be {@code NULL}.
     * @see #prefetch(String)
     */
    public void prefetch(Cursor cursor, int columnIndex) {
        for (int position = 0; cursor.moveToPosition(position); position++) {
            String url = cursor.getString(columnIndex);
            if (!TextUtils.isEmpty(url)) {
                prefetch(url);
            }
        }
    }

    private void putBitmap(String url, Bitmap bitmap) {
        SoundCloudApplication.bitmaps.put(url, new SoftReference<Bitmap>(bitmap));
    }

    private void putError(String url, Throwable error) {
        SoundCloudApplication.bitmapErrors.put(url, error);
    }

    private boolean hasError(String url) {
        return SoundCloudApplication.bitmapErrors.containsKey(url);
    }

    private Bitmap getBitmap(String url) {
        SoftReference<Bitmap> reference = SoundCloudApplication.bitmaps.get(url);
        return reference != null ? reference.get() : null;
    }

    private Throwable getError(String url) {
        return SoundCloudApplication.bitmapErrors.get(url);
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

    public void cancelLoading(String uri){
        int what = getWhat(uri);
        for (Handler handler : mTaskHandlers) {
            if (handler != null && handler.hasMessages(what)) {
                handler.removeMessages(what);
                break;
            }
        }
    }

    private class ImageTask implements Runnable {

        /**
         * A {@link WeakReference} to the {@link BaseAdapter} to be bound or
         * {@code null}.
         * <p>
         * Using a {@link WeakReference} allows the heavy-weight
         * {@link Activity}/{@link Context} object associated with the adapter
         * to be freed before the tasks completes.
         */
        private final WeakReference<BaseAdapter> mAdapterReference;

        /**
         * A {@link WeakReference} to the {@link ImageView} to be bound or
         * {@code null}.
         * <p>
         * Using a {@link WeakReference} allows the heavy-weight
         * {@link Activity}/{@link Context} object associated with the
         * {@link ImageView} to be freed before the tasks completes.
         */
        private final WeakReference<ImageView> mImageViewReference;

        private final BitmapCallback mBitmapCallback;

        private final ImageViewCallback mImageViewCallback;

        private final String mUri;

        private Bitmap mBitmap;

        private Throwable mError;

        private final boolean mLoadBitmap;

        private ImageTask(String uri, BitmapCallback callback) {
            mUri = uri;
            mBitmapCallback = callback;
            mImageViewCallback = null;
            mImageViewReference = null;
            mAdapterReference = null;
            mLoadBitmap = true;
        }

        private ImageTask(BaseAdapter adapter, ImageView view, String url, ImageViewCallback callback,
                boolean loadBitmap) {
            mAdapterReference = adapter != null ? new WeakReference<BaseAdapter>(adapter) : null;
            mImageViewReference = view != null ? new WeakReference<ImageView>(view) : null;
            mUri = url;
            mImageViewCallback = callback;
            mBitmapCallback = null;
            mLoadBitmap = loadBitmap;
        }
        /**
         * Creates an {@link ImageTask} to load a {@link Bitmap} for an
         * {@link ImageView} in an {@link android.widget.AdapterView}.
         */
        public ImageTask(BaseAdapter adapter, String url) {
            this(adapter, null, url, null, true);
        }

        /**
         * Creates an {@link ImageTask} to load a {@link Bitmap} for an
         * {@link ImageView}.
         */
        public ImageTask(ImageView view, String url, ImageViewCallback callback) {
            this(null, view, url, callback, true);
        }

        /**
         * Creates an {@link ImageTask} to prime the cache.
         */
        public ImageTask(String url, boolean loadBitmap) {
            this(null, null, url, null, loadBitmap);
        }

        /**
         * Returns the URL parameter passed to the constructor.
         */
        public String getUrl() {
            return mUri;
        }

        /**
         * Executes the {@link ImageTask}.
         *
         * @return {@code true} if the result for this {@link ImageTask} should
         *         be posted, {@code false} otherwise.
         */
        public boolean execute() {
            try {

                if (mAdapterReference != null) {
                    // The task is binding to an Adapter
                    if (null == mAdapterReference.get()) {
                        // There are no more strong references to the target
                        // adapter, therefore there is no reason to continue.
                        return false;
                    }
                }

                if (mImageViewReference != null) {
                    // The task is binding to an ImageView
                    if (null == mImageViewReference.get()) {
                        // There are no more strong references to the target
                        // view, therefore there is no reason to continue.
                        return false;
                    }
                }

                // Check if the mUri attempt to load the URL had an error
                mError = getError(mUri);
                if (mError != null) {
                    return true;
                }

                // Check if the Bitmap is already cached in memory
                mBitmap = getBitmap(mUri);
                if (mBitmap != null) {
                    // Keep a hard reference until the view has been notified.
                    return true;
                }

                URL url = new URL(null, mUri);
                URLConnection connection = openConnection(url);
                try {
                    if (mLoadBitmap) {
                        mBitmap = (Bitmap) mBitmapContentHandler.getContent(connection, TYPE_BITMAP);
                        if (mBitmap == null) {
                            throw new NullPointerException();
                        }
                        return true;
                    } else {
                        if (mPrefetchContentHandler != null) {
                            // Cache the URL without loading a Bitmap into memory.
                            mPrefetchContentHandler.getContent(connection);
                        }
                        mBitmap = null;
                        return false;
                    }
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
                putBitmap(mUri, mBitmap);
            } else if (mError != null && !hasError(mUri)) {
                putError(mUri, mError);
            }

            if (mAdapterReference != null) {
                BaseAdapter adapter = mAdapterReference.get();
                if (adapter != null && !adapter.isEmpty()) {
                    // The original ImageView may have been reassigned
                    // to a different row, so just re-bind all of the
                    // visible rows instead.
                    adapter.notifyDataSetChanged();
                } else {
                    // The adapter is empty or no longer in use.
                    // It is important that BaseAdapter#notifyDataSetChanged()
                    // is not called when the adapter is empty because this
                    // may indicate that the data is valid when it is not.
                    // For example: when the adapter cursor is deactivated.
                }
            } else if (mImageViewReference != null) {
                ImageView view = mImageViewReference.get();
                if (view != null) {
                    String binding = mImageViewBinding.get(view);
                    if (!TextUtils.equals(binding, mUri)) {
                        // The ImageView has been unbound or bound to a
                        // different URL since the task was started.
                        return;
                    }
                    Context context = view.getContext();
                    if (context instanceof Activity) {
                        Activity activity = (Activity) context;
                        if (activity.isFinishing()) {
                            return;
                        }
                    }
                    if (mBitmap != null) {
                        view.setImageBitmap(mBitmap);
                        if (mImageViewCallback != null) {
                            mImageViewCallback.onImageLoaded(view, mUri);
                        }
                    } else if (mError != null) {
                        if (mImageViewCallback != null) {
                            mImageViewCallback.onImageError(view, mUri, mError);
                        }
                    }
                } else {
                    // The ImageView is no longer in use.
                }
            } else {
                if (mBitmapCallback != null) {
                    if (mBitmap != null){
                        mBitmapCallback.onImageLoaded(mBitmap, mUri);
                    } else if (mError != null) {
                        mBitmapCallback.onImageError(mUri, mError);
                    }
                }
            }

        }
    }

    private ArrayList<ImageTask> mPendingTasks = new ArrayList<ImageTask>();

    private class TaskHandler extends Handler {
        public TaskHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ImageTask task = (ImageTask) msg.obj;
            if (task.execute()) {

                if (mPaused){
                    mPendingTasks.add(task);
                } else {
                    postResult(task);
                }
            } else {
                // No result or the result is no longer needed.
            }
        }
    }


}

