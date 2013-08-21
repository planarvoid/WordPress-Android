package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
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
    public void shouldAdjustItemCountBasedOnLoadingState() {
        createAdapter();
        adapter.addItem(new Track()); // populate to override empty count behavior
        adapter.setDisplayProgressItem(true);
        expect(adapter.getCount()).toBe(2);
        adapter.setDisplayProgressItem(false);
        expect(adapter.getCount()).toBe(1);
    }

    @Test
    public void shouldReportTwoDifferentItemViewTypes() {
        createAdapter();
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldCreateProgressView() {
        createAdapter();
        adapter.setDisplayProgressItem(true);
        View progressView = adapter.getView(0, null, new FrameLayout(Robolectric.application));
        expect(progressView).not.toBeNull();
        expect(progressView.findViewById(R.id.list_loading)).not.toBeNull();
    }

    @Test
    public void shouldConvertProgressView() {
        createAdapter();
        adapter.setDisplayProgressItem(true);
        View convertView = LayoutInflater.from(Robolectric.application).inflate(R.layout.list_loading_item, null);
        View progressView = adapter.getView(0, convertView, new FrameLayout(Robolectric.application));
        expect(progressView).toBe(convertView);
    }

    @Test
    public void countShouldBeIfLoadingAndNoItems() {
        createAdapter();
        adapter.setDisplayProgressItem(true);
        expect(adapter.getCount()).toBe(0);
    }

    @Test
    public void pageScrollListenerShouldTriggerNextPageLoad() {
        createAdapter(Observable.from(Observable.just(new Track()), pageObservable));
        adapter.subscribe(itemObserver);
        adapter.onScroll(absListView, 0, 5, 5);
        verify(pageObservable).subscribe(itemObserver);
    }

    @Test
    public void pageScrollListenerShouldLoadNextPageWithOnePageLookAhead() {
        createAdapter(Observable.from(Observable.just(new Track()), pageObservable));
        adapter.subscribe(itemObserver);
        adapter.onScroll(absListView, 0, 5, 2 * 5);
        verify(pageObservable).subscribe(itemObserver);
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfAlreadyLoading() {
        final Observable pageObservable = Mockito.mock(Observable.class);
        createAdapter(Observable.from(Observable.just(new Track()), pageObservable));
        adapter.subscribe(itemObserver);
        adapter.setDisplayProgressItem(true);
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
        adapter = new EndlessPagingAdapter<Track>(pageEmittingObservable, 10, R.layout.list_loading_item) {
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
