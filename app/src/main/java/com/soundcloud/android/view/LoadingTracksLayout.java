package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadingTracksLayout extends LinearLayout {

    private static final int TILE_COUNT = 3; // this may need to be configurable

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

    public LoadingTracksLayout(Context context) {
        super(context);
        init(context);
    }

    public LoadingTracksLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoadingTracksLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // this is because some of our lists have a natural gray BG. If this changes, we can remove this
        setBackgroundColor(Color.WHITE);
        setOrientation(LinearLayout.VERTICAL);

        int startIndex = 0;
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        imageViewList = new ArrayList<>();
        for (int i = 0; i < TILE_COUNT; i++) {

            AnimationDrawable drawable = new AnimationDrawable();
            int totalNumDrawables = LOADING_DRAWABLES.length;
            for (int j = 0; j < totalNumDrawables; j++) {
                drawable.addFrame(ContextCompat.getDrawable(context, LOADING_DRAWABLES[(startIndex + j) % totalNumDrawables]), 60);
            }

            ImageView imageView = (ImageView) layoutInflater.inflate(R.layout.emptyview_tracks_loading_items, this, false);
            imageView.setImageDrawable(drawable);
            addView(imageView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageViewList.add(imageView);

            startIndex += 3;

            if (i < TILE_COUNT - 1) {
                layoutInflater.inflate(R.layout.item_divider, this, true);
            }
        }
    }

    public void start() {
        for (ImageView imageView : imageViewList) {
            ((AnimationDrawable) (imageView).getDrawable()).start();
        }
    }
}
