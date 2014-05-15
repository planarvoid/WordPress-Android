package com.soundcloud.android.screens.elements;

import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;

public class ViewPagerElement extends Element {
    private final static int MAX_VIEWS = 3;
    private ViewPager viewPager;

    public ViewPagerElement(Han solo) {
        super(solo);
        solo.getSolo().waitForView(ViewPager.class);
        viewPager = solo.getView(ViewPager.class, 0);
    }

    public ViewPagerElement(Han solo, int viewPagerId) {
        super(solo);
        solo.getSolo().waitForView(viewPagerId);
        viewPager = (ViewPager) solo.getView(viewPagerId);
    }

    @Override
    protected int getRootViewId() {
        return R.id.pager;
    }

    public  <T extends View> ArrayList<T> getPages(Class<T> viewClass) {
        return solo.getSolo().getCurrentViews(viewClass, viewPager);
    }

    public  <T extends View> View getCurrentPage(Class<T> viewClass) {
        return solo.getSolo().getCurrentViews(viewClass, getVisiblePage()).get(0);
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

    private int getAllPages() {
        return adapter().getCount();
    }

    private int getDisplayedPagesCount() {
        return viewPager.getChildCount();
    }

    private int getPagesCount() {
        return adapter().getCount();
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

    //TODO: Move this to Device class
    private int getScreenWidth() {
        return getDisplay().getWidth();
    }

    private int getScreenHeight() {
        return getDisplay().getHeight();
    }

    private Display getDisplay() {
        return ((WindowManager) solo.getCurrentActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }


}
