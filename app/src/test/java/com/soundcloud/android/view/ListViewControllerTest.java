package com.soundcloud.android.view;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collections.EndlessPagingAdapter;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;
import rx.observables.ConnectableObservable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;

@RunWith(SoundCloudTestRunner.class)
public class ListViewControllerTest {

    @InjectMocks
    private ListViewController controller;

    private Fragment fragment = new Fragment();
    private Bundle fragmentArgs = new Bundle();
    private ConnectableObservable observable = Observable.empty().publish();

    @Mock
    private EmptyViewController emptyViewController;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private ReactiveListComponent reactiveListComponent;
    @Mock
    private ListAdapter adapter;
    @Mock
    private View layout;
    @Mock
    private EmptyListView emptyView;
    @Mock
    private ListView listView;
    @Mock
    private AbsListView.OnScrollListener scrollListener;

    @Before
    public void setup() {
        fragment.setArguments(fragmentArgs);
        when(layout.findViewById(android.R.id.list)).thenReturn(listView);
        when(layout.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(emptyViewController.getEmptyView()).thenReturn(emptyView);
    }

    @Test
    public void shouldRegisterItemClickListenerWithListViewInOnViewCreated() {
        controller.onViewCreated(reactiveListComponent, observable, layout, adapter);
        verify(listView).setOnItemClickListener(reactiveListComponent);
    }

    @Test
    public void shouldSetEmptyViewForListViewInOnViewCreated() {
        controller.onViewCreated(reactiveListComponent, observable, layout, adapter);
        verify(listView).setEmptyView(emptyView);
    }

    @Test
    public void shouldSetAdapterForListViewInOnViewCreated() {
        controller.onViewCreated(reactiveListComponent, observable, layout, adapter);
        verify(listView).setAdapter(adapter);
    }

    @Test
    public void shouldSetAdapterForGridViewInOnViewCreated() {
        GridView gridView = mock(GridView.class);
        when(layout.findViewById(android.R.id.list)).thenReturn(gridView);
        controller.onViewCreated(reactiveListComponent, observable, layout, adapter);
        verify(gridView).setAdapter(adapter);
    }

    @Test
    public void shouldRegisterDefaultImageScrollPauseListenerWithListView() {
        when(imageOperations.createScrollPauseListener(false, true)).thenReturn(scrollListener);
        controller.onViewCreated(reactiveListComponent, observable, layout, adapter);
        verify(listView).setOnScrollListener(scrollListener);
    }

    @Test
    public void shouldRegisterEndlessPagingAdapterAsImageScrollPauseListenerWithListView() {
        EndlessPagingAdapter adapter = mock(EndlessPagingAdapter.class);
        when(imageOperations.createScrollPauseListener(false, true, adapter)).thenReturn(scrollListener);
        controller.onViewCreated(reactiveListComponent, observable, layout, adapter);
        verify(listView).setOnScrollListener(scrollListener);
    }

    @Test
    public void shouldDetachAdapterFromListViewInOnDestroyView() {
        controller.onViewCreated(reactiveListComponent, observable, layout, adapter);
        controller.onDestroyView();
        verify(listView).setAdapter(null);
    }

    @Test
    public void shouldReleaseListViewInOnDestroyView() {
        controller.onViewCreated(reactiveListComponent, observable, layout, adapter);
        controller.onDestroyView();
        expect(controller.getListView()).toBeNull();
    }
}