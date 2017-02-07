package com.soundcloud.android.ads;

import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.collections.Pair;
import com.soundcloud.rx.eventbus.TestEventBus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InlayAdHelperTest extends AndroidUnitTest {
    private static AppInstallAd appInstall() {
        return AppInstallAd.create(AdFixtures.getApiAppInstall(), 434343);
    }

    private static final Date CURRENT_DATE = new Date();
    private static final boolean SCROLLING_UP = true;
    private static final boolean SCROLLING_DOWN = false;
    private static final AppInstallAd APP_INSTALL = appInstall();
    private static final VideoAd VIDEO_AD = AdFixtures.getVideoAd(32L);
    private static final StreamItem VIDEO_AD_ITEM = StreamItem.forVideoAd(VIDEO_AD);
    private static final StreamItem APP_INSTALL_ITEM = StreamItem.forAppInstall(APP_INSTALL);
    private static final StreamItem GO_UPSELL_ITEM = new StreamItem() {
        @Override
        public Kind kind() {
            return Kind.STREAM_UPSELL;
        }
    };
    private static final StreamItem TRACK_ITEM = new StreamItem() {
        @Override
        public Kind kind() {
            return Kind.TRACK;
        }
    };

    @Mock StaggeredGridLayoutManager layoutManager;
    @Mock StreamAdapter adapter;
    @Mock List<StreamItem> list;
    @Spy TestEventBus eventBus = new TestEventBus();

    private CurrentDateProvider dateProvider = new TestDateProvider(CURRENT_DATE.getTime());

    private InlayAdHelper inlayAdHelper;

    @Before
    public void setUp() {
        inlayAdHelper = new InlayAdHelper(layoutManager, adapter, dateProvider, eventBus);
    }

    @After
    public void tearDown() {
       // PagingRecyclerItemAdapter.getItemCount() can return items.size() + 1 on loading which
       // causes a crash on get.
       verify(adapter, never()).getItemCount();
    }

    @Test
    public void insertAdOnScrollDownWithOnEmptyAdapterFailsToInsertAd() {
        setEdgeVisiblePosition(0);
        setStreamItems(0, TRACK_ITEM);

        assertThat(inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_DOWN)).isFalse();
        verify(adapter, never()).addItem(anyInt(), any(StreamItem.class));
    }

    @Test
    public void insertAdOnScrollUpWithOnEmptyAdapterFailsToInsertAd() {
        setEdgeVisiblePosition(0);
        setStreamItems(0, TRACK_ITEM);

        assertThat(inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_UP)).isFalse();
    }

    @Test
    public void insertAdOnScrollDownWithPlayableItemsInsertsAd() {
        setEdgeVisiblePosition(6);
        setStreamItems(10, TRACK_ITEM);

        boolean inserted = inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(7, APP_INSTALL_ITEM);
    }

    @Test
    public void insertAdOnScrollUpWithPlayableItemsInsertsAd() {
        setEdgeVisiblePosition(6);
        setStreamItems(10, TRACK_ITEM);

        boolean inserted = inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_UP);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(5, APP_INSTALL_ITEM);
    }

    @Test
    public void insertAdOnScrollUpNeverInsertsAdBeforeEarliestPositionForAd() {
        setEdgeVisiblePosition(5);
        setStreamItems(10, TRACK_ITEM);

        boolean inserted = inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_UP);

        assertThat(inserted).isFalse();
        verify(adapter, never()).addItem(anyInt(), any(StreamItem.class));
    }

    @Test
    public void insertAdOnScrollDownPlayableItemsInsertsAdAfterEarliestPositionForAd() {
        setEdgeVisiblePosition(2);
        setStreamItems(10, TRACK_ITEM);

        boolean inserted = inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(5, APP_INSTALL_ITEM);
    }

    @Test
    public void insertAdOnScrollDownDoesntInsertAdIfSearchSpaceContainsAdsOnBothSides() {
        setEdgeVisiblePosition(4);
        setStreamItems(15, TRACK_ITEM);
        // make sure we're searching in both directions sufficiently far
        when(adapter.getItem(1)).thenReturn(APP_INSTALL_ITEM);
        when(adapter.getItem(10)).thenReturn(APP_INSTALL_ITEM);

        boolean inserted = inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isFalse();
        verify(adapter, never()).addItem(anyInt(), any(StreamItem.class));
    }

    @Test
    public void insertAdOnScrollUpDoesntInsertAdIfSearchSpaceContainsAdsOnBothSides() {
        setEdgeVisiblePosition(4);
        setStreamItems(15, TRACK_ITEM);
        // make sure we're searching in both directions sufficiently far
        when(adapter.getItem(0)).thenReturn(APP_INSTALL_ITEM);
        when(adapter.getItem(9)).thenReturn(APP_INSTALL_ITEM);

        boolean inserted = inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isFalse();
        verify(adapter, never()).addItem(anyInt(), any(StreamItem.class));
    }

    @Test
    public void insertAdTerminatesIfItCanNotFindSpaceForAdWithinMaxSearchDistance() {
        setEdgeVisiblePosition(0);
        setItemListSize(Integer.MAX_VALUE);
        when(adapter.getItem(anyInt())).thenReturn(APP_INSTALL_ITEM);

        boolean inserted = inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isFalse();
    }

    @Test
    public void insertAdOnScrollDownNeverInsertsAdBeforeUpsell() {
        setEdgeVisiblePosition(5);
        setStreamItems(10, TRACK_ITEM);
        when(adapter.getItem(6)).thenReturn(GO_UPSELL_ITEM);

        boolean inserted = inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(7, APP_INSTALL_ITEM);
    }

    @Test
    public void insertAdOnScrollUpNeverInsertsAdBeforeUpsell() {
        setEdgeVisiblePosition(8);
        setStreamItems(10, TRACK_ITEM);
        when(adapter.getItem(7)).thenReturn(GO_UPSELL_ITEM);

        boolean inserted = inlayAdHelper.insertAd(APP_INSTALL, SCROLLING_UP);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(6, APP_INSTALL_ITEM);
    }

    @Test
    public void ensureThatMaxSearchDistanceIsGreaterThanMinDistanceBetweenAds() {
        assertThat(InlayAdHelper.MAX_SEARCH_DISTANCE).isGreaterThan(InlayAdHelper.MIN_DISTANCE_BETWEEN_ADS);
    }

    @Test
    public void onScrollTracksVisibleIndices() {
        setEdgeVisiblePosition(99, 101);

        assertThat(visibleRange(inlayAdHelper)).isEqualTo(Pair.of(-1, -1));
        inlayAdHelper.onScroll();
        assertThat(visibleRange(inlayAdHelper)).isEqualTo(Pair.of(99, 101));
    }

    private Pair<Integer, Integer> visibleRange(InlayAdHelper helper) {
        return Pair.of(helper.minimumVisibleIndex, helper.maximumVisibleIndex);
    }

    @Test
    public void onScrollFiresOnScreenEventsForUntrackedEvents() {
        final AppInstallAd untracked = appInstall();
        final AppInstallAd tracked = appInstall();
        tracked.setImpressionReported();

        setEdgeVisiblePosition(7, 10);
        setStreamItems(15, TRACK_ITEM);
        when(adapter.getItem(8)).thenReturn(StreamItem.forAppInstall(tracked));
        when(adapter.getItem(10)).thenReturn(StreamItem.forAppInstall(untracked));

        inlayAdHelper.onScroll();

        verify(eventBus).publish(EventQueue.INLAY_AD, InlayAdEvent.OnScreen.create(10, untracked, CURRENT_DATE));
    }

    @Test
    public void onScrollFiresOnScreenEventWhenVideoAdOnScreen() {
        setEdgeVisiblePosition(7, 10);
        setStreamItems(15, TRACK_ITEM);
        when(adapter.getItem(8)).thenReturn(VIDEO_AD_ITEM);

        inlayAdHelper.onScroll();

        verify(eventBus).publish(EventQueue.INLAY_AD, InlayAdEvent.OnScreen.create(8, VIDEO_AD_ITEM.getAdData().get(), CURRENT_DATE));
    }

    @Test
    public void onScrollFiresOnScreenEventForFirstMostVisibleVideoAdOnScreen() {
        setEdgeVisiblePosition(7, 10);
        setStreamItems(15, TRACK_ITEM);
        when(adapter.getItem(8)).thenReturn(VIDEO_AD_ITEM);
        when(adapter.getItem(9)).thenReturn(StreamItem.forVideoAd(AdFixtures.getVideoAd(23L)));

        inlayAdHelper.onScroll();

        verify(eventBus).publish(EventQueue.INLAY_AD, InlayAdEvent.OnScreen.create(8, VIDEO_AD_ITEM.getAdData().get(), CURRENT_DATE));
    }

    @Test
    public void onScrollFiresNoVideoOnScreenEventWhenNoVideoAdsOnScreen() {
        setEdgeVisiblePosition(7, 10);
        setStreamItems(15, TRACK_ITEM);

        inlayAdHelper.onScroll();

        verify(eventBus).publish(EventQueue.INLAY_AD, InlayAdEvent.NoVideoOnScreen.create(CURRENT_DATE));
    }

    @Test
    public void isOnScreenUsesLastStoredVisibleIndicesInclusive() {
        setEdgeVisiblePosition(5, 6);
        inlayAdHelper.onScroll();

        setEdgeVisiblePosition(7, 10);
        inlayAdHelper.onScroll();

        assertThat(inlayAdHelper.isOnScreen(6)).isFalse();
        assertThat(inlayAdHelper.isOnScreen(7)).isTrue();
        assertThat(inlayAdHelper.isOnScreen(10)).isTrue();
        assertThat(inlayAdHelper.isOnScreen(11)).isFalse();
    }

    @Test
    public void insertsVideoAds() {
        setEdgeVisiblePosition(6);
        setStreamItems(10, TRACK_ITEM);

        boolean inserted = inlayAdHelper.insertAd(VIDEO_AD, SCROLLING_DOWN);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(7, VIDEO_AD_ITEM);
    }

    private void setEdgeVisiblePosition(int position) {
        setEdgeVisiblePosition(position, position);
    }

    private void setEdgeVisiblePosition(int firstPosition, int lastPosition) {
        int[] firstEdge = new int[]{firstPosition};
        int[] lastEdge = new int[]{lastPosition};

        when(layoutManager.findFirstVisibleItemPositions(any(int[].class))).thenReturn(firstEdge);
        when(layoutManager.findLastVisibleItemPositions(any(int[].class))).thenReturn(lastEdge);
    }

    private void setStreamItems(int size, StreamItem item) {
        for (int i = 0; i < size; i++)  {
            when(adapter.getItem(i)).thenReturn(item);
        }
        setItemListSize(size);
    }

    private void setItemListSize(int size) {
        when(list.size()).thenReturn(size);
        when(adapter.getItems()).thenReturn(list);
    }
}