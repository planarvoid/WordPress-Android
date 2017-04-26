package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.ViewElement;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class OfflineStateButtonElement {

    private final Waiter waiter;
    private final ViewElement wrappedElement;

    public OfflineStateButtonElement(Han driver, ViewElement element) {
        this.waiter = new Waiter(driver);
        this.wrappedElement = element;
    }

    public boolean isWaitingState() {
        return wrappedElement.toOfflineStateButton().isWaitingState();
    }

    public boolean isDefaultState() {
        return wrappedElement.toOfflineStateButton().isDefaultState();
    }

    public boolean isDownloadingState() {
        return wrappedElement.toOfflineStateButton().isDownloadingState();
    }

    public boolean isDownloadedState() {
        return wrappedElement.toOfflineStateButton().isDownloadedState();
    }

    public static class IsDefault extends TypeSafeMatcher<OfflineStateButtonElement> {
        @Override
        public void describeTo(Description description) {
            description.appendText("default");
        }

        @Override
        protected boolean matchesSafely(final OfflineStateButtonElement element) {
            return element.waiter.waitForElementCondition(element::isDefaultState);
        }

        @Factory
        public static Matcher<OfflineStateButtonElement> defaultState() {
            return new OfflineStateButtonElement.IsDefault();
        }
    }

    public static class IsDownloading extends TypeSafeMatcher<OfflineStateButtonElement> {
        @Override
        public void describeTo(Description description) {
            description.appendText("downloading");
        }

        @Override
        protected boolean matchesSafely(final OfflineStateButtonElement element) {
            return element.waiter.waitForElementCondition(element::isDownloadingState);
        }

        @Factory
        public static Matcher<OfflineStateButtonElement> downloadingState() {
            return new OfflineStateButtonElement.IsDownloading();
        }
    }

    public static class IsDownloaded extends TypeSafeMatcher<OfflineStateButtonElement> {
        @Override
        public void describeTo(Description description) {
            description.appendText("downloaded");
        }

        @Override
        protected boolean matchesSafely(final OfflineStateButtonElement element) {
            return element.waiter.waitForElementCondition(element::isDownloadedState);
        }

        @Factory
        public static Matcher<OfflineStateButtonElement> downloadedState() {
            return new OfflineStateButtonElement.IsDownloaded();
        }
    }

    public static class IsDownloadingOrDownloaded extends TypeSafeMatcher<OfflineStateButtonElement> {

        @Override
        public void describeTo(Description description) {
            description.appendText("downloading or downloaded");
        }

        @Override
        protected boolean matchesSafely(final OfflineStateButtonElement element) {
            return element.waiter.waitForElementCondition(() -> element.isDownloadingState() || element.isDownloadedState());
        }

        @Factory
        public static Matcher<OfflineStateButtonElement> downloadingOrDownloadedState() {
            return new OfflineStateButtonElement.IsDownloadingOrDownloaded();
        }
    }

}
