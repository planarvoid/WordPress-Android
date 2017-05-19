package com.soundcloud.android.ads;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;

import android.support.annotation.LayoutRes;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AutoFactory(allowSubclasses = true)
class PrestitialAdapter extends PagerAdapter {

    private final AdData adData;
    private final VisualPrestitialPresenter.Listener listener;
    private final VisualPrestitialPresenter visualPrestitialPresenter;

    private List<PrestitialPage> pages;

    PrestitialAdapter(AdData adData,
                      VisualPrestitialPresenter.Listener listener,
                      @Provided VisualPrestitialPresenter visualPrestitialPresenter) {
        this.adData = adData;
        this.listener = listener;
        this.visualPrestitialPresenter = visualPrestitialPresenter;
        configureContent();
    }

    private void configureContent() {
        if (adData instanceof SponsoredSessionAd) {
            pages = Arrays.asList(PrestitialPage.OPT_IN_CARD, PrestitialPage.VIDEO_CARD, PrestitialPage.END_CARD);
        } else {
            pages = Collections.singletonList(PrestitialPage.DISPLAY);
        }
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public final Object instantiateItem(ViewGroup container, int position) {
        return bindView(position, container, pages.get(position));
    }

    private ViewGroup bindView(int position, ViewGroup parent, PrestitialPage page) {
        final ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(page.layout, parent, false);
        switch (page) {
            case DISPLAY :
                visualPrestitialPresenter.setupContentView(view, (VisualPrestitialAd) adData, listener);
                parent.addView(view);
                break;
            default:
                throw new IllegalAccessError("Ad page not supported: " + page + ", pos:" + position);
        }
        return view;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    private enum PrestitialPage {
        OPT_IN_CARD(R.layout.sponsored_session_action_page),
        VIDEO_CARD(R.layout.sponsored_session_video_page),
        END_CARD(R.layout.sponsored_session_action_page),
        DISPLAY(R.layout.visual_prestitial);

        final int layout;

        PrestitialPage(@LayoutRes int layout) {
            this.layout = layout;
        }
    }
}
