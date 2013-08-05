package com.soundcloud.android.paging;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.adapter.SuggestedTracksAdapter;
import com.soundcloud.android.fragment.behavior.PagingAdapterViewAware;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.EmptyListView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class AdapterViewPagerTest {

    AdapterViewPager adapterViewPager;
    Fragment fragment;
    @Mock
    Observable observable;
    @Mock
    SuggestedTracksAdapter suggestedTracksAdapter;
    @Mock
    Observable<Track> suggestedTrackObservable;
    @Mock
    EmptyListView emptyListView;

    @Before
    public void setup() {
        fragment = mock(Fragment.class, withSettings().extraInterfaces(PagingAdapterViewAware.class));
        when(fragment.isAdded()).thenReturn(true);
        when(fragment.getActivity()).thenReturn(new FragmentActivity());
        when(((PagingAdapterViewAware<Track>) fragment).getAdapter()).thenReturn(suggestedTracksAdapter);
        when(((PagingAdapterViewAware<Track>) fragment).getEmptyView()).thenReturn(emptyListView);
    }

    @Test
    public void testShowsErrorState() {
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.<Track>error(new Exception())));

        adapterViewPager.startLoading(fragment);
        verify(emptyListView).setStatus(EmptyListView.Status.ERROR);
    }

    @Test
    public void testShowsEmptyState() {
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.<Track>empty()));

        adapterViewPager.startLoading(fragment);
        verify(emptyListView).setStatus(EmptyListView.Status.OK);
        verify(suggestedTracksAdapter, never()).addItem(any(Track.class));
    }

    @Test
    public void testShowsContent() {
        final Track track = new Track();
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.just(track)));

        adapterViewPager.startLoading(fragment);
        verify(emptyListView).setStatus(EmptyListView.Status.OK);
        verify(suggestedTracksAdapter, times(1)).addItem(track);
    }

    @Test
    public void loadNextPageShouldTriggerAdapterProgressItem() {
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.just(new Track())));
        adapterViewPager.startLoading(fragment);
        verify(suggestedTracksAdapter).setDisplayProgressItem(true);
    }

    @Test
    public void pageItemObserverShouldHideAdapterProgressItemAfterCompletion() {
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.<Track>empty()));
        AdapterViewPager.PageItemObserver observer = adapterViewPager.new PageItemObserver(fragment);
        observer.onCompleted(fragment);
        verify(suggestedTracksAdapter).setDisplayProgressItem(false);
    }

    @Test
    public void pageScrollListenerShouldNotDoAnythingIfAlreadyLoading() {
        AdapterViewPager adapterViewPager = mock(AdapterViewPager.class);
        AdapterViewPager.PageScrollListener listener = adapterViewPager.new PageScrollListener(fragment);
        when(suggestedTracksAdapter.isDisplayProgressItem()).thenReturn(true);

        listener.onScroll(null, 0, 5, 5);
        verify(adapterViewPager, never()).loadNextPage(any(Fragment.class));
    }

    @Test
    public void pageScrollListenerShouldTriggerNextPageLoad() {
        AdapterViewPager adapterViewPager = mock(AdapterViewPager.class);
        AdapterViewPager.PageScrollListener listener = adapterViewPager.new PageScrollListener(fragment);

        listener.onScroll(null, 0, 5, 5);
        verify(adapterViewPager).loadNextPage(fragment);
    }

    @Test
    public void pageScrollListenerShouldLoadNextPageWithOnePageLookAhead() {
        AdapterViewPager adapterViewPager = mock(AdapterViewPager.class);
        AdapterViewPager.PageScrollListener listener = adapterViewPager.new PageScrollListener(fragment);

        listener.onScroll(null, 0, 5, 2 * 5);
        verify(adapterViewPager).loadNextPage(fragment);
    }

    @Test
    public void shouldNotLoadNextPageIfZeroItems() {
        AdapterViewPager adapterViewPager = mock(AdapterViewPager.class);
        AdapterViewPager.PageScrollListener listener = adapterViewPager.new PageScrollListener(fragment);

        listener.onScroll(null, 0, 0, 0);
        verify(adapterViewPager, never()).loadNextPage(any(Fragment.class));
    }

}
