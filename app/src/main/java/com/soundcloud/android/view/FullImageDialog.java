package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.utils.AndroidUtils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

public class FullImageDialog extends Dialog {
    private WeakReference<Activity> mActivityRef;
    private Handler mHandler = new Handler();

    private ImageOperations mImageOperations;

    public FullImageDialog(Activity context, final String imageUri, ImageOperations imageOperations) {
        super(context, R.style.Theme_FullImageDialog);

        mImageOperations = imageOperations;

        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.full_image_dialog);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mActivityRef = new WeakReference<Activity>(context);
        final ImageView image = (ImageView) this.findViewById(R.id.image);
        final ProgressBar progress = (ProgressBar) this.findViewById(R.id.progress);
        mImageOperations.displayInFullDialogView(imageUri, image, new ImageListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                if (isShowing()) {
                    progress.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onLoadingFailed(String s, View view, String failedReason) {
                if (isShowing()) {
                    mHandler.post(mImageError);
                }
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (isShowing()) {
                    progress.setVisibility(View.GONE);
                }
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
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            cancel();
            return true;
        } else {
            return false;
        }
    }
}
