package com.soundcloud.android.view.tour;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class TourLayout extends FrameLayout {

    private int mBgResId;
    private ImageView mBgImageView;

    public TourLayout(Context context, int layoutResId, int bgResId) {
        super(context);
        View.inflate(context, layoutResId, this);
        mBgResId = bgResId;
        mBgImageView = (ImageView) findViewById(R.id.tour_background_image);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && mBgImageView != null) {
            mBgImageView.setImageBitmap(
                    ImageUtils.decodeSampledBitmapFromResource(getResources(), mBgResId, getWidth(), getHeight()));
        }
    }

    public CharSequence getMessage() {
        TextView text = (TextView) findViewById(R.id.txt_message);
        return text.getText();
    }

    public int getBgResId() {
        return mBgResId;
    }

}
