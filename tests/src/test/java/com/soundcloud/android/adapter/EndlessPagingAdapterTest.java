package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;

@RunWith(SoundCloudTestRunner.class)
public class EndlessPagingAdapterTest {

    @Mock
    private Observable pageEmittingObservable;
    @Mock(extraInterfaces = EmptyViewAware.class)
    private Fragment fragment;
    @Mock
    private AbsListView absListView;
    @Mock
    private Observable pageObservable;
    @Mock
    private ListFragmentObserver observer;
    @Mock
    private View rowView;
    private EndlessPagingAdapter<Track> adapter;


    @Before
    public void setup(){
        when(fragment.isAdded()).thenReturn(true);
        when (pageObservable.observeOn(AndroidSchedulers.mainThread())).thenReturn(pageObservable);
    }

    @Test
    public void shouldReportAllItemsEnabledAsFalseSinceLoadingItemIsDisabled() {
        createAdapter();
        expect(adapter.areAllItemsEnabled()).toBeFalse();
    }

    @Test
    public void appendRowShouldBeProgressRowWhenLoadingData() {
        createAdapter(Observable.from(Observable.just(1), Observable.never()));
        adapter.subscribe();
        adapter.loadNextPage();
        expect(adapter.getCount()).toBe(2); // 1 data item + 1 progress item
        expect(adapter.isEnabled(adapter.getCount() - 1)).toBeFalse(); // progress should not be clickable
    }

    @Test
    public void appendRowShouldBeErrorRowWhenLoadingData() {
        createAdapter(Observable.from(Observable.just(1), Observable.error(new Exception())));
        adapter.subscribe();
        adapter.loadNextPage();
        expect(adapter.getCount()).toBe(2); // 1 data item + 1 error row
        expect(adapter.isEnabled(adapter.getCount() - 1)).toBeTrue(); // error row should be interactive
    }

    @Test
    public void appendRowShouldBeGoneWhenNoErrorAndDoneLoading() {
        createAdapter(Observable.from(Observable.just(1)));
        adapter.subscribe();
        expect(adapter.getCount()).toBe(1);
        expect(adapter.isEnabled(adapter.getCount() - 1)).toBeTrue(); // item rows should always be enabled
    }

    @Test
    public void shouldNotThrowWhenTryingToLoadNextPageWithoutANextPage() {
        createAdapter(Observable.from(Observable.just(1)));
        adapter.subscribe();
        expect(adapter.loadNextPage()).toEqual(Subscriptions.empty());
    }

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        createAdapter();
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldCreateItemRow() {
        createAdapter(Observable.from(Observable.just(new Track(1))));
        adapter.subscribe();
        View itemView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(itemView).toBe(rowView);
    }

    @Test
    public void shouldCreateProgressRow() {
        createAdapter(Observable.from(Observable.just(new Track(1)), Observable.never()));
        adapter.subscribe();
        adapter.loadNextPage();
        View progressView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(progressView).not.toBeNull();
        expect(progressView.findViewById(R.id.loading).getVisibility()).toBe(View.VISIBLE);
        expect(progressView.findViewById(R.id.txt_list_loading_retry).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldCreateErrorRow() {
        createAdapter(Observable.from(Observable.just(new Track(1)), Observable.error(new Exception())));
        adapter.subscribe();
        adapter.loadNextPage();
        View errorView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(errorView).not.toBeNull();
        expect(errorView.findViewById(R.id.loading).getVisibility()).toBe(View.GONE);
        expect(errorView.findViewById(R.id.txt_list_loading_retry).getVisibility()).toBe(View.VISIBLE);
    }

    @Test
    public void shouldConvertProgressRow() {
        createAdapter(Observable.from(Observable.just(new Track(1)), Observable.never()));
        adapter.subscribe();
        adapter.loadNextPage();
        View convertView = LayoutInflater.from(Robolectric.application).inflate(R.layout.list_loading_item, null);
        View progressView = adapter.getView(adapter.getCount() - 1, convertView, new FrameLayout(Robolectric.application));
        expect(progressView).toBe(convertView);
    }

    @Test
    public void countShouldBeZeroIfLoadingAndNoItems() {
        createAdapter(Observable.from(Observable.empty(), Observable.never()));
        adapter.subscribe();
        adapter.loadNextPage();
        expect(adapter.getCount()).toBe(0);
    }

    @Test
    public void shouldSubscribeToFirstPageWithSpecificObserver() {
        ListFragmentObserver specificObserver = mock(ListFragmentObserver.class);
        createAdapter(Observable.just(pageObservable));
        adapter.subscribe(specificObserver);
        verify(pageObservable).subscribe(specificObserver);
        verify(pageObservable, never()).subscribe(adapter);
    }

    @Test
    public void shouldRetryRequestWhenClickingOnErrorRow() {
        Observable retriedPage = mock(Observable.class);
        when(retriedPage.observeOn(any(Scheduler.class))).thenReturn(retriedPage);
        createAdapter(Observable.from(Observable.just(new Track(1)), retriedPage));

        adapter.subscribe();
        adapter.loadNextPage();
        adapter.onError(new Exception());

        View errorView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        errorView.performClick();

        verify(retriedPage, times(2)).subscribe(any(Observer.class));
    }

    @Test
    public void pageScrollListenerShouldTriggerNextPageLoad() {
        Track track = new Track();
        createAdapter(Observable.from(Observable.just(track), pageObservable));
        adapter.subscribe();
        adapter.onScroll(absListView, 0, 5, 5);
        verify(pageObservable).subscribe(adapter);
        verify(observer).onNext(track);
    }

    @Test
    public void secondPageShouldUseTheDefaultItemObserver() {
        ListFragmentObserver specificObserver = mock(ListFragmentObserver.class);
        createAdapter(Observable.from(pageObservable, pageObservable));

        adapter.subscribe(specificObserver);
        verify(pageObservable).subscribe(specificObserver);

        adapter.onScroll(absListView, 0, 5, 5);
        verify(pageObservable).subscribe(adapter);
    }

    @Test
    public void pageScrollListenerShouldLoadNextPageWithOnePageLookAhead() {
        createAdapter(Observable.from(Observable.just(new Track()), pageObservable));
        adapter.subscribe();
        adapter.onScroll(absListView, 0, 5, 2 * 5);
        verify(pageObservable).subscribe(adapter);
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfAlreadyLoading() {
        final Observable pageObservable = mock(Observable.class);
        when(pageObservable.observeOn(any(Scheduler.class))).thenReturn(pageObservable);
        createAdapter(Observable.from(Observable.just(1), pageObservable));
        adapter.subscribe();
        adapter.loadNextPage();
        adapter.onScroll(absListView, 0, 5, 5); // should not trigger load again, already loading
        verify(pageObservable, times(1)).subscribe(any(Observer.class));
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfLastAppendWasError() {
        final Observable pageObservable = mock(Observable.class);
        when(pageObservable.observeOn(any(Scheduler.class))).thenReturn(pageObservable);
        createAdapter(Observable.from(Observable.error(new Exception()), pageObservable));
        adapter.subscribe();
        adapter.onScroll(absListView, 0, 5, 5);
        verifyZeroInteractions(pageObservable);
    }

    @Test
    public void pageScrollListenerShouldNotLoadNextPageIfZeroItems() {
        final Observable pageObservable = mock(Observable.class);
        createAdapter(Observable.from(pageObservable));
        adapter.onScroll(absListView, 0, 0, 0);
        verifyZeroInteractions(pageObservable);
    }

    private void createAdapter(){
        createAdapter(pageEmittingObservable);
    }

    private void createAdapter(Observable pageEmittingObservable){
        adapter = new EndlessPagingAdapter<Track>(pageEmittingObservable, observer, 10) {
            @Override
            protected void bindItemView(int position, View itemView) {

            }

            @Override
            protected View createItemView(int position, ViewGroup parent) {
                return rowView;
            }
        };
    }
}
