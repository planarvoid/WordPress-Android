package com.soundcloud.android.paging;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.adapter.EndlessPagingAdapter;
import com.soundcloud.android.fragment.behavior.PagingAdapterViewAware;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.Fragment;

@RunWith(SoundCloudTestRunner.class)
public class PageItemObserverTest {

    private PageItemObserver observer;
    private Fragment fragment;

    @Mock
    private EndlessPagingAdapter adapter;
    @Mock
    private EmptyListView emptyListView;

    @Before
    public void setUp() throws Exception {
        fragment = mock(Fragment.class, withSettings().extraInterfaces(PagingAdapterViewAware.class));
        when(fragment.isAdded()).thenReturn(true);
        when(((PagingAdapterViewAware) fragment).getAdapter()).thenReturn(adapter);
        when(((PagingAdapterViewAware) fragment).getEmptyView()).thenReturn(emptyListView);

        observer = new PageItemObserver(fragment);
    }

    @Test
    public void testShowsErrorState() {
        observer.onError(fragment, new Exception());
        verify(emptyListView).setStatus(EmptyListView.Status.ERROR);
        verify(adapter).setDisplayProgressItem(false);
    }

    @Test
    public void testShowsEmptyState() {
        observer.onCompleted(fragment);
        verify(emptyListView).setStatus(EmptyListView.Status.OK);
        verify(adapter, never()).addItem(anyObject());
        verify(adapter).setDisplayProgressItem(false);
    }

    @Test
    public void testShowsContent() {
        observer.onNext(fragment, 1);
        observer.onCompleted(fragment);

        verify(emptyListView).setStatus(EmptyListView.Status.OK);
        verify(adapter, times(1)).addItem(1);
    }
}
