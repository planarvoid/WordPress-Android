package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.LeaveBehind;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

import javax.inject.Inject;

public class LeaveBehindController implements View.OnClickListener{

    private final View trackView;
    private final ImageOperations imageOperations;
    private final Context context;

    private View leaveBehind;
    private ImageView adImage;

    private final ImageListener imageListener = new ImageListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {}

        @Override
        public void onLoadingFailed(String imageUri, View view, String failedReason) {}

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            show();
        }
    };

    private LeaveBehindController(View trackView, ImageOperations imageOperations, Context context) {
        this.trackView = trackView;
        this.imageOperations = imageOperations;
        this.context = context;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.leave_behind_close:
                dismiss();
                break;
            case R.id.leave_behind_image:
                String imageLink = (String) view.getTag();
                startActivity(Uri.parse(imageLink));
                dismiss();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID: "
                        + view.getContext().getResources().getResourceName(view.getId()));
        }
    }

    private void startActivity(Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void setup(LeaveBehind data) {
        leaveBehind = getLeaveBehind();

        adImage = (ImageView) leaveBehind.findViewById(R.id.leave_behind_image);
        adImage.setTag(data.getLinkUrl());
        imageOperations.displayLeaveBehind(Uri.parse(data.getImageUrl()), adImage, imageListener);

        adImage.setOnClickListener(this);
        leaveBehind.findViewById(R.id.leave_behind_close).setOnClickListener(this);
    }

    private void show() {
        leaveBehind.setVisibility(View.VISIBLE);
    }

    public void dismiss() {
        leaveBehind.setVisibility(View.GONE);
        clear();
    }

    private void clear() {
        adImage.setImageDrawable(null);
        adImage.setTag(null);
    }

    private View getLeaveBehind() {
        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        if (leaveBehind == null) {
            ViewStub stub = (ViewStub) trackView.findViewById(R.id.leave_behind_stub);
            leaveBehind = stub.inflate();
        }
        return leaveBehind;
    }

    static class Factory {
        private final ImageOperations imageOperations;
        private final Context context;

        @Inject
        Factory(ImageOperations imageOperations, Context context) {
            this.imageOperations = imageOperations;
            this.context = context;
        }

        LeaveBehindController create(View trackView) {
            return new LeaveBehindController(trackView, imageOperations, context);
        }
    }

}
