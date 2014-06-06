package com.soundcloud.android.tests;

import static com.google.common.collect.Collections2.filter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.robotium.solo.Solo;
import junit.framework.AssertionFailedError;

import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import java.util.List;

class ViewFetcher {
    private static final int ELEMENT_TIMEOUT = 3 * 1000;
    private static final int SMALL_TIMEOUT = 500;
    private Solo testDriver;
    private View parentView;

    public ViewFetcher(Solo driver){
        testDriver = driver;
    }

    public ViewFetcher(View view, Solo driver) {
        parentView = view;
        testDriver = driver;
    }

    public ViewElement findElement(int viewId) {
        return waitForViewWithId(viewId);
    }

    public List<ViewElement> findElements(String textToFind) {
        return getElementsWithText(textToFind);
    }

    public ViewElement findElement(String textToFind) {
        List<ViewElement> foundViews = getElementsWithText(textToFind);
        return foundViews.isEmpty() ? new ViewElement(testDriver) : foundViews.get(0);
    }

    private List<ViewElement> getElementsWithText(final String textToFind) {
        return Lists.newArrayList(filter(getVisibleElements(), new Predicate<ViewElement>() {
            public boolean apply(ViewElement viewElement) {
                if (viewElement.isTextView()) {
                    return viewElement.getText().equals(textToFind);
                }
                return false;
            }
        }));
    }

    public List<ViewElement> findElements(final int id) {
        return Lists.newArrayList(filter(getVisibleElements(), new Predicate<ViewElement>() {
            public boolean apply(ViewElement viewElement) {
                return viewElement.getId() == id;
            }
        }));
    }

    private ViewElement waitForViewWithId(int viewId) {
        long endTime = SystemClock.uptimeMillis() + ELEMENT_TIMEOUT;
        ViewElement viewElement = null;

        while (SystemClock.uptimeMillis() <= endTime) {
            viewElement = findElementById(viewId);

            if (viewElement.isVisible()) {
                return viewElement;
            }
            testDriver.sleep(SMALL_TIMEOUT);
        }

        return viewElement;
    }

    private ViewElement findElementById(int id){
        List<ViewElement> foundElements = findElements(id);
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
        return Lists.transform(testDriver.getViews(parentView), new Function<View, ViewElement>() {
            @Override
            public ViewElement apply(View view) {
                return new ViewElement(view, testDriver);
            }
        });
    }

    private List<ViewElement> getVisibleElements() {
        return Lists.newArrayList(filter(findAllElements(), new Predicate<ViewElement>() {
            public boolean apply(ViewElement viewElement) {
                return viewElement.isVisible();
            }
        }));

    }
}
