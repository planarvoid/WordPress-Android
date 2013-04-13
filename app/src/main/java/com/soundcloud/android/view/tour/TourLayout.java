package com.soundcloud.android.view.tour;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static java.lang.Math.max;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ImageUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class TourLayout extends FrameLayout {
    public static final int IMAGE_LOADED = 1;
    public static final int IMAGE_ERROR  = 2 ;

    private final int[] bitmapSize = new int[] { -1, -1 };

    private ImageView mBgImageView;
    private final int mBgResId;
    private Handler mLoadHandler;

    public TourLayout(Context context, int layoutResId, final int bgResId) {
        super(context);
        View.inflate(context, layoutResId, this);

        mBgResId = bgResId;
        mBgImageView = (ImageView) findViewById(R.id.tour_background_image);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int lastHeight = -1, lastWidth = -1;

            @Override
            public void onGlobalLayout() {
                if ((bitmapSize[0] < 0 || bitmapSize[1] < 0) ||
                    (lastWidth == getWidth() && lastHeight == getHeight())) {
                    return;
                }
                final int width  = bitmapSize[0];
                final int height = bitmapSize[1];

                lastHeight = getHeight();
                lastWidth  = getWidth();

                Point size = getDisplaySize();

                float heightRatio = (float)size.y / (float)height;
                float widthRatio  = (float)size.x / (float)width;
                float ratio = max(heightRatio, widthRatio);

                Matrix matrix = new Matrix();
                matrix.setScale(ratio, ratio);
                matrix.postTranslate(
                    (size.x - ratio * width  ) / 2,
                    (size.y - ratio * height ) / 2
                );

                mBgImageView.setImageMatrix(matrix);
            }
        });
    }

    private void onBitmapLoaded(Bitmap bitmap) {
        if (bitmap != null) {
            bitmapSize[0] = bitmap.getWidth();
            bitmapSize[1] = bitmap.getHeight();
            mBgImageView.setImageBitmap(bitmap);
        }
        if (mLoadHandler != null) {
            mLoadHandler.sendEmptyMessage(bitmap == null ? IMAGE_ERROR : IMAGE_LOADED);
        }
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

    public static void load(final Context context, int startPage, TourLayout... layouts) {
        if (layouts == null || layouts.length == 0) throw new IllegalArgumentException();
        loadAsync(context, startPage, layouts);
    }

    private static AsyncTask loadAsync(final Context context, final int startPage, TourLayout... layouts) {
        return new AsyncTask<TourLayout, Pair<TourLayout, Bitmap>, Void>() {
            @Override
            protected Void doInBackground(TourLayout... layouts) {
                final TourLayout initalLayout = layouts[startPage];
                loadTourLayoutImage(initalLayout);
                for (TourLayout layout : layouts) {
                    if (layout != initalLayout) loadTourLayoutImage(layout);
                }
                return null;
            }

            private void loadTourLayoutImage(TourLayout layout) {
                Point size = layout.getDisplaySize();
                Bitmap bitmap = null;
                // try / catch mostly for OOM of the huge images, but who knows really
                try {
                    bitmap = ImageUtils.decodeSampledBitmapFromResource(
                            context.getResources(),
                            layout.mBgResId,
                            size.x,
                            size.y
                    );
                } catch (Error ignored) { // will catch OOM
                    Log.w(TAG, ignored);
                } catch (Exception ignored) {
                    Log.w(TAG, ignored);
                }
                publishProgress(Pair.create(layout, bitmap));
            }

            @Override
            protected void onProgressUpdate(Pair<TourLayout, Bitmap>... result) {
                result[0].first.onBitmapLoaded(result[0].second);
            }
        }.execute(layouts);
    }

    public void setLoadHandler(Handler handler) {
        mLoadHandler = handler;
    }
}
