package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.LeaveBehind;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
    private final Resources resources;

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

    private LeaveBehindController(View trackView, ImageOperations imageOperations, Context context, Resources resources) {
        this.trackView = trackView;
        this.imageOperations = imageOperations;
        this.context = context;
        this.resources = resources;
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
        final View leaveBehind = getLeaveBehind();
        leaveBehind.findViewById(R.id.leave_behind_close).setOnClickListener(this);
        leaveBehind.findViewById(R.id.leave_behind_image).setOnClickListener(this);

        ImageView leaveBehindImage = (ImageView) leaveBehind.findViewById(R.id.leave_behind_image);
        leaveBehindImage.setTag(data.getLinkUrl());
        imageOperations.displayLeaveBehind(Uri.parse(data.getImageUrl()), leaveBehindImage, imageListener);
    }

    private void show() {
        final View leaveBehind = trackView.findViewById(R.id.leave_behind);
        leaveBehind.setVisibility(View.VISIBLE);
    }

    public void dismiss() {
        final View leaveBehind = trackView.findViewById(R.id.leave_behind);
        leaveBehind.setVisibility(View.GONE);
        clear(leaveBehind);
    }

    private void clear(View leaveBehind) {
        final ImageView image = (ImageView) leaveBehind.findViewById(R.id.leave_behind_image);
        image.setImageDrawable(null);
        image.setTag(null);
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
        private final Resources resources;

        @Inject
        Factory(ImageOperations imageOperations,
                Context context,
                Resources resources) {
            this.imageOperations = imageOperations;
            this.context = context;
            this.resources = resources;
        }

        LeaveBehindController create(View trackView) {
            return new LeaveBehindController(trackView, imageOperations, context, resources);
        }
    }

}
