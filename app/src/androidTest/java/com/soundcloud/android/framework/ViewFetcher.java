package com.soundcloud.android.framework;

import static com.soundcloud.java.collections.MoreCollections.filter;

import com.robotium.solo.Condition;
import com.soundcloud.android.framework.viewelements.DefaultViewElement;
import com.soundcloud.android.framework.viewelements.EmptyViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.viewelements.ViewNotFoundException;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;

import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ViewFetcher {
    private static final int NETWORK_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(1);

    private Han testDriver;
    private View parentView;
    private ElementWaiter elementWaiter = new ElementWaiter();
    private String TAG = getClass().getSimpleName().toString();
    private With busyUiIndicator;

    public ViewFetcher(Han driver){
        testDriver = driver;
    }

    public ViewFetcher(View view, Han driver) {
        parentView = view;
        testDriver = driver;
    }

    public void registerBusyUIIndicator(With busyUiIndicator) {
        this.busyUiIndicator = busyUiIndicator;
    }

    private boolean waitForBusyUi() {
        if(busyUiIndicator != null) {
            return testDriver.waitForCondition(new BusyIndicatorCondition(busyUiIndicator), NETWORK_TIMEOUT);
        }
        return true;
    }

    public ViewElement findElement(final With with) {
        ViewElement viewElement = elementWaiter.waitForElement(with);
        if(!viewElement.isVisible() && waitForBusyUi()) {
            return elementWaiter.waitForElement(with);
        }
        return viewElement;
    }

    public List<ViewElement> findElements(With with) {
        return elementWaiter.waitForElements(with);
    }

    public ViewElement getChildAt(int index) {
        return getDirectChildViews().get(index);
    }

    public List<ViewElement> getChildren() {
        return getDirectChildViews();
    }
    public boolean isElementDisplayed(With matcher) {
        testDriver.sleep(500);
        return findVisibleElement(matcher).isVisible();
    }

    private ViewElement findVisibleElement(With matcher) {
        List<ViewElement> viewElements = Lists.newArrayList(filter(getAllVisibleElements(), matcher));
        if (viewElements.size() > 0) {
            return viewElements.get(0);
        }
        Log.i(TAG, String.format("SELECTOR (%s), VIEWS FOUND: %d", matcher.getSelector(), viewElements.size()));
        return new EmptyViewElement(matcher.getSelector());
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
        private static final int ELEMENT_TIMEOUT = 2 * 1000;
        private static final int POLL_INTERVAL = 100;

        public List<ViewElement> waitForElements(final With with) {
            return waitForMany(with.getSelector(), new Callable<List<ViewElement>>() {
                @Override
                public List<ViewElement> call() throws Exception {
                    return Lists.newArrayList(filter(getAllVisibleElements(), with));
                }
            });
        }

        public ViewElement waitForElement(final With with) {
            return waitForOne(with.getSelector(), new Callable<List<ViewElement>>() {
                @Override
                public List<ViewElement> call() throws Exception {
                    return Lists.newArrayList(filter(getAllVisibleElements(), with));
                }
            });
        }

        private List<ViewElement> waitForMany(String selector, Callable<List<ViewElement>> callable) {
            long endTime = SystemClock.uptimeMillis() + ELEMENT_TIMEOUT;

            while (SystemClock.uptimeMillis() <= endTime) {
                testDriver.sleep(POLL_INTERVAL);
                try {
                    List<ViewElement> viewElements = callable.call();
                    Log.i(TAG, String.format("SELECTOR (%s), VIEWS FOUND: %d", selector, viewElements.size()));
                    if (viewElements.size() > 0) {
                        return viewElements;
                    }
                } catch (Exception e) {
                    throw new ViewNotFoundException(e);
                }
            }
            return Collections.<ViewElement>singletonList(new EmptyViewElement(selector));
        }

        private ViewElement waitForOne(String selector, Callable<List<ViewElement>> callable) {
            return waitForMany(selector, callable).get(0);
        }
    }
    private class BusyIndicatorCondition implements Condition{
        private final With viewMatcher;

        public BusyIndicatorCondition(With matcher){
            viewMatcher = matcher;
        }
        @Override
        public boolean isSatisfied() {
            return !isElementDisplayed(viewMatcher);
        }
    }
}
