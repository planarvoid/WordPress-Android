package com.soundcloud.android.upgrade;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.images.BackgroundDecoder;
import com.soundcloud.android.view.LoadingButton;
import com.soundcloud.android.view.pageindicator.CirclePageIndicator;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func0;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.DrawableRes;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.widget.ImageView;

import javax.inject.Inject;
import java.lang.ref.WeakReference;

class GoOnboardingView implements ViewPager.OnPageChangeListener {

    private static final int BUTTON_TRANSITION_MS = 200;

    @BindView(R.id.go_onboarding_background) ImageView background;
    @BindView(R.id.go_onboarding_pager) ViewPager pager;
    @BindView(R.id.go_onboarding_indicator) CirclePageIndicator indicator;
    @BindView(R.id.btn_go_setup_start) LoadingButton startButton;

    private final GoOnboardingAdapter adapter;
    private final BackgroundDecoder backgroundDecoder;

    private Listener listener;
    private Subscription subscription = Subscriptions.empty();

    @DrawableRes
    private int currentBackground;

    interface Listener {
        void onStartClicked();
    }

    @Inject
    GoOnboardingView(GoOnboardingAdapter adapter, BackgroundDecoder backgroundDecoder) {
        this.adapter = adapter;
        this.backgroundDecoder = backgroundDecoder;
    }

    void bind(Activity activity, Listener listener, Plan plan) {
        this.listener = listener;
        ButterKnife.bind(this, activity);
        bindBackground(R.drawable.go_onboarding_landing);
        setupAdapter(plan);
    }

    void unbind() {
        subscription.unsubscribe();
    }

    private void setupAdapter(Plan plan) {
        adapter.configureContent(plan);
        pager.setOffscreenPageLimit(1);
        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
        pager.addOnPageChangeListener(this);
        pager.addOnPageChangeListener(adapter);
    }

    @OnClick(R.id.btn_go_setup_start)
    void onSetupStartClicked() {
        listener.onStartClicked();
    }

    void reset() {
        setEnabled(true);
        startButton.setLoading(false);
    }

    void setStartButtonWaiting() {
        startButton.setEnabled(false);
        startButton.setLoading(true);
    }

    void setStartButtonRetry() {
        setEnabled(true);
        startButton.setRetry();
    }

    private void setEnabled(boolean isEnabled) {
        startButton.setEnabled(isEnabled);
    }

    void showErrorDialog(FragmentManager fragmentManager) {
        UnrecoverableErrorDialog.show(fragmentManager);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {
        configureBackground(position);
        configureStartButton(position);
    }

    private void configureBackground(int position) {
        bindBackground(position == 0
                       ? R.drawable.go_onboarding_landing
                       : R.drawable.go_onboarding_page);
    }

    private void configureStartButton(int position) {
        if (isLastPage(position)) {
            startButton.setBackgroundResource(R.drawable.btn_primary_transition);
            final TransitionDrawable transition = (TransitionDrawable) startButton.getBackground();
            transition.startTransition(BUTTON_TRANSITION_MS);
        } else {
            startButton.setBackgroundResource(R.drawable.btn_transparent);
        }
    }

    private boolean isLastPage(int position) {
        return position == pager.getAdapter().getCount() - 1;
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

    private void bindBackground(@DrawableRes int image) {
        if (currentBackground != image) {
            currentBackground = image;
            subscription = Observable.fromCallable((Func0<Bitmap>) () -> backgroundDecoder.decode(image))
                      .subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER)
                      .observeOn(AndroidSchedulers.mainThread())
                      .subscribe(new BackgroundSubscriber(background));
        }
    }

    private class BackgroundSubscriber extends DefaultSubscriber<Bitmap> {

        private final WeakReference<ImageView> viewRef;

        BackgroundSubscriber(ImageView view) {
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

}
