package com.soundcloud.android.tracks;

import com.soundcloud.android.R;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

public class DelayedLoadingDialogPresenter {
    private static final int TIME_BEFORE_SHOWING_LOADING = 800;
    private static final int MIN_TIME_ON_SCREEN = 1000;
    private final Handler handler;
    private LoadingAnimationView loadingAnimationView;
    private AlertDialog dialog;
    private long timeWhenShown;
    private final String loadingMessage;
    private final String onErrorToastText;
    private final DialogInterface.OnCancelListener onCancelListener;

    @SuppressLint("ShowToast")
    DelayedLoadingDialogPresenter(String loadingMessage, String onErrorToastText, DialogInterface.OnCancelListener onCancelListener) {
        this.loadingMessage = loadingMessage;
        this.onErrorToastText = onErrorToastText;
        this.onCancelListener = onCancelListener;
        this.handler = new Handler();
    }

    public DelayedLoadingDialogPresenter show(Context context) {
        final View content = View.inflate(context, R.layout.dialog_delayed_loading, null);

        loadingAnimationView = (LoadingAnimationView) content.findViewById(R.id.loading_animation);
        ((TextView) content.findViewById(R.id.loading_message)).setText(loadingMessage);

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

        return this;
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
            loadingAnimationView.start();
        }
    }

    private class DismissDialogRunnable implements Runnable {
        @Override
        public void run() {
            loadingAnimationView.stop();
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
            loadingAnimationView.stop();
            dialog.dismiss();
            dialog = null;
            Toast.makeText(context, onErrorToastText, Toast.LENGTH_SHORT).show();
        }
    }

    public static class Builder {
        private String loadingMessage;
        private String onErrorToastText;
        private DialogInterface.OnCancelListener onCancelListener;

        @Inject
        Builder() {
        }

        public DelayedLoadingDialogPresenter create() {
            return new DelayedLoadingDialogPresenter(loadingMessage, onErrorToastText, onCancelListener);
        }

        public Builder setLoadingMessage(String loadingMessage) {
            this.loadingMessage = loadingMessage;
            return this;
        }

        public Builder setOnErrorToastText(String onErrorToastText) {
            this.onErrorToastText = onErrorToastText;
            return this;
        }

        public Builder setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
            this.onCancelListener = onCancelListener;
            return this;
        }
    }
}
