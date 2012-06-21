package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

public class ListLoadingItem extends RelativeLayout {

    View mLoadingItem;
    Animation mRotateAnimation;

    public ListLoadingItem(Context context) {
        super(context);
        View.inflate(context, R.layout.list_loading_item, this);

        mLoadingItem = findViewById(R.id.list_loading);
        mRotateAnimation = AnimationUtils.loadAnimation(context, R.anim.progress_rotate);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mLoadingItem.startAnimation(mRotateAnimation);
    }
}
