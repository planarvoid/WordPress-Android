package com.soundcloud.android.offline;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

class DownloadHandler extends Handler {

    static final int ACTION_DOWNLOAD = 0;

    private final WeakReference<Listener> listenerRef;
    private final DownloadOperations downloadOperations;
    private final SecureFileStorage secureFileStorage;
    private final TrackDownloadsStorage trackDownloadsStorage;
    private final OfflinePerformanceTracker performanceTracker;

    private DownloadRequest current;

    boolean isDownloading() {
        return current != null;
    }

    DownloadRequest getCurrentRequest() {
        return current;
    }

    interface Listener {
        void onSuccess(DownloadState state);

        void onCancel(DownloadState state);

        void onError(DownloadState state);

        void onProgress(DownloadState state);
    }

    private DownloadHandler(Looper looper, Listener listener,
                            DownloadOperations downloadOperations,
                            SecureFileStorage secureFileStorage,
                            TrackDownloadsStorage trackDownloadsStorage,
                            OfflinePerformanceTracker performanceTracker) {
        super(looper);
        this.performanceTracker = performanceTracker;
        this.listenerRef = new WeakReference<>(listener);
        this.downloadOperations = downloadOperations;
        this.secureFileStorage = secureFileStorage;
        this.trackDownloadsStorage = trackDownloadsStorage;
    }

    @VisibleForTesting
    DownloadHandler(Listener listener, DownloadOperations downloadOperations,
                    SecureFileStorage secureFileStorage,
                    TrackDownloadsStorage trackDownloadsStorage,
                    OfflinePerformanceTracker performanceTracker) {
        this.performanceTracker = performanceTracker;
        this.listenerRef = new WeakReference<>(listener);
        this.downloadOperations = downloadOperations;
        this.secureFileStorage = secureFileStorage;
        this.trackDownloadsStorage = trackDownloadsStorage;
    }

    @Override
    public void handleMessage(Message msg) {
        final DownloadRequest request = (DownloadRequest) msg.obj;
        current = request;
        sendDownloadState(DownloadState.inProgress(request, 0));
        sendDownloadState(downloadTrack(request));
        current = null;
    }

    private DownloadState downloadTrack(DownloadRequest request) {
        performanceTracker.downloadStarted(request);

        final DownloadState result =
                downloadOperations.download(request, createDownloadProgressListener(request));

        if (result.isSuccess()) {
            return tryToStoreDownloadSuccess(result);
        }

        if (result.isUnavailable()) {
            trackDownloadsStorage.markTrackAsUnavailable(result.getTrack());
        }
        return result;
    }

    private DownloadOperations.DownloadProgressListener createDownloadProgressListener(final DownloadRequest request) {
        return downloaded -> sendDownloadState(DownloadState.inProgress(request, downloaded));
    }

    private DownloadState tryToStoreDownloadSuccess(DownloadState result) {
        if (!trackDownloadsStorage.storeCompletedDownload(result).success()) {
            secureFileStorage.deleteTrack(result.getTrack());
            return DownloadState.error(result.request);
        }
        return result;
    }

    private void sendDownloadState(DownloadState result) {
        final Listener listener = listenerRef.get();
        if (listener != null) {

            if (result.isInProgress()) {
                listener.onProgress(result);

            } else if (result.isSuccess()) {
                listener.onSuccess(result);
                performanceTracker.downloadComplete(result);

            } else if (result.isCancelled()) {
                listener.onCancel(result);
                performanceTracker.downloadCancelled(result);

            } else {
                listener.onError(result);
                performanceTracker.downloadFailed(result);
            }
        }
    }

    void cancel() {
        downloadOperations.cancelCurrentDownload();
    }

    void quit() {
        getLooper().quit();
    }

    @VisibleForTesting
    static class Builder {
        private final DownloadOperations downloadOperations;
        private final TrackDownloadsStorage tracksStorage;
        private final SecureFileStorage secureFileStorage;
        private final OfflinePerformanceTracker performanceTracker;

        @Inject
        Builder(DownloadOperations operations, TrackDownloadsStorage tracksStorage,
                SecureFileStorage secureFiles, OfflinePerformanceTracker performanceTracker) {
            this.downloadOperations = operations;
            this.tracksStorage = tracksStorage;
            this.secureFileStorage = secureFiles;
            this.performanceTracker = performanceTracker;
        }

        DownloadHandler create(Listener listener) {
            return new DownloadHandler(createLooper(), listener, downloadOperations,
                                       secureFileStorage, tracksStorage, performanceTracker);
        }

        private Looper createLooper() {
            HandlerThread thread = new HandlerThread("DownloadThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            return thread.getLooper();
        }
    }
}

