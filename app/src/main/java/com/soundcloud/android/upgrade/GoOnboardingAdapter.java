package com.soundcloud.android.upgrade;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
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

class GoOnboardingAdapter extends PagerAdapter {

    private Context context;

    @Inject
    public GoOnboardingAdapter(Context context) {
        this.context = context;
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
                return BitmapFactory.decodeResource(context.getResources(), page.background);
            }
        })
        .subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new DefaultSubscriber<Bitmap>() {
            @Override
            public void onNext(Bitmap bitmap) {
                ImageView background = ButterKnife.findById(view, R.id.go_onboarding_background);
                if (background != null) {
                    background.setImageBitmap(bitmap);
                }
            }
        });
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
        return 4;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
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
