package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;

import android.app.Dialog;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

public class FullImageDialog extends Dialog {

    private WeakReference<ScActivity> mActivityRef;
    private Handler mHandler = new Handler();

    public FullImageDialog(ScActivity context, final String imageUri) {
        super(context, R.style.Theme_FullImageDialog);

        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.full_image_dialog);

        mActivityRef = new WeakReference<ScActivity>(context);

        final ImageView image = (ImageView) this.findViewById(R.id.image);
        final ProgressBar progress = (ProgressBar) this.findViewById(R.id.progress);
        BindResult result;
        if ((result = ImageLoader.get(context).bind(image, imageUri, new ImageLoader.ImageViewCallback() {
            @Override
            public void onImageLoaded(ImageView view, String url) {
                if (!isShowing()) return;
                ScActivity activity = mActivityRef.get();
                if (activity != null) {
                    activity.safeShowDialog(new FullImageDialog(activity, imageUri));
                }
                try {
                    dismiss();
                } catch (IllegalArgumentException ignored) {}
            }

            @Override
            public void onImageError(ImageView view, String url, Throwable error) {
                if (!isShowing()) return;
                mHandler.post(mImageError);
            }
        })) != BindResult.OK) {
            if (result == BindResult.ERROR) {
                mHandler.postDelayed(mImageError, 300);
            } else {
                image.setVisibility(View.GONE);
            }
        } else {
            progress.setVisibility(View.GONE);
            image.setVisibility(View.VISIBLE);
        }
    }

    private Runnable mImageError = new Runnable() {
        public void run() {
            ScActivity activity = mActivityRef.get();
            if (activity != null) {
                activity.showToast(R.string.image_load_error);
            }
            try {
                dismiss();
            } catch (IllegalArgumentException ignored) {}
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && isOutOfBounds(event)) {
            cancel();
            return true;
        } else {
            return false;
        }
    }

    private boolean isOutOfBounds(MotionEvent event) {
        final ScActivity activity = mActivityRef.get();
        if (activity != null) {
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            final int slop = ViewConfiguration.get(activity).getScaledWindowTouchSlop();
            final View decorView = getWindow().getDecorView();
            return (x < -slop) ||
                   (y < -slop) ||
                   (x > (decorView.getWidth() + slop)) ||
                   (y > (decorView.getHeight() + slop));
        } else {
            return true;
        }
    }
}
