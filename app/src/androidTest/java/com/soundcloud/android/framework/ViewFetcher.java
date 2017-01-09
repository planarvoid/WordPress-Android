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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ViewFetcher {
    private static final int NETWORK_TIMEOUT = (int) TimeUnit.MINUTES.toMillis(1);

    private Han testDriver;
    private View parentView;
    private ElementWaiter elementWaiter = new ElementWaiter();
    private String TAG = getClass().getSimpleName();

    public ViewFetcher(Han driver) {
        testDriver = driver;
    }

    public ViewFetcher(View view, Han driver) {
        parentView = view;
        testDriver = driver;
    }

    private boolean waitForBusyUi() {
        if (testDriver.getBusyUiIndicator() != null) {
            return testDriver.waitForCondition(new BusyIndicatorCondition(testDriver.getBusyUiIndicator()),
                                               NETWORK_TIMEOUT);
        }
        return true;
    }

    public ViewElement findElement(final With with) {
        ViewElement viewElement = elementWaiter.waitForElement(with);
        if (!(viewElement instanceof EmptyViewElement) && waitForBusyUi()) {
            return elementWaiter.waitForElement(with);
        }
        return viewElement;
    }

    public ViewElement findOnScreenElement(final With with) {
        ViewElement viewElement = elementWaiter.waitForOnScreenElement(with);
        if (!viewElement.isOnScreen() && waitForBusyUi()) {
            return elementWaiter.waitForOnScreenElement(with);
        }
        return viewElement;
    }

    public List<ViewElement> findOnScreenElements(With with) {
        List<ViewElement> viewElements = elementWaiter.waitForOnScreenElements(with);
        if (!viewElements.get(0).isOnScreen() && waitForBusyUi()) {
            return elementWaiter.waitForOnScreenElements(with);
        }
        return viewElements;
    }

    public ViewElement findOnScreenElement(final With... withs) {
        return findOnScreenElements(withs).get(0);
    }

    public List<ViewElement> findOnScreenElements(final With... withs) {
        if (withs.length == 0) {
            return emptyViewElementList("Zero arguments");
        }

        if (withs.length == 1) {
            return findOnScreenElements(withs[0]);
        }

        return findOnScreenElements(withs[0], Arrays.copyOfRange(withs, 1, withs.length));
    }

    private List<ViewElement> findOnScreenElements(With with, final With... withs) {
        if (withs.length == 0) {
            return emptyViewElementList("Not enough arguments");
        }

        List<ViewElement> results = Lists.newArrayList(filter(
                getOnScreenElements(with),
                viewElement -> {
                    for (With with1 : withs) {
                        try {
                            if (!with1.apply(viewElement)) {
                                return false;
                            }
                        } catch (Exception e) {
                            return false;
                        }
                    }
                    return true;

                }
        ));

        if (results.size() == 0) {
            return emptyViewElementList(failedToFindElementsMessage(withs));
        }
        return results;
    }

    private List<ViewElement> emptyViewElementList(String message) {
        List<ViewElement> result = new ArrayList<>();
        result.add(new EmptyViewElement(message));
        return result;
    }

    private String failedToFindElementsMessage(With... withs) {
        String result = "Unable to find elements with: ";
        for (With with : withs) {
            result += with.getSelector() + ", ";
        }
        return result;
    }

    public ViewElement findAncestor(View root, With with) {
        final ViewFetcher ancestorViewsFetcher = new ViewFetcher(root, testDriver);
        final List<ViewElement> matchingViews = ancestorViewsFetcher.findOnScreenElements(with);
        final ViewElement expectedChild = new DefaultViewElement(parentView, testDriver);

        for (ViewElement matchingView : matchingViews) {
            if (matchingView.isAncestorOf(expectedChild)) {
                return matchingView;
            }
        }

        return new EmptyViewElement("Find ancestor with " + with);
    }

    public ViewElement getChildAt(int index) {
        return getDirectChildViews().get(index);
    }

    public boolean isElementOnScreen(With matcher) {
        testDriver.sleep(500);
        return getOnScreenElement(matcher).isOnScreen();
    }

    public List<ViewElement> getDirectChildViews() {
        return Lists.newArrayList(filter(getAllOnScreenElements(), viewElement -> parentView.equals(viewElement.getParent())));
    }

    private ViewElement getOnScreenElement(With matcher) {
        List<ViewElement> viewElements = Lists.newArrayList(filter(getAllOnScreenElements(), matcher));
        if (viewElements.size() > 0) {
            return viewElements.get(0);
        }
        Log.i(TAG, String.format("SELECTOR (%s), VIEWS FOUND: %d", matcher.getSelector(), viewElements.size()));
        return new EmptyViewElement(matcher.getSelector());
    }

    private List<ViewElement> getOnScreenElements(With matcher) {
        List<ViewElement> viewElements = Lists.newArrayList(filter(getAllOnScreenElements(), matcher));
        Log.i(TAG, String.format("SELECTOR (%s), VIEWS FOUND: %d", matcher.getSelector(), viewElements.size()));
        if (viewElements.size() > 0) {
            return viewElements;
        }
        return emptyViewElementList(failedToFindElementsMessage(matcher));
    }

    private List<ViewElement> getAllOnScreenElements() {
        return Lists.newArrayList(filter(getAllChildViews(), viewElement -> viewElement.isOnScreen()));
    }

    private List<ViewElement> getAllChildViews() {
        final List<View> views = testDriver.getViews(parentView);

        if (views == null) {
            return Collections.emptyList();
        } else {
            return Lists.transform(views, view -> new DefaultViewElement(view, testDriver));
        }
    }

    class ElementWaiter {
        private static final int ELEMENT_TIMEOUT = 2 * 1000;
        private static final int POLL_INTERVAL = 100;

        public ViewElement waitForElement(final With with) {
            return waitForOne(with.getSelector(), () -> Lists.newArrayList(filter(getAllChildViews(), with)));
        }

        public List<ViewElement> waitForOnScreenElements(final With with) {
            return waitForMany(with.getSelector(), () -> Lists.newArrayList(filter(getAllOnScreenElements(), with)));
        }

        public ViewElement waitForOnScreenElement(final With with) {
            return waitForOne(with.getSelector(), () -> Lists.newArrayList(filter(getAllOnScreenElements(), with)));
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

    private class BusyIndicatorCondition implements Condition {
        private final With viewMatcher;

        public BusyIndicatorCondition(With matcher) {
            viewMatcher = matcher;
        }

        @Override
        public boolean isSatisfied() {
            Log.i("BUSYUI", String.format("Waiting for Busy UI (Is busy: %b)", isElementOnScreen(viewMatcher)));
            return !isElementOnScreen(viewMatcher);
        }
    }
}
