package com.soundcloud.android.view;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;
import rx.android.LegacyPager;
import rx.observables.ConnectableObservable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ListViewControllerTest {

    @InjectMocks
    private ListViewController controller;

    private Fragment fragment = new Fragment();
    private Bundle fragmentArgs = new Bundle();
    private ConnectableObservable observable = Observable.empty().publish();

    @Mock private EmptyViewController emptyViewController;
    @Mock private ImageOperations imageOperations;
    @Mock private ReactiveListComponent reactiveListComponent;
    @Mock private ListAdapter adapter;
    @Mock private View view;
    @Mock private Bundle bundle;
    @Mock private EmptyView emptyView;
    @Mock private ListView listView;
    @Mock private AbsListView.OnScrollListener scrollListener;
    @Mock private LegacyPager pager;
    @Mock private PagingItemAdapter pagedAdapter;

    @Before
    public void setup() {
        fragment.setArguments(fragmentArgs);
        when(view.findViewById(android.R.id.list)).thenReturn(listView);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(emptyViewController.getEmptyView()).thenReturn(emptyView);
        controller.setAdapter(adapter);
    }

    @Test
    public void shouldDelegateOnViewCreatedToEmptyViewController() {
        controller.onViewCreated(fragment, view, bundle);
        verify(emptyViewController).onViewCreated(fragment, view, bundle);
    }

    @Test
    public void shouldDelegateOnDestroyViewToEmptyViewController() {
        controller.onViewCreated(fragment, view, bundle);
        controller.onDestroyView(fragment);
        verify(emptyViewController).onDestroyView(fragment);
    }

    @Test
    public void shouldSetEmptyViewForListViewInOnViewCreated() {
        controller.onViewCreated(fragment, view, bundle);
        verify(listView).setEmptyView(emptyView);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowIfNoAdapterIsSetAtTheTimeOnViewCreatedIsCalled() {
        controller.setAdapter(null);
        controller.onViewCreated(fragment, view, bundle);
    }

    @Test
    public void shouldSetAdapterForListViewInOnViewCreated() {
        controller.onViewCreated(fragment, view, bundle);
        verify(listView).setAdapter(adapter);
    }

    @Test
    public void shouldSetAdapterForGridViewInOnViewCreated() {
        GridView gridView = mock(GridView.class);
        when(view.findViewById(android.R.id.list)).thenReturn(gridView);
        controller.onViewCreated(fragment, view, bundle);
        verify(gridView).setAdapter(adapter);
    }

    @Test
    public void shouldSetDefaultImageScrollListenerInOnViewCreated() {
        when(imageOperations.createScrollPauseListener(false, true)).thenReturn(scrollListener);

        controller.onViewCreated(fragment, view, bundle);

        verify(listView).setOnScrollListener(scrollListener);
    }

    @Test
    public void shouldSetCustomScrollListenerAsImageScrollListenerInOnViewCreated() {
        when(imageOperations.createScrollPauseListener(false, true, scrollListener)).thenReturn(scrollListener);
        controller.setScrollListener(scrollListener);

        controller.onViewCreated(fragment, view, bundle);

        verify(listView).setOnScrollListener(scrollListener);
    }

    @Test
    public void shouldRegisterListComponentAsItemClickListenerWithListViewOnConnect() {
        controller.onViewCreated(fragment, view, bundle);
        controller.connect(reactiveListComponent, observable);
        verify(listView).setOnItemClickListener(reactiveListComponent);
    }

    @Test
    public void shouldRegisterOnScrollListenerForPagedEndlessListsInOnViewCreated() {
        controller.setAdapter(pagedAdapter, pager);
        controller.onViewCreated(fragment, view, bundle);

        verify(listView).setOnScrollListener(isA(AbsListView.OnScrollListener.class));
    }

    @Test
    public void shouldRegisterOnErrorRetryListenerForPagedEndlessLists() {
        when(pager.currentPage()).thenReturn(Observable.just(Arrays.asList("1", "2")));
        controller.setAdapter(pagedAdapter, pager);

        ArgumentCaptor<View.OnClickListener> listener = ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(pagedAdapter).setOnErrorRetryListener(listener.capture());

        listener.getValue().onClick(view);
        verify(pagedAdapter).setLoading();
        verify(pagedAdapter).onNext(Arrays.asList("1", "2"));
    }

    @Test
    public void shouldDetachAdapterFromListViewInOnDestroyView() {
        controller.onViewCreated(fragment, view, bundle);
        controller.onDestroyView(fragment);
        verify(listView).setAdapter(null);
    }

    @Test
    public void shouldReleaseListViewInOnDestroyView() {
        controller.onViewCreated(fragment, view, bundle);
        controller.onDestroyView(fragment);
        expect(controller.getListView()).toBeNull();
    }
}