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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.BufferingObserver;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

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
        createAdapter(createPagingObservable(2, Observable.<Integer>never()));
        adapter.subscribe();
        adapter.loadNextPage();
        expect(adapter.getCount()).toBe(2); // 1 data item + 1 progress item
        expect(adapter.isEnabled(adapter.getCount() - 1)).toBeFalse(); // progress should not be clickable
    }

    @Test
    public void appendRowShouldBeErrorRowWhenLoadingData() {
        createAdapter(createPagingObservable(2, Observable.<Integer>error(new Exception("fail!"))));
        adapter.subscribe();
        adapter.loadNextPage();
        expect(adapter.getCount()).toBe(2); // 1 data item + 1 error row
        expect(adapter.isEnabled(adapter.getCount() - 1)).toBeTrue(); // error row should be interactive
    }

    @Test
    public void appendRowShouldBeGoneWhenNoErrorAndDoneLoading() {
        createAdapter(createPagingObservable(1));
        adapter.subscribe();
        expect(adapter.getCount()).toBe(1);
        expect(adapter.isEnabled(adapter.getCount() - 1)).toBeTrue(); // item rows should always be enabled
    }

    @Test
    public void shouldNotThrowWhenTryingToLoadNextPageWithoutANextPage() {
        createAdapter(createPagingObservable(1));
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
        createAdapter(createPagingObservable(1, Observable.<Integer>never()));
        adapter.subscribe();
        View itemView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(itemView).toBe(rowView);
    }

    @Test
    public void shouldCreateProgressRow() {
        createAdapter(createPagingObservable(2, Observable.<Integer>never()));
        adapter.subscribe();
        adapter.loadNextPage();
        View progressView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(progressView).not.toBeNull();
        expect(progressView.findViewById(R.id.loading).getVisibility()).toBe(View.VISIBLE);
        expect(progressView.findViewById(R.id.txt_list_loading_retry).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldCreateErrorRow() {
        createAdapter(createPagingObservable(2, Observable.<Integer>error(new Exception("fail!"))));
        adapter.subscribe();
        adapter.loadNextPage();
        View errorView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(errorView).not.toBeNull();
        expect(errorView.findViewById(R.id.loading).getVisibility()).toBe(View.GONE);
        expect(errorView.findViewById(R.id.txt_list_loading_retry).getVisibility()).toBe(View.VISIBLE);
    }

    @Test
    public void shouldConvertProgressRow() {
        createAdapter(createPagingObservable(2, Observable.<Integer>never()));
        adapter.subscribe();
        adapter.loadNextPage();
        View convertView = LayoutInflater.from(Robolectric.application).inflate(R.layout.list_loading_item, null);
        View progressView = adapter.getView(adapter.getCount() - 1, convertView, new FrameLayout(Robolectric.application));
        expect(progressView).toBe(convertView);
    }

    @Test
    @Ignore
    public void countShouldBeZeroIfLoadingAndNoItems() {
        createAdapter(createPagingObservable(1, Observable.<Integer>never()));
        adapter.subscribe();
        expect(adapter.getCount()).toBe(0);
    }

    @Test
    public void shouldRetryRequestWhenClickingOnErrorRow() {
        Observable retriedPage = pageObservable;
        createAdapter(createPagingObservable(2, retriedPage));

        adapter.subscribe();
        adapter.loadNextPage();
        adapter.onError(new Exception());

        View errorView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        errorView.performClick();

        verify(retriedPage, times(2)).subscribe(any(Observer.class));
    }

    @Test
    public void pageObserverShouldNotSubscribeDirectlyOnFirstPageLoad() {
        createAdapter(Observable.just(pageObservable));
        adapter.subscribe();
        verify(pageObservable, never()).subscribe(adapter);
    }

    @Test
    public void pageObserverShouldNeverSubscribeDirectlyOnSubsequentPageLoad() {
        createAdapter(createPagingObservable(2, pageObservable));
        adapter.subscribe();
        adapter.onScroll(absListView, 0, 5, 5);
        verify(pageObservable, never()).subscribe(adapter);
    }

    @Test
    public void pageScrollListenerShouldTriggerNextPageLoad() {
        createAdapter(createPagingObservable(2, pageObservable));
        adapter.subscribe();
        adapter.onScroll(absListView, 0, 5, 5);
        verify(pageObservable).subscribe(any(BufferingObserver.class));
        verify(observer).onNext(any());
    }

    @Test
    public void firstPageShouldUseSpecificItemObserver() {
        ListFragmentObserver specificObserver = mock(ListFragmentObserver.class);
        createAdapter(Observable.from(pageObservable));

        adapter.subscribe(specificObserver);

        ArgumentCaptor<BufferingObserver> captor = ArgumentCaptor.forClass(BufferingObserver.class);
        verify(pageObservable).subscribe(captor.capture());
        expect(captor.getValue().isWrapping(specificObserver)).toBeTrue();
    }

    @Test
    public void secondPageShouldUseTheDefaultItemObserver() {
        ListFragmentObserver specificObserver = mock(ListFragmentObserver.class);
        createAdapter(createPagingObservable(2, pageObservable));
        adapter.subscribe(specificObserver);
        adapter.onScroll(absListView, 0, 5, 5);

        ArgumentCaptor<BufferingObserver> captor = ArgumentCaptor.forClass(BufferingObserver.class);
        verify(pageObservable).subscribe(captor.capture());
        expect(captor.getValue().isWrapping(adapter)).toBeTrue();
    }

    @Test
    public void pageScrollListenerShouldLoadNextPageWithOnePageLookAhead() {
        createAdapter(createPagingObservable(2, pageObservable));
        adapter.subscribe();
        adapter.onScroll(absListView, 0, 5, 2 * 5);

        ArgumentCaptor<BufferingObserver> captor = ArgumentCaptor.forClass(BufferingObserver.class);
        verify(pageObservable).subscribe(captor.capture());
        expect(captor.getValue().isWrapping(adapter)).toBeTrue();
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfAlreadyLoading() {
        final Observable pageObservable = mock(Observable.class);
        when(pageObservable.observeOn(any(Scheduler.class))).thenReturn(pageObservable);
        createAdapter(createPagingObservable(2, pageObservable));
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

    @Test
    public void onCompleteInPagingObserverShouldSetNextPageObservableToNull() {
        createAdapter(Observable.from(Observable.just(new Track(1)), Observable.just(new Track(2))));
        adapter.subscribe();
        expect(adapter.hasMorePages()).toBeFalse();
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

    private Observable<Observable<Integer>> createPagingObservable(final int numPages, final Observable<Integer> lastPageObservable) {
        return Observable.create(new Func1<Observer<Observable<Integer>>, Subscription>() {
            @Override
            public Subscription call(Observer<Observable<Integer>> observableObserver) {
                observableObserver.onNext(nextPageObservable(numPages, 1, observableObserver, lastPageObservable));
                return Subscriptions.empty();
            }
        });
    }

    private Observable<Observable<Integer>> createPagingObservable(final int numPages) {
        return createPagingObservable(numPages, null);
    }

    private Observable<Integer> nextPageObservable(final int numPages, final int currentPage, final Observer<Observable<Integer>> pageObserver,
                                                   final Observable<Integer> lastPageObservable) {
        return Observable.create(new Func1<Observer<Integer>, Subscription>() {
            @Override
            public Subscription call(Observer<Integer> observer) {
                observer.onNext(1);
                observer.onCompleted();
                if (currentPage == numPages) {
                    pageObserver.onCompleted();
                } else if (numPages - currentPage == 1) {
                    // last page
                    if (lastPageObservable != null) {
                        pageObserver.onNext(lastPageObservable);
                    } else {
                        pageObserver.onNext(nextPageObservable(numPages, currentPage + 1, pageObserver, lastPageObservable));
                    }
                }
                return Subscriptions.empty();
            }
        });
    }
}
