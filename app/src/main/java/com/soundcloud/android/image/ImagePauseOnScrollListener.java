package com.soundcloud.android.image;

import android.support.v7.widget.RecyclerView;

import javax.inject.Inject;

/**
 * https://github.com/nostra13/Android-Universal-Image-Loader/issues/799
 */
public class ImagePauseOnScrollListener extends RecyclerView.OnScrollListener {

    private final ImageOperations imageOperations;

    @Inject
    public ImagePauseOnScrollListener(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        switch (newState) {
            case RecyclerView.SCROLL_STATE_IDLE:
                imageOperations.resume();
                break;
            case RecyclerView.SCROLL_STATE_SETTLING:
                imageOperations.pause();
                break;
            default:
                // no-op
                break;
        }
    }

    public void resume() {
        imageOperations.resume();
    }
}

