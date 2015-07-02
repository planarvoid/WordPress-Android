package com.soundcloud.android.tracks;

import com.soundcloud.android.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

public class LoadingRelatedTracksPresenter {
    private static final int TIME_BEFORE_SHOWING_LOADING = 800;
    private static final int MIN_TIME_ON_SCREEN = 1000;
    private final Handler handler;
    private LoadingRelatedTracksView loadingRelatedTracksView;
    private AlertDialog dialog;
    private long timeWhenShown;

    @SuppressLint("ShowToast")
    @Inject
    LoadingRelatedTracksPresenter() {
        this.handler = new Handler();
    }

    public void show(Context context, final DialogInterface.OnCancelListener onCancelListener) {
        final View content = View.inflate(context, R.layout.dialog_loading_related_tracks, null);
        loadingRelatedTracksView = (LoadingRelatedTracksView) content.findViewById(R.id.loading_tracks_animation);
        dialog = new AlertDialog.Builder(context)
                .setOnCancelListener((new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancelDelayedPresentation();
                        onCancelListener.onCancel(dialog);
                    }
                }))
                .setView(content)
                .create();
        delayPresentation(new ShowDialogRunnable(), TIME_BEFORE_SHOWING_LOADING);
    }

    public void onError(Context context) {
        delayPresentation(new ErrorDialogRunnable(context), getRemainingTimeOnScreen());
    }

    public void onSuccess() {
        delayPresentation(new DismissDialogRunnable(), getRemainingTimeOnScreen());
    }

    private long getRemainingTimeOnScreen() {
        final long timeSpentOnScreen = System.currentTimeMillis() - timeWhenShown;
        return MIN_TIME_ON_SCREEN - timeSpentOnScreen;
    }

    private void delayPresentation(Runnable runnable, long delayMillis) {
        cancelDelayedPresentation();
        handler.postDelayed(runnable, delayMillis);
    }

    private void cancelDelayedPresentation() {
        handler.removeCallbacksAndMessages(null);
    }

    private class ShowDialogRunnable implements Runnable {
        @Override
        public void run() {
            timeWhenShown = System.currentTimeMillis();
            dialog.show();
            loadingRelatedTracksView.start();
        }
    }

    private class DismissDialogRunnable implements Runnable {
        @Override
        public void run() {
            loadingRelatedTracksView.stop();
            dialog.dismiss();
            dialog = null;
        }
    }

    private class ErrorDialogRunnable implements Runnable {

        private final Context context;

        private ErrorDialogRunnable(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            loadingRelatedTracksView.stop();
            dialog.dismiss();
            dialog = null;
            Toast.makeText(context, R.string.unable_to_play_related_tracks, Toast.LENGTH_SHORT).show();
        }
    }
}
