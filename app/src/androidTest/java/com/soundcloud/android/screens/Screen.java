package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.EmptyViewElement;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;
import com.soundcloud.android.screens.elements.ToolBarElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import java.util.List;

public abstract class Screen {

    protected Han testDriver;
    protected Waiter waiter;

    public Screen(Han solo) {
        this.testDriver = solo;
        this.waiter = new Waiter(solo);
    }

    public void pullToRefresh() {
        testDriver.swipeDown();
        waiter.waitForContentAndRetryIfLoadingFailed();
        waiter.waitForTextToDisappear("Loading");
    }

    public void waitForContentAndRetryIfLoadingFailed() {
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void swipeLeft() {
        testDriver.swipeLeft();
    }

    public EmptyViewElement emptyView() {
        return new EmptyViewElement(testDriver, With.className(EmptyView.class.getName()));
    }

    public ViewElement errorView() {
        return testDriver.findOnScreenElement(With.id(com.soundcloud.android.R.id.ak_error_view));
    }

    public ViewElement emptyConnectionErrorMessage() {
        return emptyView().emptyConnectionErrorMessage();
    }

    public void swipeRight() {
        testDriver.swipeRight();
    }

    public boolean isVisible() {
        waiter.waitForActivity(getActivity());
        return getActivity().getSimpleName().equals(testDriver.getCurrentActivity().getClass().getSimpleName());
    }

    public ToolBarElement actionBar() {
        return new ToolBarElement(testDriver);
    }

    public String getActionBarTitle() {
        return actionBar().getTitle();
    }

    public GoBackOnlineDialogElement getGoBackOnlineDialog() {
        return new GoBackOnlineDialogElement(testDriver);
    }

    public List<TrackItemElement> getTracks() {
        scrollToItem(With.id(com.soundcloud.android.R.id.track_list_item));
        return Lists.transform(
                testDriver.findOnScreenElements(With.id(com.soundcloud.android.R.id.track_list_item)),
                toTrackItemElement
        );
    }

    public List<UserItemElement> getUsers() {
        return Lists.transform(
                testDriver.findOnScreenElements(With.id(com.soundcloud.android.R.id.user_list_item)),
                toUserItemElement
        );
    }

    protected ViewElement scrollToItem(final With... with) {
        return testDriver.scrollToItem(with);
    }

    protected ViewElement scrollToItemInRecyclerView(final With... with) {
        return testDriver.scrollToItemInRecyclerView(with);
    }

    private final Function<ViewElement, TrackItemElement> toTrackItemElement = new Function<ViewElement, TrackItemElement>() {
        @Override
        public TrackItemElement apply(ViewElement viewElement) {
            return new TrackItemElement(testDriver, viewElement);
        }
    };

    private final Function<ViewElement, UserItemElement> toUserItemElement = new Function<ViewElement, UserItemElement>() {
        @Override
        public UserItemElement apply(ViewElement viewElement) {
            return new UserItemElement(testDriver, viewElement);
        }
    };


    abstract protected Class getActivity();

    @Override
    public String toString() {
        return String.format("Page: %s, Activity: %s, CurrentActivity: %s",
                             getClass().getSimpleName(),
                             getActivity().getSimpleName(),
                             testDriver.getCurrentActivity().toString()
        );
    }
}
