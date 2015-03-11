package com.soundcloud.android.offline;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.offline.commands.StoreCompletedDownloadCommand;
import com.soundcloud.android.offline.commands.UpdateContentAsUnavailableCommand;
import com.soundcloud.propeller.PropellerWriteException;
import org.jetbrains.annotations.Nullable;

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
    private final StoreCompletedDownloadCommand storeCompletedDownload;
    private final UpdateContentAsUnavailableCommand  updateContentAsUnavailable;
    private DownloadRequest current;

    public boolean isDownloading() {
        return current != null;
    }

    @Nullable
    public DownloadRequest getCurrent() {
        return current;
    }

    interface Listener {
        void onSuccess(DownloadResult result);
        void onError(DownloadResult request);
    }

    public DownloadHandler(Looper looper, MainHandler mainHandler, DownloadOperations downloadOperations, StoreCompletedDownloadCommand storeCompletedDownload, UpdateContentAsUnavailableCommand updateContentAsUnavailable) {
        super(looper);
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.storeCompletedDownload = storeCompletedDownload;
        this.updateContentAsUnavailable = updateContentAsUnavailable;
    }

    @VisibleForTesting
    DownloadHandler(MainHandler mainHandler, DownloadOperations downloadOperations, StoreCompletedDownloadCommand storeCompletedDownload, UpdateContentAsUnavailableCommand updateContentAsUnavailable) {
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.storeCompletedDownload = storeCompletedDownload;
        this.updateContentAsUnavailable = updateContentAsUnavailable;
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
                fireAndForget(updateContentAsUnavailable.call(result.getUrn()));
            }
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_FAILED, result);
        }
    }

    private void tryToStoreDownloadSuccess(DownloadResult result) {
        try {
            storeCompletedDownload.with(result).call();
            sendDownloadResult(MainHandler.ACTION_DOWNLOAD_SUCCESS, result);
        } catch (PropellerWriteException e) {
            downloadOperations.deleteTrack(result.getUrn());
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
        private final StoreCompletedDownloadCommand storeCompletedDownload;
        private final UpdateContentAsUnavailableCommand updateContentAsUnavailable;

        @Inject
        Builder(DownloadOperations downloadOperations, StoreCompletedDownloadCommand storeCompletedDownload, UpdateContentAsUnavailableCommand updateContentAsUnavailable) {
            this.downloadOperations = downloadOperations;
            this.storeCompletedDownload = storeCompletedDownload;
            this.updateContentAsUnavailable = updateContentAsUnavailable;
        }

        DownloadHandler create(Listener listener) {
            return new DownloadHandler(createLooper(), new MainHandler(listener), downloadOperations, storeCompletedDownload, updateContentAsUnavailable);
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

