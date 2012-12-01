package com.soundcloud.android.view.tour;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.view.Display;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import static java.lang.Math.max;

public class TourLayout extends FrameLayout {

    private ImageView mBgImageView;

    public TourLayout(Context context, int layoutResId, int bgResId) {
        super(context);
        View.inflate(context, layoutResId, this);

        mBgImageView = (ImageView) findViewById(R.id.tour_background_image);

        Point size = getDisplaySize();
        final Bitmap bitmap = ImageUtils.decodeSampledBitmapFromResource(
            getResources(),
                bgResId,
            size.x,
            size.y
        );

        mBgImageView.setImageBitmap(bitmap);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int lastHeight = -1, lastWidth = -1;

            @Override
            public void onGlobalLayout() {
                if (lastWidth == getWidth() && lastHeight == getHeight()) {
                    return;
                }

                lastHeight = getHeight();
                lastWidth  = getWidth();

                Point size = getDisplaySize();

                float heightRatio = (float)size.y / (float)bitmap.getHeight();
                float widthRatio  = (float)size.x / (float)bitmap.getWidth();
                float ratio = max(heightRatio, widthRatio);

                Matrix matrix = new Matrix();
                matrix.setScale(ratio, ratio);
                matrix.postTranslate(
                    (size.x - ratio * bitmap.getWidth()  ) / 2,
                    (size.y - ratio * bitmap.getHeight() ) / 2
                );

                mBgImageView.setImageMatrix(matrix);
            }
        });
    }

    private Point getDisplaySize() {
        WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        return new Point(display.getWidth(), display.getHeight());
    }

    public CharSequence getMessage() {
        TextView text = (TextView) findViewById(R.id.txt_message);
        return text.getText();
    }

    public void recycle() {
        ImageUtils.recycleImageViewBitmap(mBgImageView);
    }
}
