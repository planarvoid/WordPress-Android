package com.soundcloud.android.framework;

import static com.google.common.collect.Collections2.filter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.robotium.solo.Solo;
import com.soundcloud.android.framework.viewelements.DefaultViewElement;
import com.soundcloud.android.framework.viewelements.EmptyViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.viewelements.ViewNotFoundException;
import com.soundcloud.android.framework.with.With;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class ViewFetcher {
    private Solo testDriver;
    private View parentView;
    private ElementWaiter elementWaiter = new ElementWaiter();
    private String TAG = getClass().getSimpleName().toString();

    public ViewFetcher(Solo driver){
        testDriver = driver;
    }

    public ViewFetcher(View view, Solo driver) {
        parentView = view;
        testDriver = driver;
    }
    public ViewElement findElement(final With with) {
        return elementWaiter.waitForElement(new Callable<List<ViewElement>>() {
            @Override
            public List<ViewElement> call() throws Exception {
                return Lists.newArrayList(filter(getAllVisibleElements(), with));
            }
        });
    }

    public List<ViewElement> findElements(With with) {
        return Lists.newArrayList(filter(getAllVisibleElements(), with));
    }

    public ViewElement getChildAt(int index) {
        return getDirectChildViews().get(index);
    }

    public boolean isElementDisplayed(With matcher) {
        testDriver.sleep(1000);
        return findVisibleElement(matcher).isVisible();
    }

    private ViewElement findVisibleElement(With matcher) {
        List<ViewElement> viewElements = Lists.newArrayList(filter(getAllVisibleElements(), matcher));
        if (viewElements.size() > 0) {
            Log.i(TAG, String.format("Number of views with ID found: %d", viewElements.size()));
            return viewElements.get(0);
        }
        return new EmptyViewElement();
    }

    private List<ViewElement> getDirectChildViews() {
        return Lists.newArrayList(filter(getAllVisibleElements(), new Predicate<ViewElement>() {
            @Override
            public boolean apply(ViewElement viewElement) {
                return viewElement.getParent().equals(parentView);
            }
        }));
    }

    private List<ViewElement> getAllVisibleElements() {
        return Lists.newArrayList(filter(getAllViewsFromScreen(), new Predicate<ViewElement>() {
            public boolean apply(ViewElement viewElement) {
                return viewElement.isVisible();
            }
        }));
    }

    private List<ViewElement> getAllViewsFromScreen() {
        final List<View> views = testDriver.getViews(parentView);

        if (views == null) {
            return Collections.emptyList();
        } else {
            return Lists.transform(views, new Function<View, ViewElement>() {
                @Override
                public ViewElement apply(View view) {
                    return new DefaultViewElement(view, testDriver);
                }
            });
        }
    }

    class ElementWaiter {
        private static final int ELEMENT_TIMEOUT = 4 * 1000;
        private static final int POLL_INTERVAL = 500;

        public ViewElement waitForElement(Callable<List<ViewElement>> callable) {
            return waitForOne(callable);
        }

        private ViewElement waitForOne(Callable<List<ViewElement>> callable) {
            long endTime = SystemClock.uptimeMillis() + ELEMENT_TIMEOUT;

            while (SystemClock.uptimeMillis() <= endTime) {
                testDriver.sleep(POLL_INTERVAL);
                try {
                    List<ViewElement> viewElements = callable.call();
                    if (viewElements.size() > 0) {
                        Log.i(TAG, String.format("Number of views with ID found: %d", viewElements.size()));
                        return viewElements.get(0);
                    }
                } catch (Exception e) {
                    throw new ViewNotFoundException(e);
                }
            }
            return new EmptyViewElement();
        }
    }
}
