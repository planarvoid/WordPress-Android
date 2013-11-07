package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static rx.android.OperationPaged.Page;
import static rx.android.OperationPaged.paged;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.OperationPaged;
import rx.android.concurrency.AndroidSchedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class EndlessPagingAdapterTest {

    @Mock
    private AbsListView absListView;
    @Mock
    private Observable mockObservable;
    @Mock
    private View rowView;

    private EndlessPagingAdapter adapter;


    @Before
    public void setup() {
        adapter = buildAdapter();
        when(mockObservable.observeOn(AndroidSchedulers.mainThread())).thenReturn(mockObservable);
    }

    @Test
    public void shouldReportAllItemsEnabledAsFalseSinceLoadingItemIsDisabled() {
        expect(adapter.areAllItemsEnabled()).toBeFalse();
    }

    @Test
    public void shouldAddAllItemsFromAnEmittedPage() {
        final Observable<Page<List<Parcelable>>> finish = OperationPaged.emptyPageObservable();

        loadFirstPageThen(finish);

        expect(adapter.getCount()).toBe(3);
    }

    @Test
    public void itemRowsShouldBeClickable() {
        final Observable<Page<List<Parcelable>>> finish = OperationPaged.emptyPageObservable();

        loadFirstPageThen(finish);

        expect(adapter.getCount()).toBeGreaterThan(0);
        for (int index = 0; index < adapter.getCount(); index++) {
            expect(adapter.isEnabled(index)).toBeTrue();
        }
    }

    @Test
    public void appendRowShouldBeProgressRowWhenLoadingData() {
        Observable<Page<List<Parcelable>>> neverReturn = Observable.never();

        loadFirstPageThen(neverReturn);

        expect(adapter.getCount()).toBe(4); // 3 data items + 1 progress item
        expect(adapter.isEnabled(adapter.getCount() - 1)).toBeFalse(); // progress should not be clickable
    }

    @Test
    public void appendRowShouldBeErrorRowWhenLoadingData() {
        Observable<Page<List<Parcelable>>> fail = Observable.error(new Exception("fail!"));

        loadFirstPageThen(fail);

        expect(adapter.getCount()).toBe(4); // 3 data items + 1 error row
        expect(adapter.isEnabled(adapter.getCount() - 1)).toBeFalse(); // error has a custom click handler
    }

    @Test
    public void errorRowShouldBeClickable() {
        Observable<Page<List<Parcelable>>> fail = Observable.error(new Exception("fail!"));

        loadFirstPageThen(fail);

        final View errorRow = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(Robolectric.shadowOf(errorRow).getOnClickListener()).not.toBeNull();
    }

    @Test
    public void shouldReturnImmediatelyWhenNoNextPageAvailable() {
        final Observable<Page<List<Parcelable>>> finish = OperationPaged.emptyPageObservable();

        Subscription subscription = loadFirstPageThen(finish);

        expect(subscription).toEqual(Subscriptions.empty());
        expect(adapter.getCount()).toBe(3); // 3 data items + NO progress item
    }

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldCreateItemRow() {
        final Observable<Page<List<Parcelable>>> finish = OperationPaged.emptyPageObservable();

        loadFirstPageThen(finish);

        View itemView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(itemView).toBe(rowView);
    }

    @Test
    public void shouldCreateProgressRow() {
        Observable<Page<List<Parcelable>>> neverReturn = Observable.never();

        loadFirstPageThen(neverReturn);

        View progressView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(progressView).not.toBeNull();
        expect(progressView.findViewById(R.id.list_loading_view).getVisibility()).toBe(View.VISIBLE);
        expect(progressView.findViewById(R.id.list_loading_retry_view).getVisibility()).toBe(View.GONE);
    }

    @Test
    public void shouldConvertProgressRow() {
        Observable<Page<List<Parcelable>>> neverReturn = Observable.never();

        loadFirstPageThen(neverReturn);

        View convertView = LayoutInflater.from(Robolectric.application).inflate(R.layout.list_loading_item, null);
        View progressView = adapter.getView(adapter.getCount() - 1, convertView, new FrameLayout(Robolectric.application));
        expect(progressView).toBe(convertView);
    }

    @Test
    public void shouldCreateErrorRow() {
        Observable<Page<List<Parcelable>>> fail = Observable.error(new Exception("fail!"));

        loadFirstPageThen(fail);

        View errorView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(errorView).not.toBeNull();
        expect(errorView.findViewById(R.id.list_loading_view).getVisibility()).toBe(View.GONE);
        expect(errorView.findViewById(R.id.list_loading_retry_view).getVisibility()).toBe(View.VISIBLE);
    }

    @Test
    public void shouldRetryRequestWhenClickingOnErrorRow() {
        Observable<Page<List<Parcelable>>> failedSequence = mockObservable;

        // loads page 1 successfully
        pagingObservable(sourceSequence(), failedSequence).subscribe(adapter);

        // loads page 2
        adapter.loadNextPage();
        adapter.onError(new Exception()); // call explicitly since the mock won't do it

        View errorView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        errorView.performClick();

        //TODO: this didn't pass when testing against the adapter instance, but looking at the references they
        //were actually identical? WTF?
        verify(failedSequence, times(2)).subscribe(any(Observer.class));
    }

    @Test
    public void pageScrollListenerShouldTriggerNextPageLoad() {
        Observable<Page<List<Parcelable>>> nextPage = mockObservable;
        pagingObservable(sourceSequence(), nextPage).subscribe(adapter);

        adapter.onScroll(absListView, 0, 5, 5);
        verify(nextPage).subscribe(any(Observer.class));
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfAlreadyLoading() {
        Observable<Page<List<Parcelable>>> nextPage = mockObservable;
        pagingObservable(sourceSequence(), nextPage).subscribe(adapter);
        adapter.loadNextPage();
        adapter.onScroll(absListView, 0, 5, 5); // should not trigger load again, already loading
        verify(nextPage, times(1)).subscribe(any(Observer.class));
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfLastAppendWasError() {
        Observable<Page<List<Parcelable>>> nextPage = mockObservable;
        pagingObservable(Observable.<List<Parcelable>>error(new Exception()), nextPage).subscribe(adapter);

        adapter.onScroll(absListView, 0, 5, 5);
        verifyZeroInteractions(mockObservable);
    }

    @Test
    public void pageScrollListenerShouldNotLoadNextPageIfZeroItems() {
        Observable<Page<List<Parcelable>>> nextPage = mockObservable;
        pagingObservable(Observable.<List<Parcelable>>empty(), nextPage).subscribe(adapter);

        adapter.onScroll(absListView, 0, 0, 0);
        verifyZeroInteractions(mockObservable);
    }

    private <T> Observable<Page<List<T>>> pagingObservable(Observable<List<T>> source, final Observable<Page<List<T>>> nextPage) {
        return Observable.create(paged(source, new Func1<List<T>, Observable<Page<List<T>>>>() {
            @Override
            public Observable<Page<List<T>>> call(List<T> objects) {
                return nextPage;
            }
        }));
    }

    private Subscription loadFirstPageThen(final Observable<Page<List<Parcelable>>> nextPage) {
        // loads page 1 successfully
        pagingObservable(sourceSequence(), nextPage).subscribe(adapter);

        // loads page 2
        return adapter.loadNextPage();
    }

    private EndlessPagingAdapter buildAdapter() {
        return new EndlessPagingAdapter(10) {
            @Override
            protected View createItemView(int position, ViewGroup parent) {
                return rowView;
            }

            @Override
            protected void bindItemView(int position, View itemView) {

            }
        };
    }

    private Observable<List<Parcelable>> sourceSequence() {
        return Observable.from(mock(Parcelable.class), mock(Parcelable.class), mock(Parcelable.class)).toList();
    }
}
