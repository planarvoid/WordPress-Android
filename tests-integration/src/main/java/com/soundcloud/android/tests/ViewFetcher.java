package com.soundcloud.android.tests;

import static com.google.common.collect.Collections2.filter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

import com.robotium.solo.Solo;
import com.soundcloud.android.screens.elements.ViewElement;
import junit.framework.AssertionFailedError;


import android.os.SystemClock;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

class ViewFetcher {
    private static final int DEFAULT_TIMEOUT = 20 * 1000;
    private static final int ELEMENT_TIMEOUT = 20 * 1000;
    private static final int SMALL_TIMEOUT = 500;
    private final Solo testDriver;

    public ViewFetcher(Solo driver){
        testDriver = driver;
    }

    public ViewElement findElement(int viewId) {
        return waitForViewWithId(viewId);
    }

    public ArrayList<ViewElement> findElements(final int id) {
        return Lists.newArrayList(filter(findVisibleElements(), new Predicate<ViewElement>() {
            public boolean apply(ViewElement viewElement) {
                return viewElement.getId() == id;
            }
        }));
    }

    private ViewElement waitForViewWithId(int viewId) {
        long endTime = SystemClock.uptimeMillis() + ELEMENT_TIMEOUT;
        ViewElement viewElement;

        while (SystemClock.uptimeMillis() <= endTime) {
            viewElement = findElementById(viewId);
            testDriver.sleep(SMALL_TIMEOUT);

            if (viewElement.isVisible()) {
                return viewElement;
            }
        }

        return findElementById(viewId);
    }

    private ViewElement findElementById(int id){
        ArrayList<ViewElement> foundElements = findElements(id);
        if (foundElements.isEmpty()){
            return new ViewElement(testDriver);
        }
        return foundElements.get(0);
    }

    public ViewElement findElement(Class<? extends View> viewClass) {
        View view = null;
        try {
            view = testDriver.getView(viewClass, 0);
        } catch (AssertionFailedError ignored) {

        }
        return new ViewElement(view, testDriver);
    }

    private List<ViewElement> findAllElements() {
        return Lists.transform(testDriver.getViews(), new Function<View, ViewElement>() {
            @Override
            public ViewElement apply(View view) {
                return new ViewElement(view, testDriver);
            }
        });
    }

    private List<ViewElement> findVisibleElements() {
        return Lists.newArrayList(filter(findAllElements(), new Predicate<ViewElement>() {
            public boolean apply(ViewElement viewElement) {
                return viewElement.isVisible();
            }
        }));

    }


}