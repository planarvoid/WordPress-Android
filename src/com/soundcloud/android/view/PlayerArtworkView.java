package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PlayerArtworkView extends FrameLayout {

    private ImageView mArtwork;
    private ImageLoader.BindResult mCurrentArtBindResult;
    private String mCurrentPath;

    public PlayerArtworkView(Context context) {
        super(context);

        setBackgroundDrawable(getResources().getDrawable(R.drawable.artwork_player));
        mArtwork = new ImageView(context);
        mArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mArtwork.setVisibility(View.INVISIBLE);
    }

    public void setImagePath(String path) {
        if (path != mCurrentPath){
            mCurrentPath = path;
            updateArtwork();
        }

    }

    private void onArtworkSet() {
        if (mArtwork.getVisibility() == View.INVISIBLE || mArtwork.getVisibility() == View.GONE) {
            AnimUtils.runFadeInAnimationOn(getContext(), mArtwork);
            mArtwork.setVisibility(View.VISIBLE);
        }

    }

    private void updateArtwork() {

        if (TextUtils.isEmpty(mCurrentPath)) {
            // no artwork
            ImageLoader.get(getContext()).unbind(mArtwork);
            mArtwork.setVisibility(View.INVISIBLE);
        } else {
            // load artwork as necessary
            if ((mCurrentArtBindResult = ImageUtils.loadImageSubstitute(
                    getContext(),
                    mArtwork,
                    mCurrentPath,
                    Consts.GraphicSize.T500, new ImageLoader.ImageViewCallback() {
                @Override
                public void onImageError(ImageView view, String url, Throwable error) {
                    mCurrentArtBindResult = ImageLoader.BindResult.ERROR;
                    Log.e(getClass().getSimpleName(), "Error loading artwork " + error);
                }

                @Override
                public void onImageLoaded(ImageView view, String url) {
                    onArtworkSet();
                }
            }, null)) != ImageLoader.BindResult.OK) {
                mArtwork.setVisibility(View.INVISIBLE);
            } else {
                onArtworkSet();
            }
        }
    }

    public void onDataConnected() {
        if (mCurrentArtBindResult == ImageLoader.BindResult.ERROR) {
            updateArtwork();
        }
    }
}
