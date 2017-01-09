package com.soundcloud.android.screens.elements;

import com.robotium.solo.Condition;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.tests.offline.OfflinePlaylistTest;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.objects.MoreObjects;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class DownloadImageViewElement {
    private final Waiter waiter;
    private final ViewElement wrappedElement;


    public DownloadImageViewElement(Han driver, ViewElement element) {
        this.waiter = new Waiter(driver);
        this.wrappedElement = element;
    }

    public boolean isVisible() {
        return wrappedElement.isOnScreen();
    }

    public boolean isUnavailable() {
        return wrappedElement.toDownloadImageView().isUnavailable();
    }

    public boolean isRequested() {
        return wrappedElement.toDownloadImageView().isRequested();
    }

    public boolean isDownloading() {
        return wrappedElement.toDownloadImageView().isDownloading();
    }

    public boolean isDownloaded() {
        return wrappedElement.toDownloadImageView().isDownloaded();
    }

    public static class IsRequested extends TypeSafeMatcher<DownloadImageViewElement> {
        @Override
        public void describeTo(Description description) {
            description.appendText("requested");
        }

        @Override
        protected boolean matchesSafely(final DownloadImageViewElement element) {
            debugLog("IsRequested.matchesSafely ", element);
            return element.waiter.waitForElementCondition(() -> element.isRequested());
        }

        @Factory
        public static Matcher<DownloadImageViewElement> requested() {
            return new IsRequested();
        }

    }

    public static class IsDownloading extends TypeSafeMatcher<DownloadImageViewElement> {
        @Override
        public void describeTo(Description description) {
            description.appendText("downloading");
        }

        @Override
        protected boolean matchesSafely(final DownloadImageViewElement element) {
            return element.waiter.waitForElementCondition(() -> {
                debugLog("IsDownloading.matchesSafely ", element);
                return element.isDownloading();
            });
        }

        @Factory
        public static Matcher<DownloadImageViewElement> downloading() {
            return new IsDownloading();
        }

    }

    public static class IsDownloaded extends TypeSafeMatcher<DownloadImageViewElement> {
        @Override
        public void describeTo(Description description) {
            description.appendText("downloaded");
        }

        @Override
        protected boolean matchesSafely(final DownloadImageViewElement element) {
            return element.waiter.waitForElementCondition(() -> {
                debugLog("IsDownloaded.matchesSafely ", element);
                return element.isDownloaded();
            });
        }

        @Factory
        public static Matcher<DownloadImageViewElement> downloaded() {
            return new IsDownloaded();
        }

    }

    public static class IsDownloadingOrDownloaded extends TypeSafeMatcher<DownloadImageViewElement> {

        @Override
        public void describeTo(Description description) {
            description.appendText("downloading or downloaded");
        }

        @Override
        protected boolean matchesSafely(final DownloadImageViewElement element) {
            return element.waiter.waitForElementCondition(() -> {
                debugLog("IsDownloadingOrDownloaded.matchesSafely ", element);
                return element.isDownloading() || element.isDownloaded();
            });
        }

        @Factory
        public static Matcher<DownloadImageViewElement> downloadingOrDownloaded() {
            return new IsDownloadingOrDownloaded();
        }
    }

    private static void debugLog(String name, DownloadImageViewElement element) {
        Log.d(OfflinePlaylistTest.TAG, new StringBuilder()
                .append(name)
                .append("isVisible=").append(element.isVisible())
                .append("wrappedElement details [").append(element.wrappedElement.debugOfflineTest()).append(']')
                .append("state=").append(element.getStateString())
                .append("=>").append(element.isDownloaded())
                .toString()
        );
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(DownloadImageViewElement.class)
                          .add("isVisible", isVisible())
                          .add("state", getStateString())
                          .toString();
    }

    private String getStateString() {
        if (isUnavailable()) return "unavailable";
        else if (isRequested()) return "requested";
        else if (isDownloading()) return "downloading";
        else if (isDownloaded()) return "downloaded";
        else return "unknown?";
    }
}

