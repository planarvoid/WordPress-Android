package com.soundcloud.android.presentation;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.rx.Pager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

@RunWith(MockitoJUnitRunner.class)
public class PagingRecyclerScrollListenerTest {

    private PagingRecyclerScrollListener pagingScrollListener;

    @Mock private PagedCollectionBinding pagedCollectionBinding;
    @Mock private Pager pager;
    @Mock private CollectionViewPresenter presenter;
    @Mock private PagingAwareAdapter<?> adapter;
    @Mock private RecyclerView recyclerView;
    @Mock private LinearLayoutManager layoutManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(presenter.getBinding()).thenReturn(pagedCollectionBinding);
        when(pagedCollectionBinding.pager()).thenReturn(pager);
        pagingScrollListener = new PagingRecyclerScrollListener(presenter, adapter, layoutManager, 2);
    }

    @Test
    public void doesNotPageWithNoItems() throws Exception {
        setupCounts(0, 0, 0);
        pagingScrollListener.onScrolled(recyclerView, 0, 0);

        verify(pager, never()).next();
        verify(adapter, never()).setLoading();
    }

    @Test
    public void doesNotPageIfNotWithinTwiceVisibleItemCount() throws Exception {
        setupCounts(3, 3, 10);
        pagingScrollListener.onScrolled(recyclerView, 0, 0);

        verify(pager, never()).next();
        verify(adapter, never()).setLoading();
    }

    @Test
    public void doesNotPageIfWithinTwiceVisibleItemCountAndAdapterIsNotIdle() throws Exception {
        setupCounts(3, 4, 10);
        pagingScrollListener.onScrolled(recyclerView, 0, 0);

        verify(pager, never()).next();
        verify(adapter, never()).setLoading();
    }

    @Test
    public void doesNotPageIfWithinTwiceVisibleItemCountAdapterIsIdleAndPageDoesNotHaveNext() throws Exception {
        setupCounts(3, 4, 10);
        when(adapter.isIdle()).thenReturn(true);
        pagingScrollListener.onScrolled(recyclerView, 0, 0);

        verify(pager, never()).next();
        verify(adapter, never()).setLoading();
    }

    @Test
    public void pagesIfWithinTwiceVisibleItemCountAdapterIsIdleAndPageHasNext() throws Exception {
        setupCounts(3, 4, 10);
        when(adapter.isIdle()).thenReturn(true);
        when(pager.hasNext()).thenReturn(true);
        pagingScrollListener.onScrolled(recyclerView, 0, 0);

        verify(pager).next();
        verify(adapter).setLoading();
    }

    private void setupCounts(int firstVisibleItem, int totalVisibleItems, int totalItems) {
        when(layoutManager.findFirstVisibleItemPosition()).thenReturn(firstVisibleItem);
        when(layoutManager.getChildCount()).thenReturn(totalVisibleItems);
        when(layoutManager.getItemCount()).thenReturn(totalItems);
    }
}