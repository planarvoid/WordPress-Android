
package com.soundcloud.android.view;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.BindResult;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.utils.AnimUtils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class LazyRow extends RelativeLayout {
    protected ScActivity mActivity;

    protected LazyBaseAdapter mAdapter;

    protected ImageLoader mImageLoader;

    protected ImageView mIcon;

    protected int mCurrentPosition;

    public boolean pendingIcon = false;

    public LazyRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity);
        mActivity = _activity;
        mAdapter = _adapter;

        if (mActivity != null) mImageLoader = ImageLoader.get(mActivity);

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

        if (TextUtils.isEmpty(getIconRemoteUri())){
            pendingIcon = false;
            setTemporaryDrawable(BindResult.ERROR);
            return;
        }

        if (mActivity.getScrollState() == AbsListView.OnScrollListener.SCROLL_STATE_FLING || mActivity.pendingIconsUpdate){
            pendingIcon = true;
            setTemporaryDrawable(BindResult.ERROR);
        } else {
            loadPendingIcon();
        }
    }

    public void loadPendingIcon(){
        pendingIcon = false;
        BindResult result = BindResult.ERROR;
        try { // put the bind in a try catch to catch any loading error (or the
            // occasional bad url)
            if (CloudUtils.checkIconShouldLoad(getIconRemoteUri()))
                result = mImageLoader.bind(mAdapter, getRowIcon(), getIconRemoteUri());
            else
                mImageLoader.unbind(getRowIcon());
        } catch (Exception e) {
            e.printStackTrace();
        }
        setTemporaryDrawable(result);
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
            mIcon.setImageDrawable(mAdapter.getDefaultIcon());


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
