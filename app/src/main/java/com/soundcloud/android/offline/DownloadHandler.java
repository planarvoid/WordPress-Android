package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.WriteResult;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class DownloadHandler extends Handler {

    public static final int ACTION_DOWNLOAD = 0;

    private final MainHandler mainHandler;
    private final DownloadOperations downloadOperations;
    private final SecureFileStorage secureFileStorage;
    private final TrackDownloadsStorage trackDownloadsStorage;

    private DownloadRequest current;

    public boolean isDownloading() {
        return current != null;
    }

    public boolean isCurrentRequest(DownloadRequest request) {
        return current != null && current.equals(request);
    }

    public Urn getCurrentTrack() {
        return isDownloading() ? current.track : Urn.NOT_SET;
    }

    public DownloadRequest getCurrentRequest() {
        return current;
    }

    interface Listener {
        void onSuccess(DownloadState state);

        void onCancel(DownloadState state);

        void onError(DownloadState state);

        void onProgress(DownloadState state);
    }

    public DownloadHandler(Looper looper, MainHandler mainHandler,
                           DownloadOperations downloadOperations,
                           SecureFileStorage secureFileStorage, TrackDownloadsStorage trackDownloadsStorage) {
        super(looper);
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.secureFileStorage = secureFileStorage;
        this.trackDownloadsStorage = trackDownloadsStorage;
    }

    @VisibleForTesting
    DownloadHandler(MainHandler mainHandler,
                    DownloadOperations downloadOperations,
                    SecureFileStorage secureFileStorage, TrackDownloadsStorage trackDownloadsStorage) {
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.secureFileStorage = secureFileStorage;
        this.trackDownloadsStorage = trackDownloadsStorage;
    }

    @Override
    public void handleMessage(Message msg) {
        final DownloadRequest request = (DownloadRequest) msg.obj;
        current = request;
        sendDownloadResult(MainHandler.ACTION_DOWNLOAD_PROGRESS, DownloadState.inProgress(request, 0));
        final DownloadState result = downloadOperations.download(request, createDownloadProgressListener(request));
        current = null;

        if (result.isSuccess()) {
            tryToStoreDownloadSuccess(result);
        } else if (result.isCancelled()) {
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_CANCEL, result);
        } else {
            if (result.isUnavailable()) {
                trackDownloadsStorage.markTrackAsUnavailable(result.getTrack());
            }
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_FAILED, result);
        }
    }

    private DownloadOperations.DownloadProgressListener createDownloadProgressListener(final DownloadRequest request) {
        return new DownloadOperations.DownloadProgressListener() {
            @Override
            public void onProgress(long downloaded) {
                sendDownloadResult(MainHandler.ACTION_DOWNLOAD_PROGRESS, DownloadState.inProgress(request, downloaded));
            }
        };
    }

    private void tryToStoreDownloadSuccess(DownloadState result) {
        final WriteResult writeResult = trackDownloadsStorage.storeCompletedDownload(result);
        if (writeResult.success()) {
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_SUCCESS, result);
        } else {
            secureFileStorage.deleteTrack(result.getTrack());
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_FAILED, result);
        }
    }

    private void sendDownloadResult(int status, DownloadState state) {
        mainHandler.sendMessage(mainHandler.obtainMessage(status, state));
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
        static final int ACTION_DOWNLOAD_PROGRESS = 3;

        private final WeakReference<Listener> listenerRef;

        public MainHandler(Listener listenerRef) {
            super(Looper.getMainLooper());
            this.listenerRef = new WeakReference<>(listenerRef);
        }

        @Override
        public void handleMessage(Message msg) {
            final Listener listener = listenerRef.get();
            if (listener != null) {
                final DownloadState result = (DownloadState) msg.obj;
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
                    case ACTION_DOWNLOAD_PROGRESS:
                        listener.onProgress(result);
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

