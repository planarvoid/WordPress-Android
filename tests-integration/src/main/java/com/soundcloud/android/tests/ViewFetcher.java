package com.soundcloud.android.tests;

import static com.google.common.collect.Collections2.filter;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.robotium.solo.Solo;
import com.soundcloud.android.tests.with.With;

import android.os.SystemClock;
import android.view.View;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

class ViewFetcher {
    private Solo testDriver;
    private View parentView;
    private Waiter waiter = new Waiter();

    public ViewFetcher(Solo driver){
        testDriver = driver;
    }

    public ViewFetcher(View view, Solo driver) {
        parentView = view;
        testDriver = driver;
    }
    public ViewElement findElement(final With with) {
        return waiter.waitForElement(new Callable<List<ViewElement>>() {
            @Override
            public List<ViewElement> call() throws Exception {
                return Lists.newArrayList(filter(getAllVisibleElements(), with));
            }
        });
    }

    public List<ViewElement> findElements(With with) {
        return Lists.newArrayList(filter(getAllViewsFromScreen(), with));
    }

    public ViewElement getChildAt(int index) {
        return getDirectChildViews().get(index);
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
                    return new ViewElement(view, testDriver);
                }
            });
        }
    }

    class Waiter {
        private static final int ELEMENT_TIMEOUT = 3 * 1000;
        private static final int POLL_INTERVAL = 500;

        public ViewElement waitForElement(Callable<List<ViewElement>> callable) {
            return waitForOne(callable);
        }

        private ViewElement waitForOne(Callable<List<ViewElement>> callable) {
            long endTime = SystemClock.uptimeMillis() + ELEMENT_TIMEOUT;
            List<ViewElement> viewElements;

            while (SystemClock.uptimeMillis() <= endTime) {
                try {
                    viewElements = callable.call();
                    if (viewElements.size() > 0) {
                        return viewElements.get(0);
                    }
                } catch (Exception e) {
                    throw new ViewNotFoundException(e);
                }
                testDriver.sleep(POLL_INTERVAL);
            }
            throw new ViewNotFoundException(callable.toString());
        }
    }
}
