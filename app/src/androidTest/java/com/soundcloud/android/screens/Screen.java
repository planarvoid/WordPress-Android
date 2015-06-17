package com.soundcloud.android.screens;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.EmptyViewElement;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;
import com.soundcloud.android.screens.elements.PlaylistItemElement;
import com.soundcloud.android.screens.elements.ToolBarElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.UserItemElement;

import java.util.List;

public abstract class Screen {
    protected Han testDriver;
    protected Waiter waiter;
    protected static final int CONTENT_ROOT = android.R.id.content;

    public Screen(Han solo) {
        this.testDriver = solo;
        this.waiter = new Waiter(solo);
        waiter.waitForActivity(getActivity());
        waiter.waitForElement(CONTENT_ROOT);
    }

    public void pullToRefresh() {
        testDriver.swipeDown();
        waiter.waitForContentAndRetryIfLoadingFailed();
        waiter.waitForTextToDisappear("Loading");
    }

    public void swipeLeft() {
        testDriver.swipeLeft();
    }

    public EmptyViewElement emptyView(){
       return new EmptyViewElement(testDriver, With.id(R.id.ak_emptyview_holder));
    }

    public ViewElement errorView(){
        return testDriver.findElement(With.id(com.soundcloud.android.R.id.ak_error_view));
    }

    public void retryFromErrorView(){
        errorView().findElement(With.id(R.id.ak_emptyview_btn_retry)).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public ViewElement emptyConnectionErrorMessage(){
        return emptyView().emptyConnectionErrorMessage();
    }

    public void swipeRight() {
        testDriver.swipeRight();
    }

    public boolean isVisible() {
        return getActivity().getSimpleName().equals(testDriver.getCurrentActivity().getClass().getSimpleName());
    }

    public ToolBarElement actionBar() {
        return new ToolBarElement(testDriver);
    }

    public GoBackOnlineDialogElement getGoBackOnlineDialog() {
        return new GoBackOnlineDialogElement(testDriver);
    }

    public List<TrackItemElement> getTracks() {
        return Lists.transform(
                testDriver.findElements(With.id(com.soundcloud.android.R.id.track_list_item)),
                toTrackItemElement
        );
    }

    public List<PlaylistItemElement> getPlaylists() {
        return Lists.transform(
                testDriver.findElements(With.id(com.soundcloud.android.R.id.playlist_list_item)),
                toPlaylistItemElement
        );
    }

    public List<UserItemElement> getUsers() {
        return Lists.transform(
                testDriver.findElements(With.id(com.soundcloud.android.R.id.user_list_item)),
                toUserItemElement
        );
    }

    private final Function<ViewElement, TrackItemElement> toTrackItemElement = new Function<ViewElement, TrackItemElement>() {
        @Override
        public TrackItemElement apply(ViewElement viewElement) {
            return new TrackItemElement(testDriver, viewElement);
        }
    };

    private final Function<ViewElement, PlaylistItemElement> toPlaylistItemElement = new Function<ViewElement, PlaylistItemElement>() {
        @Override
        public PlaylistItemElement apply(ViewElement viewElement) {
            return new PlaylistItemElement(testDriver, viewElement);
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
