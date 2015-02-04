package com.soundcloud.android.offline;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.offline.commands.StoreCompletedDownloadCommand;
import com.soundcloud.propeller.PropellerWriteException;

import android.os.*;

import javax.inject.Inject;

public class DownloadHandler extends Handler {

    interface Listener {
        void onSuccess(DownloadResult result);

        void onError(DownloadRequest request);
    }

    public static final int ACTION_DOWNLOAD = 0;

    private MainHandler mainHandler;
    private final DownloadOperations downloadOperations;
    private final StoreCompletedDownloadCommand storeCompletedDownload;

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

            final Message message = mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_SUCCESS, result);
            mainHandler.sendMessage(message);

        } catch (DownloadFailedException | PropellerWriteException e) {
            final Message message = mainHandler.obtainMessage(MainHandler.ACTION_DOWNLOAD_FAILED, request);
            mainHandler.sendMessage(message);
        }
    }

    void quit() {
        getLooper().quit();
    }

    @VisibleForTesting
    static class Builder {
        private DownloadOperations downloadOperations;
        private StoreCompletedDownloadCommand storeCompletedDownload;

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

        private final Listener listener;

        public MainHandler(Listener listener) {
            super(Looper.getMainLooper());
            this.listener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (ACTION_DOWNLOAD_SUCCESS == msg.what) {
                listener.onSuccess((DownloadResult) msg.obj);
            } else if (ACTION_DOWNLOAD_FAILED == msg.what) {
                listener.onError((DownloadRequest) msg.obj);
            }
        }
    }
}

