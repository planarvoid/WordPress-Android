
package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.utils.AnimUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class LazyRow extends RelativeLayout {
    protected ScActivity mActivity;

    protected LazyBaseAdapter mAdapter;

    protected ImageView mIcon;

    protected int mCurrentPosition;

    public LazyRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity);
        mActivity = _activity;
        mAdapter = _adapter;

        LayoutInflater inflater = (LayoutInflater) mActivity
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(getRowResourceId(), this);
    }

    protected int getRowResourceId() {
        return R.layout.track_list_item;
    }

    /** update the views with the data corresponding to selection index */
    public void display(int position) {

        mCurrentPosition = position;

        Log.i(getClass().getSimpleName(),"DDDISPLAY ");

        if (position == mAdapter.submenuIndex){
            if (findViewById(R.id.row_submenu) != null){
                ((FrameLayout) findViewById(R.id.row_submenu)).setVisibility(View.VISIBLE);
            } else {
                ((ViewStub) findViewById(R.id.stub_submenu)).setVisibility(View.VISIBLE);
                initSubmenu();
            }

            onSubmenu();

            if (position == mAdapter.animateSubmenuIndex){
                mAdapter.animateSubmenuIndex = -1;
                Animation inFromRight = AnimUtils.inFromRightAnimation();
                ((FrameLayout) findViewById(R.id.row_submenu)).startAnimation(inFromRight);
            }



        } else {
            onNoSubmenu();

            if (findViewById(R.id.row_submenu) != null){
                ((FrameLayout) findViewById(R.id.row_submenu)).setVisibility(View.GONE);
            }
        }
    }

    protected void initSubmenu() {
    }

    protected void onSubmenu() {
    }

    protected void onNoSubmenu() {
    }



    public ImageView getRowIcon() {
        return null;
    }

    public String getIconRemoteUri() {
        return "";
    }

    public void setTemporaryDrawable(BindResult result) {
        if (mIcon == null)
            return;

        if (result != BindResult.OK)
            mIcon.setImageDrawable(this.getTemporaryDrawable());

        mIcon.getLayoutParams().width = (int) (getContext().getResources().getDisplayMetrics().density * getIconWidth());
        mIcon.getLayoutParams().height = (int) (getContext().getResources().getDisplayMetrics().density * getIconHeight());
    }

    protected int getIconWidth() {
        return CloudUtils.GRAPHIC_DIMENSIONS_BADGE;
    }

    protected int getIconHeight() {
        return CloudUtils.GRAPHIC_DIMENSIONS_BADGE;
    }

    protected Drawable getTemporaryDrawable() {
        return mActivity.getResources().getDrawable(R.drawable.artwork_badge);
    }

}
