
package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;

public abstract class LazyRow extends FrameLayout {
    protected ScActivity mActivity;

    protected LazyBaseAdapter mAdapter;
    protected ImageLoader mImageLoader;
    protected ImageView mIcon;

    protected int mCurrentPosition;

    public LazyRow(ScActivity activity, LazyBaseAdapter adapter) {
        super(activity);
        mActivity = activity;
        mAdapter = adapter;

        if (mActivity != null) mImageLoader = ImageLoader.get(mActivity);

        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(getRowResourceId(), this);
        mIcon = (ImageView) findViewById(R.id.icon);

        if (getContext().getResources().getDisplayMetrics().density > 1) {
            mIcon.getLayoutParams().width  = 67;
            mIcon.getLayoutParams().height = 67;
        }
    }


    protected abstract int getRowResourceId();

    /** update the views with the data corresponding to selection index */
    public void display(int position) {
        mCurrentPosition = position;

        if (TextUtils.isEmpty(getIconRemoteUri())){
            mImageLoader.unbind(getRowIcon());
            mIcon.setImageDrawable(null);
            return;
        }

        if (CloudUtils.checkIconShouldLoad(getIconRemoteUri())) {
            mImageLoader.bind(mAdapter, getRowIcon(), getIconRemoteUri());
        } else {
            mImageLoader.unbind(getRowIcon());
        }
    }

    public ImageView getRowIcon() {
        return null;
    }

    public String getIconRemoteUri() {
        return "";
    }

    public void cleanup() {
        if (mIcon != null) {
            mImageLoader.unbind(mIcon);
        }
    }
}
