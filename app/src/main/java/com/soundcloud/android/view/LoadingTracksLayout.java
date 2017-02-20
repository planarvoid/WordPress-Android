package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadingTracksLayout extends LinearLayout {

    private static int[] LOADING_DRAWABLES = new int[]{
            R.drawable.skeleton_track_cell_frame_1,
            R.drawable.skeleton_track_cell_frame_2,
            R.drawable.skeleton_track_cell_frame_3,
            R.drawable.skeleton_track_cell_frame_4,
            R.drawable.skeleton_track_cell_frame_5,
            R.drawable.skeleton_track_cell_frame_6,
            R.drawable.skeleton_track_cell_frame_7,
            R.drawable.skeleton_track_cell_frame_8,
            R.drawable.skeleton_track_cell_frame_9,
            R.drawable.skeleton_track_cell_frame_10,
            R.drawable.skeleton_track_cell_frame_9,
            R.drawable.skeleton_track_cell_frame_8,
            R.drawable.skeleton_track_cell_frame_7,
            R.drawable.skeleton_track_cell_frame_6,
            R.drawable.skeleton_track_cell_frame_5,
            R.drawable.skeleton_track_cell_frame_4,
            R.drawable.skeleton_track_cell_frame_3,
            R.drawable.skeleton_track_cell_frame_2,
            R.drawable.skeleton_track_cell_frame_1
    };

    List<ImageView> imageViewList = Collections.emptyList();
    private boolean animating;

    public LoadingTracksLayout(Context context) {
        super(context);
        init();
    }

    public LoadingTracksLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LoadingTracksLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // this is because some of our lists have a natural gray BG. If this changes, we can remove this
        setBackgroundColor(Color.WHITE);
        setOrientation(LinearLayout.VERTICAL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        populate();
    }

    private void populate() {
        removeAllViews();

        Context context = getContext();
        int startIndex = 0;
        int currentBottom = 0;
        boolean filled = false;
        imageViewList = new ArrayList<>();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        do {
            ImageView imageView = (ImageView) layoutInflater.inflate(R.layout.emptyview_tracks_loading_items, this, false);
            imageView.setImageDrawable(createAnimationDrawable(context, startIndex));
            imageView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

            addView(imageView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageViewList.add(imageView);

            startIndex = bumpStartIndexForNextLoader(startIndex);
            currentBottom += imageView.getMeasuredHeight();

            if (currentBottom < getMeasuredHeight()) {
                layoutInflater.inflate(R.layout.item_divider, this, true);
            } else {
                filled = true;
            }

        } while (!filled);

        post(this::requestLayout);

        if (animating) {
            start();
        }
    }

    private int bumpStartIndexForNextLoader(int startIndex) {
        startIndex -= 3;
        if (startIndex < 0) {
            startIndex += LOADING_DRAWABLES.length;
        }
        return startIndex;
    }

    @NonNull
    private AnimationDrawable createAnimationDrawable(Context context, int startIndex) {
        AnimationDrawable drawable = new AnimationDrawable();
        for (int j = 0; j < LOADING_DRAWABLES.length; j++) {
            drawable.addFrame(ContextCompat.getDrawable(context, LOADING_DRAWABLES[(startIndex + j) % LOADING_DRAWABLES.length]), 60);
        }
        return drawable;
    }

    public void start() {
        animating = true;
        for (ImageView imageView : imageViewList) {
            ((AnimationDrawable) (imageView).getDrawable()).start();
        }
    }
}
