package com.soundcloud.android.view;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.image.ImageProcessor;
import com.soundcloud.android.image.ImageProcessorCompat;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import javax.inject.Inject;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class GlassLinearLayout extends LinearLayout implements ViewTreeObserver.OnDrawListener {

    private static final boolean JELLY_BEAN_MR1_OR_HIGHER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;

    private static final float BLUR_SCALE = 0.25f;
    private static final float RENDER_HORIZONTAL_SCALE = 0.125f;
    private static final float RENDER_VERTICAL_SCALE = 0.5f;
    private static final float DEFAULT_BLUR_RADIUS = 20f;
    private static final int MINIMUM_DELAY_MS = 16; // 0=as fast as it cans, 16=target 60 fps max, 32 = 30 fps max...
    private static final int STRIDE_SIZE = 4;

    private Bitmap backgroundBitmap;
    private Bitmap blurBitmap;
    private Paint paint = new Paint();
    private float blurRadius;
    private int source;
    private long lastUpdate = System.currentTimeMillis() - MINIMUM_DELAY_MS;
    private boolean updating;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject ImageProcessor imageProcessor;

    private PublishSubject<Bitmap> subject;

    private BackgroundUpdater backgroundUpdater = new BackgroundUpdater();

    private Func1<Bitmap, Bitmap> blurBackground = new Func1<Bitmap, Bitmap>() {
        @Override
        public Bitmap call(Bitmap backgroundBitmap) {
            Bitmap scaledBitmap = scaleBitmap(backgroundBitmap, blurWidth, blurHeightExtra);
            imageProcessor.blurBitmap(scaledBitmap, blurBitmap, Optional.of(blurRadius));
            scaledBitmap.recycle();
            return Bitmap.createBitmap(blurBitmap, 0, 0, blurWidth, blurHeight);
        }
    };

    private int blurWidth;
    private int blurHeight;
    private int blurHeightExtra;

    public GlassLinearLayout(Context context) {
        super(context);
        initGraph();
    }

    public GlassLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GlassLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            initGraph();
            initAttributes(context, attrs);
            subject = PublishSubject.create();
        } else {
            setBackgroundColor(ContextCompat.getColor(getContext(), R.color.translucent_dark_background));
        }
    }

    private void initGraph() {
        if (isInEditMode()) {
            this.imageProcessor = new ImageProcessorCompat();
        } else {
            SoundCloudApplication.getObjectGraph().inject(this);
        }
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.GlassLinearLayout);
        blurRadius = array.getFloat(R.styleable.GlassLinearLayout_blurRadius, DEFAULT_BLUR_RADIUS);
        source = array.getResourceId(R.styleable.GlassLinearLayout_source, Consts.NOT_SET);
        array.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (JELLY_BEAN_MR1_OR_HIGHER) {
            updating = false;
            lastUpdate = System.currentTimeMillis() - MINIMUM_DELAY_MS;
            subscription = subject.map(blurBackground)
                                  .subscribeOn(Schedulers.computation())
                                  .observeOn(AndroidSchedulers.mainThread())
                                  .subscribe(backgroundUpdater);
            getViewTreeObserver().addOnDrawListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (JELLY_BEAN_MR1_OR_HIGHER) {
            getViewTreeObserver().removeOnDrawListener(this);
            subscription.unsubscribe();
            cleanupBitmaps();
        }
        super.onDetachedFromWindow();
    }

    private void cleanupBitmaps() {
        if (!updating) {
            if (blurBitmap != null) {
                blurBitmap.recycle();
                blurBitmap = null;
            }
            if (backgroundBitmap != null) {
                backgroundBitmap.recycle();
                backgroundBitmap = null;
            }
        }
    }

    @Override
    public void onDraw() {
        long current = System.currentTimeMillis();

        if (!updating && current - lastUpdate >= MINIMUM_DELAY_MS) {
            updating = true;
            lastUpdate = current;
            updateBackground();
        }
    }

    private void updateBackground() {
        View body = ((ViewGroup) getParent().getParent()).findViewById(source);

        if (body.getHeight() > 0 && body.getWidth() > 0) {
            final int renderBlurHeight = (int) (blurRadius / BLUR_SCALE * RENDER_VERTICAL_SCALE);
            final int renderWidth = Math.round(getWidth() * RENDER_HORIZONTAL_SCALE);
            final int renderHeight = Math.round(getHeight() * RENDER_VERTICAL_SCALE) + renderBlurHeight;

            blurWidth = strideAligned(Math.round(getWidth() * BLUR_SCALE));
            blurHeight = strideAligned(Math.round(getHeight() * BLUR_SCALE));
            blurHeightExtra = strideAligned(blurHeight + (int) blurRadius);

            backgroundBitmap = initializeBitmap(backgroundBitmap, renderWidth, renderHeight);
            blurBitmap = initializeBitmap(blurBitmap, blurWidth, blurHeightExtra);

            subject.onNext(renderToBitmap(body, backgroundBitmap));
        }
    }

    private int strideAligned(int size) {
        return size - (size % STRIDE_SIZE);
    }

    private Bitmap renderToBitmap(View view, Bitmap bitmap) {
        Canvas canvas = new Canvas(bitmap);
        canvas.scale(RENDER_HORIZONTAL_SCALE, RENDER_VERTICAL_SCALE);
        view.draw(canvas);
        // Draw dark line on top to avoid excessive bloom
        paint.setColor(bitmap.getPixel(0, 0));
        canvas.drawRect(0, 0, canvas.getWidth() / RENDER_HORIZONTAL_SCALE, 2 / RENDER_VERTICAL_SCALE, paint);
        return bitmap;
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private Bitmap initializeBitmap(Bitmap bitmap, int width, int height) {
        if (bitmap == null || width != bitmap.getWidth()) {
            if (bitmap != null) {
                bitmap.recycle();
            }
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        return bitmap;
    }

    private class BackgroundUpdater extends DefaultSubscriber<Bitmap> {
        @Override
        public void onNext(Bitmap bitmap) {
            setBackground(new BitmapDrawable(getResources(), bitmap));
            updating = false;
        }

        @Override
        public void onError(Throwable error) {
            updating = false;
        }
    }
}
