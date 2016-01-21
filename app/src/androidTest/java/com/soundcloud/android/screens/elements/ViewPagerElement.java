package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;

import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

public class ViewPagerElement extends Element {
    private ViewPager viewPager;

    public ViewPagerElement(Han solo) {
        super(solo, With.id(R.id.pager));
        waiter.waitForElement(With.className(ViewPager.class));
        viewPager = solo.findOnScreenElement(With.className(ViewPager.class)).toViewPager();
    }

    /**
     * @deprecated Please use testDriver fo find your elements
     */
    @Deprecated
    public  <T extends View> View getCurrentPage(Class<T> viewClass) {
        return testDriver.getSolo().getCurrentViews(viewClass, getVisiblePage()).get(0);
    }

    public String getCurrentTabText() {
        return adapter().getPageTitle(viewPager.getCurrentItem()).toString();
    }

    //TODO: Yes, this only works assuming that we have one page visible on screen
    private View getVisiblePage() {
        View childView;
        int[] locationOnScreen = new int[2];

        for (int i = 0; i < viewPager.getChildCount(); i += 1 ) {
            childView = viewPager.getChildAt(i);
            childView.getLocationOnScreen(locationOnScreen);

            if( isVisible(childView) ) {
                return childView;
            }
        }

        return viewPager.getChildAt(0);
    }

    private boolean isVisible(View view) {
        int[] locationOnScreen = new int[2];
        view.getLocationOnScreen(locationOnScreen);

        return locationOnScreen[0] == getX();
    }

    private PagerAdapter adapter() {
        return viewPager.getAdapter();
    }

    private int getX() {
        return getLocationOnScreen()[0];
    }

    private int getY() {
        return getLocationOnScreen()[1];
    }

    private int[] getLocationOnScreen() {
        int[] locationOnScreen = new int [2];
        viewPager.getLocationOnScreen(locationOnScreen);
        return locationOnScreen;
    }
}
