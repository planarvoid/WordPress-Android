package com.soundcloud.android.view;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.MotionEventUtils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
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
        ImageLoader.getInstance().displayImage(imageUri, image, new SimpleImageLoadingListener(){
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                image.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                if (!isShowing()) return;
                mHandler.post(mImageError);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (!isShowing()) return;
                image.setVisibility(View.VISIBLE);
                progress.setVisibility(View.GONE);
            }
        });
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
