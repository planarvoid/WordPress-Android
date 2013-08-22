package com.soundcloud.android.fragment;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.adapter.PlaylistTracksAdapter2;
import com.soundcloud.android.adapter.ScBaseAdapter;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.view.EmptyListView;
import com.soundcloud.android.view.ScListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@RunWith(DefaultTestRunner.class)
public class ReactiveListFragmentTest {

    private ReactiveListFragment<Track> fragment;
    private @Mock Observer<Track> mockObserver;

    @Before
    public void setup() {
        fragment = new ReactiveListFragment<Track>() {

            final Observable<Track> nextPageObservable = mock(Observable.class);

            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                Context context = Robolectric.application;
                inflater = mock(LayoutInflater.class);
                ViewGroup mockLayout = mock(ViewGroup.class);
                when(inflater.inflate(R.layout.basic_list_fragment, container, false)).thenReturn(mockLayout);
                when(mockLayout.findViewById(android.R.id.empty)).thenReturn(new EmptyListView(context));
                when(mockLayout.findViewById(R.id.list)).thenReturn(mock(ScListView.class));
                return super.onCreateView(inflater, null, null);
            }

            @Override
            protected ScBaseAdapter<Track> newAdapter() {
                return new PlaylistTracksAdapter2(Content.PLAYLIST_TRACKS.uri);
            }

            @Override
            protected void configureEmptyListView(EmptyListView emptyView) {
            }

            @Override
            protected Observable<Track> getLoadNextPageObservable() {
                return nextPageObservable;
            }
        };
    }

    @Test
    public void shouldNotExecuteAnythingByDefault() {
        fragment.onCreate(null);
        fragment.onStart();

        expect(fragment.hasPendingObservables()).toBeFalse();
        verifyZeroInteractions(mockObserver);
    }

    @Test
    public void shouldExecuteFirstPendingObservableToLoadListItems() {
        fragment.onCreate(null);
        final Observable<Track> trackObservable = mock(Observable.class);

        Observable<Observable<Track>> observable1 = Observable.just(trackObservable);
        Observable<Observable<Track>> observable2 = mock(Observable.class);
        fragment.addPendingObservable(observable1);
        fragment.addPendingObservable(observable2);

        expect(fragment.hasPendingObservables()).toBeTrue();

        fragment.onStart();

        verify(trackObservable).subscribe(fragment.mLoadItemsObserver);
        verify(observable2, never()).subscribe(any());
        expect(fragment.hasPendingObservables()).toBeFalse();
    }

    @Test
    public void shouldAddNextEmittedItemToAdapter() {
        fragment.onCreate(null);
        fragment.onStart();

        expect(fragment.getListAdapter().isEmpty()).toBeTrue();
        fragment.mLoadItemsObserver.onNext(new Track());
        expect(fragment.getListAdapter().isEmpty()).toBeFalse();
    }

    @Test
    public void shouldShowEmptyViewIfNoItemsReceived() {
        fragment.onCreate(null);
        fragment.onCreateView(null, null, null);
        fragment.onStart();

        expect(fragment.getListAdapter().isEmpty()).toBeTrue();
        fragment.mLoadItemsObserver.onCompleted();
        expect(fragment.mEmptyView.getVisibility()).toBe(View.VISIBLE);
        expect(fragment.mEmptyView.getStatus()).toEqual(EmptyListView.Status.OK);
    }

    @Test
    public void shouldHideEmptyViewIfNewItemsReceived() {
        fragment.onCreate(null);
        fragment.onCreateView(null, null, null);
        fragment.onStart();

        expect(fragment.getListAdapter().isEmpty()).toBeTrue();
        fragment.mLoadItemsObserver.onNext(new Track());
        fragment.mLoadItemsObserver.onCompleted();
        expect(fragment.mEmptyView.getVisibility()).toBe(View.GONE);
        expect(fragment.getListAdapter().isEmpty()).toBeFalse();
    }

    @Test
    public void shouldShowEmptyViewWithErrorIfErrorOccurred() {
        fragment.onCreate(null);
        fragment.onCreateView(null, null, null);
        fragment.onStart();

        fragment.mLoadItemsObserver.onError(new Exception());
        expect(fragment.mEmptyView.getVisibility()).toBe(View.VISIBLE);
        expect(fragment.mEmptyView.getStatus()).toEqual(EmptyListView.Status.CONNECTION_ERROR);
    }

    @Test
    public void shouldNotAttemptToLoadNextPageWhenAdapterIsEmpty() {
        fragment.mAdapter = mock(ScBaseAdapter.class);
        when(fragment.mAdapter.isEmpty()).thenReturn(true);

        fragment.onScroll(null, 0, 0, 0);

        verify(fragment.mAdapter, never()).shouldRequestNextPage(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void shouldInvokeLoadNextPageObservableWhenReachingListBottom() {
        fragment.onCreate(null);

        fragment.mAdapter = mock(ScBaseAdapter.class);
        when(fragment.mAdapter.isEmpty()).thenReturn(false);
        when(fragment.mAdapter.shouldRequestNextPage(anyInt(), anyInt(), anyInt())).thenReturn(true);

        fragment.onScroll(null, 0, 0, 0);

        verify(fragment.getLoadNextPageObservable()).subscribe(fragment.mLoadItemsObserver);
        verify(fragment.mAdapter).setIsLoadingData(true);
    }
}
