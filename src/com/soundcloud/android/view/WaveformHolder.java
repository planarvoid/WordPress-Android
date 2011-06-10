
package com.soundcloud.android.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;
import com.soundcloud.android.utils.CloudUtils;

public class WaveformHolder extends RelativeLayout {

    private static final String TAG = "WaveformHolder";

    private RelativeLayout mConnectingBar;

    public WaveformHolder(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public void showConnectingLayout() {
        if (mConnectingBar == null) mConnectingBar = (RelativeLayout) findViewById(R.id.connecting_bar);
        if (!CloudUtils.isLandscape(getResources()))setStaticTransformationsEnabled(true);
        mConnectingBar.setVisibility(View.VISIBLE);
    }

    public void hideConnectingLayout() {
        if (mConnectingBar == null) mConnectingBar = (RelativeLayout) findViewById(R.id.connecting_bar);
        setStaticTransformationsEnabled(false);
        mConnectingBar.setVisibility(View.GONE);
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        boolean ret = super.getChildStaticTransformation(child, t);
        if (child == mConnectingBar){
            t.setAlpha((float) 0.5);
            return true;
        }
        return ret;
    }


}
