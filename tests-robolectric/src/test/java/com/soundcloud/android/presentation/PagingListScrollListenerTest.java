package com.soundcloud.android.presentation;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.Pager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.widget.AbsListView;

@RunWith(SoundCloudTestRunner.class)
public class PagingListScrollListenerTest {

    private PagingListScrollListener pagingScrollListener;

    @Mock private PagedCollectionBinding pagedCollectionBinding;
    @Mock private Pager pager;
    @Mock private CollectionViewPresenter presenter;
    @Mock private PagingAwareAdapter<?> adapter;
    @Mock private AbsListView.OnScrollListener listenerDelegate;
    @Mock private AbsListView absListView;

    @Before
    public void setUp() throws Exception {
        when(presenter.getBinding()).thenReturn(pagedCollectionBinding);
        when(pagedCollectionBinding.pager()).thenReturn(pager);
        pagingScrollListener = new PagingListScrollListener(presenter, adapter, listenerDelegate);
    }

    @Test
    public void callsOnScrolLStateChangedOnDelegate() throws Exception {
        pagingScrollListener.onScrollStateChanged(absListView, 1);

        verify(listenerDelegate).onScrollStateChanged(absListView, 1);
    }

    @Test
    public void callsOnScrollOnDelegate() throws Exception {
        pagingScrollListener.onScroll(absListView, 1, 2, 3);

        verify(listenerDelegate).onScroll(absListView, 1, 2, 3);
    }

    @Test
    public void doesNotPageWithNoItems() throws Exception {
        pagingScrollListener.onScroll(absListView, 0, 0, 0);

        verify(pager, never()).next();
        verify(adapter, never()).setLoading();
    }

    @Test
    public void doesNotPageIfNotWithinTwiceVisibleItemCount() throws Exception {
        pagingScrollListener.onScroll(absListView, 3, 3, 10);

        verify(pager, never()).next();
        verify(adapter, never()).setLoading();
    }

    @Test
    public void doesNotPageIfWithinTwiceVisibleItemCountAndAdapterIsNotIdle() throws Exception {
        pagingScrollListener.onScroll(absListView, 3, 3, 10);

        verify(pager, never()).next();
        verify(adapter, never()).setLoading();
    }

    @Test
    public void doesNotPageIfWithinTwiceVisibleItemCountAdapterIsIdleAndPageDoesNotHaveNext() throws Exception {
        when(adapter.isIdle()).thenReturn(true);
        pagingScrollListener.onScroll(absListView, 3, 3, 10);

        verify(pager, never()).next();
        verify(adapter, never()).setLoading();
    }

    @Test
    public void pagesIfWithinTwiceVisibleItemCountAdapterIsIdleAndPageHasNext() throws Exception {
        when(adapter.isIdle()).thenReturn(true);
        when(pager.hasNext()).thenReturn(true);
        pagingScrollListener.onScroll(absListView, 4, 3, 10);

        verify(pager).next();
        verify(adapter).setLoading();
    }
}