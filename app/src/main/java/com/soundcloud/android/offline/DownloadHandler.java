package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.propeller.WriteResult;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class DownloadHandler extends Handler {

    public static final int ACTION_DOWNLOAD = 0;

    private final MainHandler mainHandler;
    private final DownloadOperations downloadOperations;
    private final OfflineTracksStorage offlineTracksStorage;
    private DownloadRequest current;

    public boolean isDownloading() {
        return current != null;
    }

    public boolean isCurrentRequest(DownloadRequest request) {
        return current != null && request.equals(current);
    }

    interface Listener {
        void onSuccess(DownloadResult result);

        void onError(DownloadResult request);
    }

    public DownloadHandler(Looper looper, MainHandler mainHandler,
                           DownloadOperations downloadOperations,
                           OfflineTracksStorage offlineTracksStorage) {
        super(looper);
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.offlineTracksStorage = offlineTracksStorage;
    }

    @VisibleForTesting
    DownloadHandler(MainHandler mainHandler,
                    DownloadOperations downloadOperations,
                    OfflineTracksStorage offlineTracksStorage) {
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.offlineTracksStorage = offlineTracksStorage;
    }

    @Override
    public void handleMessage(Message msg) {
        final DownloadRequest request = (DownloadRequest) msg.obj;
        current = request;
        final DownloadResult result = downloadOperations.download(request);
        current = null;

        if (result.isSuccess()) {
            tryToStoreDownloadSuccess(result);
        } else {
            if (result.isUnavailable()) {
                offlineTracksStorage.markTrackAsUnavailable(result.getTrack());
            }
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_FAILED, result);
        }
    }

    private void tryToStoreDownloadSuccess(DownloadResult result) {
        final WriteResult writeResult = offlineTracksStorage.storeCompletedDownload(result);
        if (writeResult.success()) {
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_SUCCESS, result);
        } else {
            downloadOperations.deleteTrack(result.getTrack());
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_FAILED, result);
        }
    }

    private void sendDownloadResult(int status, DownloadResult result) {
        mainHandler.sendMessage(mainHandler.obtainMessage(status, result));
    }

    void quit() {
        getLooper().quit();
        mainHandler.quit();
    }

    @VisibleForTesting
    static class Builder {
        private final DownloadOperations downloadOperations;
        private final OfflineTracksStorage tracksStorage;

        @Inject
        Builder(DownloadOperations downloadOperations, OfflineTracksStorage tracksStorage) {
            this.downloadOperations = downloadOperations;
            this.tracksStorage = tracksStorage;
        }

        DownloadHandler create(Listener listener) {
            return new DownloadHandler(createLooper(), new MainHandler(listener), downloadOperations, tracksStorage);
        }

        private Looper createLooper() {
            HandlerThread thread = new HandlerThread("DownloadThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            return thread.getLooper();
        }
    }

    @VisibleForTesting
    static class MainHandler extends Handler {
        static final int ACTION_DOWNLOAD_SUCCESS = 0;
        static final int ACTION_DOWNLOAD_FAILED = 1;

        private final WeakReference<Listener> listenerRef;

        public MainHandler(Listener listenerRef) {
            super(Looper.getMainLooper());
            this.listenerRef = new WeakReference<>(listenerRef);
        }

        @Override
        public void handleMessage(Message msg) {
            final Listener listener = listenerRef.get();
            if (listener != null) {
                final DownloadResult result = (DownloadResult) msg.obj;
                if (ACTION_DOWNLOAD_SUCCESS == msg.what) {
                    listener.onSuccess(result);
                } else if (ACTION_DOWNLOAD_FAILED == msg.what) {
                    listener.onError(result);
                }
            }
        }

        public void quit() {
            listenerRef.clear();
        }
    }
}

