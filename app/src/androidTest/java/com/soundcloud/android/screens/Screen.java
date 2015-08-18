package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.RecyclerViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.viewelements.ViewNotFoundException;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.elements.EmptyViewElement;
import com.soundcloud.android.screens.elements.GoBackOnlineDialogElement;
import com.soundcloud.android.screens.elements.PlaylistItemElement;
import com.soundcloud.android.screens.elements.ToolBarElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.UserItemElement;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import java.util.List;

public abstract class Screen {
    protected static final int MAX_SCROLLS_TO_FIND_ITEM = 10;

    protected Han testDriver;
    protected Waiter waiter;

    public Screen(Han solo) {
        this.testDriver = solo;
        this.waiter = new Waiter(solo);
        waiter.waitForActivity(getActivity());
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
        scrollListToItem(With.id(com.soundcloud.android.R.id.track_list_item));
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

    public UserItemElement scrollToUserWithUsername(String username) {
        int tries = 0;
        while (tries < 10) {
            scrollListToItem(With.id(com.soundcloud.android.R.id.user_list_item));
            for (UserItemElement user : getUsers()) {
                if (user.getUsername().equals(username)) {
                    return user;
                }
            }
            testDriver.scrollToBottom();
            tries++;
        }
        throw new ViewNotFoundException("Unable to find user with username " + username);
    }

    protected ViewElement scrollListToItem(With with) {
        ViewElement result = testDriver.findElement(with);
        while (result instanceof com.soundcloud.android.framework.viewelements.EmptyViewElement) {
            if (!testDriver.scrollDown()) {
                return new com.soundcloud.android.framework.viewelements.EmptyViewElement("Unable to scroll to item; item not in list");
            }
            result = testDriver.findElement(with);
        }
        return result;
    }

    protected ViewElement scrollToItem(With with, RecyclerViewElement recyclerViewElement) {
        int tries = 0;
        ViewElement result = testDriver.findElement(with);
        while (result instanceof com.soundcloud.android.framework.viewelements.EmptyViewElement) {
            tries++;
            recyclerViewElement.scrollDown();
            if (tries > MAX_SCROLLS_TO_FIND_ITEM) {
                return new com.soundcloud.android.framework.viewelements.EmptyViewElement("Unable to scroll to item; item not in list");
            }
            result = testDriver.findElement(with);
        }
        return result;
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
