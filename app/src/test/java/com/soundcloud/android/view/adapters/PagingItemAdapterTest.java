package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.MockObservable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.pagedWith;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.android.OperatorPaged;
import rx.subscriptions.Subscriptions;

import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PagingItemAdapterTest {

    @Mock
    private CellPresenter cellPresenter;
    @Mock
    private AbsListView absListView;
    @Mock
    private View rowView;

    private PagingItemAdapter adapter;


    @Before
    public void setup() {
        adapter = new PagingItemAdapter(cellPresenter, 10);
    }

    @Test
    public void shouldReportAllItemsEnabledAsFalseSinceLoadingItemIsDisabled() {
        expect(adapter.areAllItemsEnabled()).toBeFalse();
    }

    @Test
    public void shouldAddAllItemsFromAnEmittedPage() {
        final Observable<Page<List<Parcelable>>> finish = OperatorPaged.emptyObservable();

        loadFirstPageThen(finish);

        expect(adapter.getCount()).toBe(3);
    }

    @Test
    public void itemRowsShouldBeClickable() {
        final Observable<Page<List<Parcelable>>> finish = OperatorPaged.emptyObservable();

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
    public void appendRowShouldBeErrorRowWhenLoadingDataFails() {
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
        final Observable<Page<List<Parcelable>>> finish = OperatorPaged.emptyObservable();

        Subscription subscription = loadFirstPageThen(finish);

        expect(subscription).toEqual(Subscriptions.empty());
        expect(adapter.getCount()).toBe(3); // 3 data items + NO progress item
    }

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldCreateNormalItemRowUsingPresenter() {
        final FrameLayout parent = new FrameLayout(Robolectric.application);
        final Observable<Page<List<Parcelable>>> finish = OperatorPaged.emptyObservable();

        loadFirstPageThen(finish);

        adapter.getView(0, null, parent);
        verify(cellPresenter).createItemView(0, parent, ItemAdapter.DEFAULT_ITEM_VIEW_TYPE);
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
        MockObservable<Page<List<Parcelable>>> failedSequence = TestObservables.errorObservable(new Exception());

        // loads page 1 successfully
        pagingObservable(sourceSequence(), failedSequence).subscribe(adapter);

        // loads page 2 (will fail)
        adapter.loadNextPage();

        View errorView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        errorView.performClick();

        expect(failedSequence.subscribers()).toNumber(2);
    }

    @Test
    public void pageScrollListenerShouldTriggerNextPageLoad() {
        MockObservable<Page<List<Parcelable>>> nextPage = TestObservables.emptyObservable();
        pagingObservable(sourceSequence(), nextPage).subscribe(adapter);

        adapter.onScroll(absListView, 0, 5, 5);
        expect(nextPage.subscribedTo()).toBeTrue();
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfAlreadyLoading() {
        MockObservable<Page<List<Parcelable>>> nextPage = TestObservables.emptyObservable();
        pagingObservable(sourceSequence(), nextPage).subscribe(adapter);
        adapter.loadNextPage();
        adapter.onScroll(absListView, 0, 5, 5); // should not trigger load again, already loading
        expect(nextPage.subscribers()).toNumber(1);
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfLastAppendWasError() {
        MockObservable<Page<List<Parcelable>>> nextPage = TestObservables.emptyObservable();
        pagingObservable(Observable.<List<Parcelable>>error(new Exception()), nextPage).subscribe(adapter);

        adapter.onScroll(absListView, 0, 5, 5);
        expect(nextPage.subscribers()).toBeEmpty();
    }

    @Test
    public void pageScrollListenerShouldNotLoadNextPageIfZeroItems() {
        MockObservable<Page<List<Parcelable>>> nextPage = TestObservables.emptyObservable();
        pagingObservable(Observable.<List<Parcelable>>empty(), nextPage).subscribe(adapter);

        adapter.onScroll(absListView, 0, 0, 0);
        expect(nextPage.subscribers()).toBeEmpty();
    }

    private <T> Observable<Page<List<T>>> pagingObservable(Observable<List<T>> source, final Observable<Page<List<T>>> nextPage) {
        return source.lift(pagedWith(new OperatorPaged.Pager<List<T>>() {
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

    private Observable<List<Parcelable>> sourceSequence() {
        return Observable.from(Arrays.asList(mock(Parcelable.class), mock(Parcelable.class), mock(Parcelable.class))).toList();
    }
}
