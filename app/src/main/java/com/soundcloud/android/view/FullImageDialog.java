package com.soundcloud.android.view;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.java.optional.Optional;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import javax.inject.Inject;

public class FullImageDialog extends DialogFragment {

    private static final String TAG = "FullImage";
    private static final String KEY_URN = "urn";
    private static final String KEY_IMAGE_URL_TEMPLATE = "imageUrlTemplate";

    @Inject Context context;
    @Inject ImageOperations imageOperations;

    @BindView(R.id.image) ImageView image;
    @BindView(R.id.progress) ProgressBar progress;
    private Unbinder unbinder;

    public static void show(FragmentManager fragmentManager, ImageResource imageResource) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_URN, imageResource.getUrn());
        args.putString(KEY_IMAGE_URL_TEMPLATE, imageResource.getImageUrlTemplate().orNull());
        DialogFragment dialog = new FullImageDialog();
        dialog.setArguments(args);
        dialog.show(fragmentManager, TAG);
    }

    public FullImageDialog() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        setupWindow(dialog);
        setupLayout(dialog);
        displayImage(getImageResource(getArguments()));
        return dialog;
    }

    private ImageResource getImageResource(Bundle bundle) {
        return SimpleImageResource.create(bundle.getParcelable(KEY_URN),
                                          Optional.fromNullable(bundle.getString(KEY_IMAGE_URL_TEMPLATE)));
    }

    @SuppressLint("InflateParams")
    private void setupLayout(Dialog dialog) {
        View layout = LayoutInflater.from(getActivity()).inflate(R.layout.full_image_dialog, null);
        unbinder = ButterKnife.bind(this, layout);
        dialog.setContentView(layout);
    }

    @OnClick(R.id.full_image)
    void dismissOnClick() {
        dismiss();
    }

    private void setupWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.requestFeature(Window.FEATURE_NO_TITLE);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    private void displayImage(ImageResource imageResource) {
        imageOperations.displayInFullDialogView(imageResource, ApiImageSize.T500, image, new ImageListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadingFailed(String s, View view, String failedReason) {
                if (isAdded()) {
                    handleLoadingError();
                }
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                if (isAdded()) {
                    if (loadedImage == null) {
                        handleLoadingError();
                    } else {
                        image.setVisibility(View.VISIBLE);
                        progress.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }

    private void handleLoadingError() {
        AndroidUtils.showToast(context, R.string.image_load_error);
        dismiss();
    }

}
