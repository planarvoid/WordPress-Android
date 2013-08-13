package com.soundcloud.android.paging;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.adapter.EndlessPagingAdapter;
import com.soundcloud.android.adapter.ExploreTracksAdapter;
import com.soundcloud.android.fragment.behavior.PagingAdapterViewAware;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.observers.PageItemObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

@RunWith(SoundCloudTestRunner.class)
public class AdapterViewPagerTest {

    private AdapterViewPager adapterViewPager;
    private Fragment fragment;
    @Mock
    private AbsListView fragmentLayout;
    @Mock
    private Observable observable;
    @Mock
    private ExploreTracksAdapter adapter;
    @Mock
    private Observable<Track> suggestedTrackObservable;
    @Mock
    private Observer itemObserver;

    @Before
    public void setup() {
        fragment = mock(Fragment.class, withSettings().extraInterfaces(PagingAdapterViewAware.class));
        when(fragment.isAdded()).thenReturn(true);
        when(fragment.getActivity()).thenReturn(new FragmentActivity());
        when(((PagingAdapterViewAware<Track>) fragment).getAdapter()).thenReturn(adapter);
        when(fragmentLayout.getAdapter()).thenReturn(adapter);
        when(fragment.getView()).thenReturn(fragmentLayout);
    }

    @Test
    public void loadNextPageShouldTriggerAdapterProgressItem() {
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.just(new Track())));
        adapterViewPager.subscribe(fragment, new PageItemObserver(fragment));
        verify(adapter).setDisplayProgressItem(true);
    }

    @Test
    public void shouldBeAbleToCallUnsubscribeWithNullSubscriptions() {
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.just(new Track())));
        adapterViewPager.unsubscribe();
    }

    @Test(expected = IllegalArgumentException.class)
    public void pageScrollListenerShouldEnsureEndlessListAdapterIsUsed() {
        when(fragmentLayout.getAdapter()).thenReturn(mock(BaseAdapter.class));
        AdapterViewPager adapterViewPager = mock(AdapterViewPager.class);
        AdapterViewPager.PageScrollListener listener = adapterViewPager.new PageScrollListener(itemObserver);
        listener.onScroll(fragmentLayout, 0, 5, 5);
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfAlreadyLoading() {
        AdapterViewPager adapterViewPager = mock(AdapterViewPager.class);
        AdapterViewPager.PageScrollListener listener = adapterViewPager.new PageScrollListener(itemObserver);
        when(adapter.isDisplayProgressItem()).thenReturn(true);

        listener.onScroll(fragmentLayout, 0, 5, 5);
        verify(adapterViewPager, never()).loadNextPage(any(EndlessPagingAdapter.class), any(Observer.class));
    }

    @Test
    public void pageScrollListenerShouldTriggerNextPageLoad() {
        AdapterViewPager adapterViewPager = mock(AdapterViewPager.class);
        AdapterViewPager.PageScrollListener listener = adapterViewPager.new PageScrollListener(itemObserver);

        listener.onScroll(fragmentLayout, 0, 5, 5);
        verify(adapterViewPager).loadNextPage(refEq(adapter), any(Observer.class));
    }

    @Test
    public void pageScrollListenerShouldLoadNextPageWithOnePageLookAhead() {
        AdapterViewPager adapterViewPager = mock(AdapterViewPager.class);
        AdapterViewPager.PageScrollListener listener = adapterViewPager.new PageScrollListener(itemObserver);

        listener.onScroll(fragmentLayout, 0, 5, 2 * 5);
        verify(adapterViewPager).loadNextPage(refEq(adapter), any(Observer.class));
    }

    @Test
    public void pageScrollListenerShouldNotLoadNextPageIfZeroItems() {
        AdapterViewPager adapterViewPager = mock(AdapterViewPager.class);
        AdapterViewPager.PageScrollListener listener = adapterViewPager.new PageScrollListener(itemObserver);

        listener.onScroll(fragmentLayout, 0, 0, 0);
        verify(adapterViewPager, never()).loadNextPage(any(EndlessPagingAdapter.class), any(Observer.class));
    }
}
