package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.imageloader.OldImageLoader;
import com.soundcloud.android.imageloader.OldImageLoader.BindResult;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.MotionEventUtils;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

public class FullImageDialog extends Dialog {
    private WeakReference<Activity> mActivityRef;
    private Handler mHandler = new Handler();

    public FullImageDialog(Activity context, final String imageUri) {
        super(context, R.style.Theme_FullImageDialog);

        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.full_image_dialog);

        mActivityRef = new WeakReference<Activity>(context);

        final ImageView image = (ImageView) this.findViewById(R.id.image);
        final ProgressBar progress = (ProgressBar) this.findViewById(R.id.progress);
        BindResult result;
        if ((result = OldImageLoader.get(context).bind(image, imageUri, new OldImageLoader.Callback() {
            @Override
            public void onImageLoaded(ImageView view, String url) {
                if (!isShowing()) return;
                Activity activity = mActivityRef.get();
                if (activity != null && !activity.isFinishing()) {
                    new FullImageDialog(activity, imageUri).show();
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
            Activity activity = mActivityRef.get();
            if (activity != null && !activity.isFinishing()) {
                AndroidUtils.showToast(activity, R.string.image_load_error);
            }
            try {
                dismiss();
            } catch (IllegalArgumentException ignored) {}
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && MotionEventUtils.isOutOfBounds(event, this)) {
            cancel();
            return true;
        } else {
            return false;
        }
    }
}
