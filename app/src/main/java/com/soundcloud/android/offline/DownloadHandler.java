package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.model.Urn;
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
    private final SecureFileStorage secureFileStorage;
    private final TrackDownloadsStorage offlineTracksStorage;

    private DownloadRequest current;

    public boolean isDownloading() {
        return current != null;
    }

    public boolean isCurrentRequest(DownloadRequest request) {
        return current != null && current.equals(request);
    }

    public Urn getCurrentTrack() {
        return current != null ? current.track : Urn.NOT_SET;
    }

    interface Listener {
        void onSuccess(DownloadResult result);

        void onCancel(DownloadResult result);

        void onError(DownloadResult request);
    }

    public DownloadHandler(Looper looper, MainHandler mainHandler,
                           DownloadOperations downloadOperations,
                           SecureFileStorage secureFileStorage, TrackDownloadsStorage offlineTracksStorage) {
        super(looper);
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.secureFileStorage = secureFileStorage;
        this.offlineTracksStorage = offlineTracksStorage;
    }

    @VisibleForTesting
    DownloadHandler(MainHandler mainHandler,
                    DownloadOperations downloadOperations,
                    SecureFileStorage secureFileStorage, TrackDownloadsStorage offlineTracksStorage) {
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.secureFileStorage = secureFileStorage;
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
        } else if (result.isCancelled()) {
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_CANCEL, result);
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
            secureFileStorage.deleteTrack(result.getTrack());
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_FAILED, result);
        }
    }

    private void sendDownloadResult(int status, DownloadResult result) {
        mainHandler.sendMessage(mainHandler.obtainMessage(status, result));
    }

    void cancel() {
        downloadOperations.cancelCurrentDownload();
    }

    void quit() {
        getLooper().quit();
        mainHandler.quit();
    }

    @VisibleForTesting
    static class Builder {
        private final DownloadOperations downloadOperations;
        private final TrackDownloadsStorage tracksStorage;
        private final SecureFileStorage secureFileStorage;

        @Inject
        Builder(DownloadOperations operations, TrackDownloadsStorage tracksStorage, SecureFileStorage secureFiles) {
            this.downloadOperations = operations;
            this.tracksStorage = tracksStorage;
            this.secureFileStorage = secureFiles;
        }

        DownloadHandler create(Listener listener) {
            return new DownloadHandler(
                    createLooper(), new MainHandler(listener), downloadOperations, secureFileStorage, tracksStorage);
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
        static final int ACTION_DOWNLOAD_CANCEL = 2;

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
                switch (msg.what) {
                    case ACTION_DOWNLOAD_SUCCESS:
                        listener.onSuccess(result);
                        break;
                    case ACTION_DOWNLOAD_CANCEL:
                        listener.onCancel(result);
                        break;
                    case ACTION_DOWNLOAD_FAILED:
                        listener.onError(result);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown action received by DownloadHandler: " + msg.what);
                }
            }
        }

        public void quit() {
            listenerRef.clear();
        }
    }
}

