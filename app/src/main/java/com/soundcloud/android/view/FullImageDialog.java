package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
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

    private final WeakReference<Activity> activityRef;
    private final Handler handler = new Handler();

    private final Runnable imageError = new Runnable() {
        public void run() {
            Activity activity = activityRef.get();
            if (activity != null && !activity.isFinishing()) {
                AndroidUtils.showToast(activity, R.string.image_load_error);
            }
            try {
                dismiss();
            } catch (IllegalArgumentException ignored) {
            }
        }
    };

    public FullImageDialog(Activity context, final Urn resourceUrn, ImageOperations imageOperations) {
        super(context, R.style.Theme_FullImageDialog);

        setCancelable(true);
        setCanceledOnTouchOutside(true);
        setContentView(R.layout.full_image_dialog);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        activityRef = new WeakReference<>(context);
        final ImageView image = (ImageView) this.findViewById(R.id.image);
        final ProgressBar progress = (ProgressBar) this.findViewById(R.id.progress);
        imageOperations.displayInFullDialogView(resourceUrn, ApiImageSize.T500, image, new ImageListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                if (isShowing()) {
                    progress.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onLoadingFailed(String s, View view, String failedReason) {
                if (isShowing()) {
                    handler.post(imageError);
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
