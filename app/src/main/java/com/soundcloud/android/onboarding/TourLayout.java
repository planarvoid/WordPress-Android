package com.soundcloud.android.onboarding;

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
import com.soundcloud.android.R;
import com.soundcloud.android.tasks.ParallelAsyncTask;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.images.ImageUtils;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static java.lang.Math.max;

public class TourLayout extends FrameLayout {
    public static final int IMAGE_LOADED = 1;
    public static final int IMAGE_ERROR = 2;

    private final int[] bitmapSize = new int[]{-1, -1};

    private final ImageView bgImageView;
    private final int bgResId;
    private Handler loadHandler;

    public TourLayout(Context context, int layoutResId, final int bgResId) {
        super(context);
        View.inflate(context, layoutResId, this);

        this.bgResId = bgResId;
        bgImageView = (ImageView) findViewById(R.id.tour_background_image);
        bgImageView.setVisibility(View.GONE);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int lastHeight = -1
                    ,
                    lastWidth = -1;

            @Override
            public void onGlobalLayout() {
                if ((bitmapSize[0] < 0 || bitmapSize[1] < 0) ||
                        (lastWidth == getWidth() && lastHeight == getHeight())) {
                    return;
                }
                final int width = bitmapSize[0];
                final int height = bitmapSize[1];

                lastHeight = getHeight();
                lastWidth = getWidth();

                Point size = getDisplaySize();

                float heightRatio = (float) size.y / (float) height;
                float widthRatio = (float) size.x / (float) width;
                float ratio = max(heightRatio, widthRatio);

                Matrix matrix = new Matrix();
                matrix.setScale(ratio, ratio);
                matrix.postTranslate(
                        (size.x - ratio * width) / 2,
                        (size.y - ratio * height) / 2
                );

                bgImageView.setImageMatrix(matrix);
            }
        });
    }

    private void onBitmapLoaded(Bitmap bitmap) {
        if (bitmap != null) {
            bitmapSize[0] = bitmap.getWidth();
            bitmapSize[1] = bitmap.getHeight();
            bgImageView.setImageBitmap(bitmap);
        }
        AnimUtils.showView(getContext(), bgImageView, true);
        if (loadHandler != null) {
            loadHandler.sendEmptyMessage(bitmap == null ? IMAGE_ERROR : IMAGE_LOADED);
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
        ImageUtils.recycleImageViewBitmap(bgImageView);
    }

    public static void load(final Context context, TourLayout... layouts) {
        if (layouts == null || layouts.length == 0) {
            throw new IllegalArgumentException();
        }
        loadAsync(context, layouts);
    }

    private static AsyncTask loadAsync(final Context context, TourLayout... layouts) {
        return new ParallelAsyncTask<TourLayout, Pair<TourLayout, Bitmap>, Void>() {
            @Override
            protected Void doInBackground(TourLayout... layouts) {
                for (TourLayout layout : layouts) {
                    Point size = layout.getDisplaySize();
                    Bitmap bitmap = null;
                    // try / catch mostly for OOM of the huge images, but who knows really
                    try {
                        bitmap = ImageUtils.decodeSampledBitmapFromResource(
                                context.getResources(),
                                layout.bgResId,
                                size.x,
                                size.y
                        );
                    } catch (Error | Exception ignored) {
                        Log.w(TAG, ignored);
                        ErrorUtils.handleSilentException(ignored);
                    }
                    publishProgress(Pair.create(layout, bitmap));
                }
                return null;
            }

            @Override
            protected final void onProgressUpdate(Pair<TourLayout, Bitmap>... result) {
                result[0].first.onBitmapLoaded(result[0].second);
            }
        }.executeOnThreadPool(layouts);
    }

    public void setLoadHandler(Handler handler) {
        loadHandler = handler;
    }
}
