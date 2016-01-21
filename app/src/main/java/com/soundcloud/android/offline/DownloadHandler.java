package com.soundcloud.android.offline;

import com.soundcloud.android.model.Urn;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

public class DownloadHandler extends Handler {

    public static final int ACTION_DOWNLOAD = 0;

    private final WeakReference<Listener> listenerRef;
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
        return isDownloading() ? current.getTrack() : Urn.NOT_SET;
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

    public DownloadHandler(Looper looper,
                           Listener listener,
                           DownloadOperations downloadOperations,
                           SecureFileStorage secureFileStorage,
                           TrackDownloadsStorage trackDownloadsStorage) {
        super(looper);
        this.listenerRef = new WeakReference<>(listener);
        this.downloadOperations = downloadOperations;
        this.secureFileStorage = secureFileStorage;
        this.trackDownloadsStorage = trackDownloadsStorage;
    }

    @VisibleForTesting
    DownloadHandler(Listener listener,
                    DownloadOperations downloadOperations,
                    SecureFileStorage secureFileStorage, TrackDownloadsStorage trackDownloadsStorage) {
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
        final DownloadState result = downloadOperations.download(request, createDownloadProgressListener(request));
        if (result.isSuccess()) {
            tryToStoreDownloadSuccess(result);
        } else if (result.isUnavailable()) {
            trackDownloadsStorage.markTrackAsUnavailable(result.getTrack());
        }
        return result;
    }

    private DownloadOperations.DownloadProgressListener createDownloadProgressListener(final DownloadRequest request) {
        return new DownloadOperations.DownloadProgressListener() {
            @Override
            public void onProgress(long downloaded) {
                sendDownloadState(DownloadState.inProgress(request, downloaded));
            }
        };
    }

    private void tryToStoreDownloadSuccess(DownloadState result) {
        if (!trackDownloadsStorage.storeCompletedDownload(result).success()) {
            secureFileStorage.deleteTrack(result.getTrack());
        }
    }

    private void sendDownloadState(DownloadState result) {
        final Listener listener =  listenerRef.get();
        if (listener != null) {
            if (result.isInProgress()) {
                listener.onProgress(result);
            } else if (result.isSuccess()) {
                listener.onSuccess(result);
            } else if (result.isCancelled()) {
                listener.onCancel(result);
            } else {
                listener.onError(result);
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

        @Inject
        Builder(DownloadOperations operations, TrackDownloadsStorage tracksStorage, SecureFileStorage secureFiles) {
            this.downloadOperations = operations;
            this.tracksStorage = tracksStorage;
            this.secureFileStorage = secureFiles;
        }

        DownloadHandler create(Listener listener) {
            return new DownloadHandler(
                    createLooper(), listener, downloadOperations, secureFileStorage, tracksStorage);
        }

        private Looper createLooper() {
            HandlerThread thread = new HandlerThread("DownloadThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            return thread.getLooper();
        }
    }

}

