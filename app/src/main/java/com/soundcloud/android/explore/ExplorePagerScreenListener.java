package com.soundcloud.android.explore;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.rx.eventbus.EventBus;

import android.support.v4.view.ViewPager;

import javax.inject.Inject;

class ExplorePagerScreenListener implements ViewPager.OnPageChangeListener {

    private final EventBus eventBus;

    @Inject
    public ExplorePagerScreenListener(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onPageSelected(int position) {
        switch (position) {
            case ExplorePagerAdapter.TAB_GENRES:
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.EXPLORE_GENRES));
                break;
            case ExplorePagerAdapter.TAB_TRENDING_MUSIC:
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.EXPLORE_TRENDING_MUSIC));
                break;
            case ExplorePagerAdapter.TAB_TRENDING_AUDIO:
                eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.EXPLORE_TRENDING_AUDIO));
                break;
            default:
                throw new IllegalArgumentException("Did not recognise page in pager to publish screen event");
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageScrollStateChanged(int state) {}

}
