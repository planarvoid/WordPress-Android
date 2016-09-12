package com.soundcloud.android.upgrade;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

class GoOnboardingAdapter extends PagerAdapter {

    private static final int BG_SAMPLE_SIZE = 2;

    private Context context;
    private DeviceHelper deviceHelper;

    @Inject
    public GoOnboardingAdapter(Context context, DeviceHelper deviceHelper) {
        this.context = context;
        this.deviceHelper = deviceHelper;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        switch (position) {
            case 0:
                return bindView(container, R.layout.go_onboarding_title_page, OnboardingPage.WELCOME);
            case 1:
                return bindView(container, R.layout.go_onboarding_page, OnboardingPage.FULL_TRACKS);
            case 2:
                return bindView(container, R.layout.go_onboarding_page, OnboardingPage.OFFLINE);
            case 3:
                return bindView(container, R.layout.go_onboarding_page, OnboardingPage.GO);
            default:
                throw new IllegalStateException("Unexpected index in " + GoOnboardingAdapter.class.getSimpleName());
        }
    }

    private ViewGroup bindView(ViewGroup parent, @LayoutRes int layout, OnboardingPage page) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(layout, parent, false);
        bindBasicViews(page, view);
        bindBackground(page, view);
        parent.addView(view);
        return view;
    }

    private void bindBackground(final OnboardingPage page, final ViewGroup view) {
        Observable.fromCallable(new Func0<Bitmap>() {
            @Override
            public Bitmap call() {
                BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
                if (deviceHelper.isLowMemoryDevice() || shouldResampleBackground(page)) {
                    // Scale background image to half source size
                    decodeOpts.inSampleSize = BG_SAMPLE_SIZE;
                }
                return BitmapFactory.decodeResource(context.getResources(), page.background, decodeOpts);
            }
        })
        .subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new BackgroundSubscriber((ImageView) view.findViewById(R.id.go_onboarding_background)));
    }

    private BitmapFactory.Options measureBackground(OnboardingPage page) {
        BitmapFactory.Options measureOpts = new BitmapFactory.Options();
        measureOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), page.background, measureOpts);
        return measureOpts;
    }

    private boolean shouldResampleBackground(OnboardingPage page) {
        final BitmapFactory.Options options = measureBackground(page);
        int fullWidth = options.outWidth;
        int resampleWidth = options.outWidth / BG_SAMPLE_SIZE;
        int screenWidth = deviceHelper.getDisplayMetrics().widthPixels;
        // Resample if screen width is closer to sampled width than source width
        return Math.abs(fullWidth - screenWidth) > Math.abs(resampleWidth - screenWidth);
    }

    private void bindBasicViews(OnboardingPage page, ViewGroup view) {
        ImageView image = ButterKnife.findById(view, R.id.tooltip_image);
        image.setImageResource(page.tooltip);
        TextView title = ButterKnife.findById(view, R.id.go_onboarding_title);
        title.setText(page.title);
        TextView body = ButterKnife.findById(view, R.id.go_onboarding_body);
        body.setText(page.body);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ImageView imgView = ButterKnife.findById((View) object, R.id.go_onboarding_background);
        BitmapDrawable drawable = (BitmapDrawable) imgView.getDrawable();
        if (drawable != null && drawable.getBitmap() != null) {
            drawable.getBitmap().recycle();
        }
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return OnboardingPage.values().length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private class BackgroundSubscriber extends DefaultSubscriber<Bitmap> {

        private final WeakReference<ImageView> viewRef;

        public BackgroundSubscriber(ImageView view) {
            this.viewRef = new WeakReference<>(view);
        }

        @Override
        public void onNext(Bitmap bitmap) {
            ImageView background = viewRef.get();
            if (background != null && bitmap != null) {
                background.setImageBitmap(bitmap);
            }
        }

        @Override
        public void onError(Throwable e) {
            Log.e(getClass().getSimpleName(), "Failed to decode background: " + e.getMessage());
            super.onError(e); // Log remotely
        }
    }

    enum OnboardingPage {

        WELCOME(R.drawable.go_onboarding_1, R.drawable.conversion_cloud,  R.string.go_onboarding_title_1, R.string.go_onboarding_body_1),
        FULL_TRACKS(R.drawable.go_onboarding_2, R.drawable.go_onboarding_go, R.string.go_onboarding_title_2, R.string.go_onboarding_body_2),
        OFFLINE(R.drawable.go_onboarding_3, R.drawable.go_onboarding_offline, R.string.go_onboarding_title_3, R.string.go_onboarding_body_3),
        GO(R.drawable.go_onboarding_4, R.drawable.go_onboarding_heart, R.string.go_onboarding_title_4, R.string.go_onboarding_body_4);

        final int background;
        final int tooltip;
        final int title;
        final int body;

        OnboardingPage(@DrawableRes int background, @DrawableRes int tooltip, @StringRes int title, @StringRes int body) {
            this.background = background;
            this.tooltip = tooltip;
            this.title = title;
            this.body = body;
        }
    }

}
