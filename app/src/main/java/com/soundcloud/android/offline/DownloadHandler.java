package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.offline.commands.StoreCompletedDownloadCommand;
import com.soundcloud.propeller.PropellerWriteException;

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

    interface Listener {
        void onSuccess(DownloadResult result);
        void onError(DownloadRequest request);
    }

    public DownloadHandler(Looper looper, MainHandler mainHandler, DownloadOperations downloadOperations, StoreCompletedDownloadCommand storeCompletedDownload) {
        super(looper);
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.storeCompletedDownload = storeCompletedDownload;
    }

    @VisibleForTesting
    DownloadHandler(MainHandler mainHandler, DownloadOperations downloadOperations, StoreCompletedDownloadCommand storeCompletedDownload) {
        this.mainHandler = mainHandler;
        this.downloadOperations = downloadOperations;
        this.storeCompletedDownload = storeCompletedDownload;
    }

    @Override
    public void handleMessage(Message msg) {
        final DownloadRequest request = (DownloadRequest) msg.obj;
        try {
            final DownloadResult result = downloadOperations.download(request);
            storeCompletedDownload.with(result).call();
            sendMessage(MainHandler.ACTION_DOWNLOAD_SUCCESS, result);
        } catch (DownloadFailedException | PropellerWriteException e) {
            sendMessage(MainHandler.ACTION_DOWNLOAD_FAILED, request);
        }
    }

    private void sendMessage(int status, Object obj) {
        mainHandler.sendMessage(mainHandler.obtainMessage(status, obj));
    }

    void quit() {
        getLooper().quit();
        mainHandler.quit();
    }

    @VisibleForTesting
    static class Builder {
        private final DownloadOperations downloadOperations;
        private final StoreCompletedDownloadCommand storeCompletedDownload;

        @Inject
        Builder(DownloadOperations downloadOperations, StoreCompletedDownloadCommand storeCompletedDownload) {
            this.downloadOperations = downloadOperations;
            this.storeCompletedDownload = storeCompletedDownload;
        }

        DownloadHandler create(Listener listener) {
            return new DownloadHandler(createLooper(), new MainHandler(listener), downloadOperations, storeCompletedDownload);
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
                if (ACTION_DOWNLOAD_SUCCESS == msg.what) {
                    listener.onSuccess((DownloadResult) msg.obj);
                } else if (ACTION_DOWNLOAD_FAILED == msg.what) {
                    listener.onError((DownloadRequest) msg.obj);
                }
            }
        }

        public void quit() {
            listenerRef.clear();
        }
    }
}

