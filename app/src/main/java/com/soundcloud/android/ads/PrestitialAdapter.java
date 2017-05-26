package com.soundcloud.android.ads;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;

import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

@AutoFactory(allowSubclasses = true)
class PrestitialAdapter extends PagerAdapter {

    private final SponsoredSessionAd adData;
    private final PrestitialView.Listener listener;

    private final SponsoredSessionCardView sponsoredSessionCardView;
    private final SponsoredSessionVideoView sponsoredSessionVideoView;

    private List<PrestitialPage> pages;

    PrestitialAdapter(SponsoredSessionAd adData,
                      PrestitialView.Listener listener,
                      SponsoredSessionVideoView sponsoredSessionVideoView,
                      @Provided SponsoredSessionCardView sponsoredSessionCardView) {
        this.adData = adData;
        this.listener = listener;
        this.sponsoredSessionCardView = sponsoredSessionCardView;
        this.sponsoredSessionVideoView = sponsoredSessionVideoView;
        pages = Arrays.asList(PrestitialPage.OPT_IN_CARD, PrestitialPage.VIDEO_CARD, PrestitialPage.END_CARD);
    }

    PrestitialPage getPage(int position) {
        return pages.get(position);
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public final Object instantiateItem(ViewGroup container, int position) {
        return bindView(position, container, pages.get(position));
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    private ViewGroup bindView(int position, ViewGroup parent, PrestitialPage page) {
        final ViewGroup view = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(page.layout, parent, false);
        switch (page) {
            case OPT_IN_CARD:
            case END_CARD:
                sponsoredSessionCardView.setupContentView(view, adData, listener, page);
                break;
            case VIDEO_CARD:
                sponsoredSessionVideoView.setupContentView(view, adData, listener);
                break;
            default:
                throw new IllegalAccessError("Ad page not supported: " + page + ", pos:" + position);
        }
        parent.addView(view);
        return view;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    enum PrestitialPage {
        OPT_IN_CARD(R.layout.sponsored_session_action_page, R.string.ads_opt_in_text, R.string.ads_no_thanks, R.string.ads_watch_now),
        VIDEO_CARD(R.layout.sponsored_session_video_page, Consts.NOT_SET, Consts.NOT_SET, Consts.NOT_SET),
        END_CARD(R.layout.sponsored_session_action_page, R.string.ads_sponsored_session_unlocked, R.string.ads_learn_more, R.string.ads_start_session);

        final int layout;
        final int description;
        final int optionOne;
        final int optionTwo;

        PrestitialPage(@LayoutRes int layout,
                       @StringRes int description,
                       @StringRes int optionOne,
                       @StringRes int optionTwo) {
            this.layout = layout;
            this.description = description;
            this.optionOne = optionOne;
            this.optionTwo = optionTwo;
        }
    }
}
