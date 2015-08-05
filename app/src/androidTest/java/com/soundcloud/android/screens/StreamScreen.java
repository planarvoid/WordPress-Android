package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.EmptyViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.RecyclerViewElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import java.util.List;

public class StreamScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;
    private final MenuScreen menuScreen;

    public StreamScreen(Han solo) {
        super(solo);
        waiter.waitForFragmentByTag("stream_fragment");
        menuScreen = new MenuScreen(solo);
    }

    public String getTitle() {
        return actionBar().getTitle();
    }

    public int getItemCount() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return streamList().getItemCount();
    }

    public int getBoundItemCount() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return streamList().getBoundItemCount();
    }

    public StreamScreen scrollToBottomOfPage() {
        streamList().scrollToBottomOfPage();
        return this;
    }

    public UpgradeScreen clickMidTierTrackForUpgrade(String title) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        testDriver.findElement(With.textContaining(title)).click();
        return new UpgradeScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public TrackItemElement getTrack(int index) {
        return trackItemElements().get(index);
    }

    public VisualPlayerElement clickFirstTrack() {
        return clickTrack(0);
    }

    public VisualPlayerElement clickFirstNotPromotedTrack() {
        if (isFirstTrackPromoted()) {
            return clickTrack(1);
        } else {
            return clickFirstTrack();
        }
    }

    public VisualPlayerElement clickTrack(int index) {
        getTrack(index).click();
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(testDriver);
        visualPlayerElement.waitForExpandedPlayer();
        return visualPlayerElement;
    }

    public TrackItemMenuElement clickFirstTrackOverflowButton() {
        getTrack(0).clickOverflowButton();
        return new TrackItemMenuElement(testDriver);
    }

    public boolean isFirstTrackPromoted() {
        return getTrack(0).isPromotedTrack();
    }

    public boolean isPromotedTrackWithPromoter() {
        return getTrack(0).hasPromoter();
    }

    public MenuScreen openMenu() {
        return menuScreen.open();
    }

    public ExploreScreen openExploreFromMenu() {
        return menuScreen.open().clickExplore();
    }

    private RecyclerViewElement streamList() {
        return testDriver.findElement(With.id(R.id.ak_recycler_view)).toRecyclerView();
    }

    private List<TrackItemElement> trackItemElements() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        scrollToItem(With.id(R.id.track_list_item));
        return Lists.transform(testDriver.findElements(With.id(R.id.track_list_item)), toTrackItemElement);
    }

    private ViewElement scrollToItem(With with) {
        int tries = 0;
        ViewElement result = testDriver.findElement(with);
        while (result instanceof EmptyViewElement) {
            tries++;
            streamList().scrollDown();
            if (tries > 10) {
                return new EmptyViewElement("Unable to scroll to item; item not in list");
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
}
