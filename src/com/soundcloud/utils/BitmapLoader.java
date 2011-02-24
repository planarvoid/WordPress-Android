package com.soundcloud.utils;


import com.google.android.imageloader.BitmapContentHandler;
import com.google.android.imageloader.ContentURLStreamHandlerFactory;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.SoundCloudApplication;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.WeakHashMap;

public class BitmapLoader {

    private static final String TAG = "BitmapLoader";

    private static final int SDK = Integer.parseInt(Build.VERSION.SDK);

    private static final int GINGERBREAD = 9;

    public static final String BITMAP_LOADER_SERVICE = "com.soundcloud.utils.bitmaploader";

    /**
     * Gets the {@link ImageLoader} from a {@link Context}.
     *
     * @throws IllegalStateException if the {@link Application} does not have an
     *             {@link ImageLoader}.
     * @see #IMAGE_LOADER_SERVICE
     */
    public static BitmapLoader get(Context context) {
        BitmapLoader loader = (BitmapLoader) context.getSystemService(BITMAP_LOADER_SERVICE);
        if (loader == null) {
            context = context.getApplicationContext();
            loader = (BitmapLoader) context.getSystemService(BITMAP_LOADER_SERVICE);
        }
        if (loader == null) {
            throw new IllegalStateException("ImageLoader not available");
        }
        return loader;
    }

    /**
     * Creates an {@link BitmapLoader}.
     *
     * @param streamFactory a {@link URLStreamHandlerFactory} for creating
     *            connections to special URLs such as {@code content://} URIs.
     *            This parameter can be {@code null} if the {@link BitmapLoader}
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
    public BitmapLoader(URLStreamHandlerFactory streamFactory, ContentHandler bitmapHandler,
            ContentHandler prefetchHandler, Handler handler) {
        mURLStreamHandlerFactory = streamFactory;
        mStreamHandlers = streamFactory != null ? new HashMap<String, URLStreamHandler>() : null;
        mBitmapContentHandler = bitmapHandler != null ? bitmapHandler : new BitmapContentHandler();
        mPrefetchContentHandler = prefetchHandler;
        mResultHandler = handler != null ? handler : new Handler(Looper.getMainLooper());

        mTaskHandlers = new Handler[WORKER_COUNT];
    }

    /**
     * Creates a basic {@link BitmapLoader} with support for HTTP URLs and
     * in-memory caching.
     * <p>
     * Persistent caching and content:// URIs are not supported when this
     * constructor is used.
     */
    public BitmapLoader() {
        this(null, null, null, null);
    }

    /**
     * Creates an {@link BitmapLoader} with support for pre-fetching.
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
    public BitmapLoader(ContentHandler bitmapHandler, ContentHandler prefetchHandler) {
        this(null, bitmapHandler, prefetchHandler, null);
    }

    /**
     * Creates an {@link BitmapLoader} with support for http:// and content://
     * URIs.
     * <p>
     * Prefetching is not supported when this constructor is used.
     *
     * @param resolver a {@link ContentResolver} for accessing content:// URIs.
     */
    public BitmapLoader(ContentResolver resolver) {
        this(new ContentURLStreamHandlerFactory(resolver), null, null, null);
    }

    /**
     * Creates an {@link BitmapLoader} with a custom
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
    public BitmapLoader(URLStreamHandlerFactory factory) {
        this(factory, null, null, null);
    }


    public Bitmap getBitmap(String uri, boolean loadIfNecessary, Callback callback) {
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
        void onImageLoaded(Bitmap mBitmap, String uri);

        /**
         * Notifies an observer that an image could not be loaded.
         *
         * @param url the URL that could not be loaded.
         * @param error the exception that was thrown.
         */
        void onImageError(String uri, Throwable error);
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

        private final Callback mCallback;

        private final String mUri;

        private Bitmap mBitmap;

        private Throwable mError;

        private ImageTask(String uri, Callback callback) {
            mUri = uri;
            mCallback = callback;
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
                if (mCallback != null)
                    mCallback.onImageLoaded(mBitmap, mUri);
            } else if (mError != null && !hasError(mUri)) {
                putError(mUri, mError);
                if (mCallback != null)
                    mCallback.onImageError(mUri,mError);
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

