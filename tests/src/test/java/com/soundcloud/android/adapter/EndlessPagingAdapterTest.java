package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ItemObserver;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.subscriptions.Subscriptions;

import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class EndlessPagingAdapterTest {

    @Mock
    Observable pageEmittingObservable;
    @Mock(extraInterfaces = AdapterViewAware.class)
    private Fragment fragment;
    @Mock
    private AbsListView absListView;
    @Mock
    private Observable pageObservable;

    private ItemObserver itemObserver;
    private EndlessPagingAdapter<Track> adapter;


    @Before
    public void setup(){
        when(fragment.isAdded()).thenReturn(true);
        when (pageObservable.observeOn(ScSchedulers.UI_SCHEDULER)).thenReturn(pageObservable);
        itemObserver = new ItemObserver(fragment);
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
    public void shouldCreateProgressView() {
        createAdapter(Observable.from(Observable.just(new Track(1)), Observable.never()));
        adapter.subscribe();
        adapter.loadNextPage();
        View progressView = adapter.getView(adapter.getCount() - 1, null, new FrameLayout(Robolectric.application));
        expect(progressView).not.toBeNull();
        expect(progressView.findViewById(R.id.list_loading)).not.toBeNull();
    }

    @Test
    public void shouldConvertProgressView() {
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
        ItemObserver specificObserver = Mockito.mock(ItemObserver.class);
        createAdapter(Observable.just(pageObservable));
        adapter.subscribe(specificObserver);
        verify(pageObservable).subscribe(specificObserver);
        verify(pageObservable, never()).subscribe(itemObserver);
    }

    @Test
    public void pageScrollListenerShouldTriggerNextPageLoad() {
        createAdapter(Observable.from(Observable.just(new Track()), pageObservable));
        adapter.subscribe();
        adapter.onScroll(absListView, 0, 5, 5);
        verify(pageObservable).subscribe(itemObserver);
    }

    @Test
    public void secondPageShouldUseTheDefaultItemObserver() {
        ItemObserver specificObserver = Mockito.mock(ItemObserver.class);
        createAdapter(Observable.from(pageObservable, pageObservable));

        adapter.subscribe(specificObserver);
        verify(pageObservable).subscribe(specificObserver);

        adapter.onScroll(absListView, 0, 5, 5);
        verify(pageObservable).subscribe(itemObserver);
    }

    @Test
    public void pageScrollListenerShouldLoadNextPageWithOnePageLookAhead() {
        createAdapter(Observable.from(Observable.just(new Track()), pageObservable));
        adapter.subscribe();
        adapter.onScroll(absListView, 0, 5, 2 * 5);
        verify(pageObservable).subscribe(itemObserver);
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfAlreadyLoading() {
        final Observable pageObservable = Mockito.mock(Observable.class);
        when(pageObservable.observeOn(any(Scheduler.class))).thenReturn(pageObservable);
        createAdapter(Observable.from(Observable.just(1), pageObservable));
        adapter.subscribe();
        adapter.loadNextPage();
        adapter.onScroll(absListView, 0, 5, 5); // should not trigger load again, already loading
        verify(pageObservable, times(1)).subscribe(any(Observer.class));
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfLastAppendWasError() {
        final Observable pageObservable = Mockito.mock(Observable.class);
        when(pageObservable.observeOn(any(Scheduler.class))).thenReturn(pageObservable);
        createAdapter(Observable.from(Observable.error(new Exception()), pageObservable));
        adapter.subscribe();
        adapter.onScroll(absListView, 0, 5, 5);
        verifyZeroInteractions(pageObservable);
    }

    @Test
    public void pageScrollListenerShouldNotLoadNextPageIfZeroItems() {
        final Observable pageObservable = Mockito.mock(Observable.class);
        createAdapter(Observable.from(pageObservable));
        adapter.onScroll(absListView, 0, 0, 0);
        verifyZeroInteractions(pageObservable);
    }

    private void createAdapter(){
        createAdapter(pageEmittingObservable);
    }

    private void createAdapter(Observable pageEmittingObservable){
        adapter = new EndlessPagingAdapter<Track>(pageEmittingObservable, itemObserver, 10) {
            @Override
            protected void bindItemView(int position, View itemView) {
                ((TextView) itemView).setText(getItem(position).getTitle());
            }

            @Override
            protected View createItemView(int position, ViewGroup parent) {
                return new TextView(parent.getContext());
            }
        };
        when(((AdapterViewAware) fragment).getAdapterObserver()).thenReturn(adapter);
    }
}
