package com.soundcloud.android.ads;

import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.stream.StreamAdapter;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InlayAdInsertionHelperTest extends AndroidUnitTest {

    private static final boolean SCROLLING_UP = true;
    private static final boolean SCROLLING_DOWN = false;
    private static final AppInstallAd APP_INSTALL = AppInstallAd.create(AdFixtures.getApiAppInstall());
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

    private InlayAdInsertionHelper insertionHelper;

    @Before
    public void setUp() {
        insertionHelper = new InlayAdInsertionHelper(layoutManager, adapter);
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

        assertThat(insertionHelper.insertAd(APP_INSTALL, SCROLLING_DOWN)).isFalse();
        verify(adapter, never()).addItem(anyInt(), any(StreamItem.class));
    }

    @Test
    public void insertAdOnScrollUpWithOnEmptyAdapterFailsToInsertAd() {
        setEdgeVisiblePosition(0);
        setStreamItems(0, TRACK_ITEM);

        assertThat(insertionHelper.insertAd(APP_INSTALL, SCROLLING_UP)).isFalse();
    }

    @Test
    public void insertAdOnScrollDownWithPlayableItemsInsertsAd() {
        setEdgeVisiblePosition(6);
        setStreamItems(10, TRACK_ITEM);

        boolean inserted = insertionHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(7, APP_INSTALL_ITEM);
    }

    @Test
    public void insertAdOnScrollUpWithPlayableItemsInsertsAd() {
        setEdgeVisiblePosition(6);
        setStreamItems(10, TRACK_ITEM);

        boolean inserted = insertionHelper.insertAd(APP_INSTALL, SCROLLING_UP);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(5, APP_INSTALL_ITEM);
    }

    @Test
    public void insertAdOnScrollUpNeverInsertsAdBeforeEarliestPositionForAd() {
        setEdgeVisiblePosition(5);
        setStreamItems(10, TRACK_ITEM);

        boolean inserted = insertionHelper.insertAd(APP_INSTALL, SCROLLING_UP);

        assertThat(inserted).isFalse();
        verify(adapter, never()).addItem(anyInt(), any(StreamItem.class));
    }

    @Test
    public void insertAdOnScrollDownPlayableItemsInsertsAdAfterEarliestPositionForAd() {
        setEdgeVisiblePosition(2);
        setStreamItems(10, TRACK_ITEM);

        boolean inserted = insertionHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

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

        boolean inserted = insertionHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

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

        boolean inserted = insertionHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isFalse();
        verify(adapter, never()).addItem(anyInt(), any(StreamItem.class));
    }

    @Test
    public void insertAdTerminatesIfItCanNotFindSpaceForAdWithinMaxSearchDistance() {
        setEdgeVisiblePosition(0);
        setItemListSize(Integer.MAX_VALUE);
        when(adapter.getItem(anyInt())).thenReturn(APP_INSTALL_ITEM);

        boolean inserted = insertionHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isFalse();
    }

    @Test
    public void insertAdOnScrollDownNeverInsertsAdBeforeUpsell() {
        setEdgeVisiblePosition(5);
        setStreamItems(10, TRACK_ITEM);
        when(adapter.getItem(6)).thenReturn(GO_UPSELL_ITEM);

        boolean inserted = insertionHelper.insertAd(APP_INSTALL, SCROLLING_DOWN);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(7, APP_INSTALL_ITEM);
    }

    @Test
    public void insertAdOnScrollUpNeverInsertsAdBeforeUpsell() {
        setEdgeVisiblePosition(8);
        setStreamItems(10, TRACK_ITEM);
        when(adapter.getItem(7)).thenReturn(GO_UPSELL_ITEM);

        boolean inserted = insertionHelper.insertAd(APP_INSTALL, SCROLLING_UP);

        assertThat(inserted).isTrue();
        verify(adapter).addItem(6, APP_INSTALL_ITEM);
    }

    @Test
    public void ensureThatMaxSearchDistanceIsGreaterThanMinDistanceBetweenAds() {
        assertThat(InlayAdInsertionHelper.MAX_SEARCH_DISTANCE).isGreaterThan(InlayAdInsertionHelper.MIN_DISTANCE_BETWEEN_ADS);
    }

    private void setEdgeVisiblePosition(int position) {
        int[] edgeVisiblePositions = new int[]{position};
        when(layoutManager.findFirstVisibleItemPositions(any(int[].class))).thenReturn(edgeVisiblePositions);
        when(layoutManager.findLastVisibleItemPositions(any(int[].class))).thenReturn(edgeVisiblePositions);
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