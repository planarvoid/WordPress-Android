package com.soundcloud.android.view;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import rx.Observable;
import rx.subjects.ReplaySubject;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * Indeterminate progress dialog displayed while generating a share link. Can be dismissed by
 * hitting back but not by touching outside the window's bounds. Use {@link #onCancelObservable()} to be notified
 * of cancellation.
 */
public class ShareDialog extends DialogFragment {

    private static final String TAG = "ShareDialog";

    private final ReplaySubject<Void> onCancelObservable = ReplaySubject.createWithSize(1);

    public static ShareDialog show(Context context) {
        ShareDialog dialog = new ShareDialog();
        dialog.show(getFragmentActivity(context).getSupportFragmentManager(), TAG);
        return dialog;
    }

    /**
     * Returns an Observable which will emit a single item when the dialog is canceled, then complete. If the dialog
     * is dismissed but not canceled, the Observable will complete without emitting an item.
     */
    public Observable<Void> onCancelObservable() {
        return onCancelObservable;
    }

    @Override
    public void dismiss() {
        super.dismiss();
        onCancelObservable.onCompleted();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        onCancelObservable.onNext(null);
        onCancelObservable.onCompleted();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        setupWindow(dialog);
        setupLayout(dialog);
        // Don't cancel on touch.
        dialog.setCanceledOnTouchOutside(false);
        // Do cancel on back.
        dialog.setCancelable(true);
        return dialog;
    }

    @Override
    public void onDestroyView() {
        final Dialog dialog = getDialog();

        // Work around bug: http://code.google.com/p/android/issues/detail?id=17423
        if ((dialog != null) && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }

        super.onDestroyView();
    }

    public boolean isShowing() {
        return getDialog() != null && getDialog().isShowing();
    }

    @SuppressLint("InflateParams")
    private void setupLayout(Dialog dialog) {
        View layout = View.inflate(getActivity(), R.layout.share_dialog, null);
        ButterKnife.bind(this, layout);
        dialog.setContentView(layout);
    }

    private void setupWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.requestFeature(Window.FEATURE_NO_TITLE);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
}
