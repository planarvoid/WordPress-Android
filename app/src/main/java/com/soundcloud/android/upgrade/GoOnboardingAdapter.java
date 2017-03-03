package com.soundcloud.android.upgrade;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.Plan;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

class GoOnboardingAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {

    private WeakHashMap<Integer, AnimationDrawable> pendingAnimationMap = new WeakHashMap<>(OnboardingPage.values().length);
    private List<OnboardingPage> pages;

    @Inject
    GoOnboardingAdapter() {}

    void configureContent(Plan plan) {
        if (plan == Plan.MID_TIER) {
            pages = Arrays.asList(OnboardingPage.WELCOME_GO, OnboardingPage.OFFLINE, OnboardingPage.NO_ADS);
        } else {
            pages = Arrays.asList(OnboardingPage.WELCOME_GO_PLUS, OnboardingPage.FULL_TRACKS, OnboardingPage.OFFLINE, OnboardingPage.NO_ADS, OnboardingPage.START);
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        return bindView(position, container, pages.get(position));
    }

    private ViewGroup bindView(int position, ViewGroup parent, OnboardingPage page) {
        ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(page.layout, parent, false);
        bindBasicViews(position, page, view);
        parent.addView(view);
        return view;
    }

    private void bindBasicViews(int position, OnboardingPage page, ViewGroup view) {
        ImageView image = ButterKnife.findById(view, R.id.tooltip_image);
        image.setImageResource(page.tooltip);
        final Drawable drawable = image.getDrawable();
        if (drawable instanceof AnimationDrawable) {
            pendingAnimationMap.put(position, (AnimationDrawable) drawable);
        }
        TextView title = ButterKnife.findById(view, R.id.go_onboarding_title);
        title.setText(page.title);
        TextView body = ButterKnife.findById(view, R.id.go_onboarding_body);
        body.setText(page.body);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {
        if (pendingAnimationMap.containsKey(position)) {
            pendingAnimationMap.get(position).start();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

    private enum OnboardingPage {
        WELCOME_GO_PLUS(R.layout.go_onboarding_landing, R.drawable.go_onboarding_cloud, R.string.go_onboarding_landing_title, R.string.go_onboarding_landing_body),
        WELCOME_GO(R.layout.go_onboarding_landing, R.drawable.go_onboarding_cloud, R.string.go_onboarding_landing_title_mid, R.string.go_onboarding_landing_body),
        FULL_TRACKS(R.layout.go_onboarding_page, R.drawable.go_onboarding_preview, R.string.go_onboarding_full_tracks_title, R.string.go_onboarding_full_tracks_body),
        NO_ADS(R.layout.go_onboarding_page, R.drawable.go_onboarding_no_ads, R.string.go_onboarding_no_ads_title, R.string.go_onboarding_no_ads_body),
        OFFLINE(R.layout.go_onboarding_page, R.drawable.go_onboarding_offline, R.string.go_onboarding_offline_title, R.string.go_onboarding_offline_body),
        START(R.layout.go_onboarding_page, R.drawable.go_onboarding_heartburst, R.string.go_onboarding_start_title, R.string.go_onboarding_start_body);

        final int layout;
        final int tooltip;
        final int title;
        final int body;

        OnboardingPage(@LayoutRes int layout, @DrawableRes int tooltip, @StringRes int title, @StringRes int body) {
            this.layout = layout;
            this.tooltip = tooltip;
            this.title = title;
            this.body = body;
        }
    }

}
