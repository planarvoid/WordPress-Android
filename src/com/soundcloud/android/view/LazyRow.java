
package com.soundcloud.android.view;

import android.graphics.Bitmap;
import android.util.Log;
import com.google.android.imageloader.ImageLoader;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.utils.CloudUtils;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.soundcloud.android.utils.ListAlphaAnimation;

public abstract class LazyRow extends FrameLayout {
    protected ScActivity mActivity;

    protected LazyBaseAdapter mAdapter;
    protected ImageLoader mImageLoader;
    protected ImageView mIcon;
    protected String mCurrentImageUri;

    protected int mCurrentPosition;
    protected ImageLoader.Options mIconOptions;

    public LazyRow(ScActivity activity, LazyBaseAdapter adapter) {
        super(activity);
        mActivity = activity;
        mAdapter = adapter;

        if (mIconOptions == null) mIconOptions = new ImageLoader.Options();
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

        final String iconUri = getIconRemoteUri();
        if (TextUtils.isEmpty(iconUri)){
            mImageLoader.unbind(mIcon);
            mIcon.setImageDrawable(null);
            mIcon.setVisibility(View.GONE);
            if (mIcon.getAnimation() != null) mIcon.clearAnimation();
            return;
        }

        if (CloudUtils.checkIconShouldLoad(iconUri)) {
            final Bitmap bmp = mImageLoader.getBitmap(iconUri,null,new ImageLoader.Options(false));
            if (bmp != null){

                 mIcon.setImageBitmap(bmp);

                ListAlphaAnimation anim = (ListAlphaAnimation) mAdapter.getIconAnimation(position);

                if (anim == null){
                    anim = new ListAlphaAnimation();
                    mAdapter.setIconAnimation(position,anim);
                    mIcon.startAnimation(anim);

                } else {
                    long startTime = anim.getStartTime();
                    mIcon.setAnimation(anim);
                    anim.setStartTime(startTime);
                }

            } else {
                if (mIcon.getAnimation() != null) mIcon.clearAnimation();
                mImageLoader.bind(mAdapter, mIcon, iconUri, mIconOptions);
            }
        } else {
            mImageLoader.unbind(mIcon);
            if (mIcon.getAnimation() != null) mIcon.clearAnimation();
            mIcon.setImageDrawable(null);
            mIcon.setVisibility(View.GONE);
        }
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
