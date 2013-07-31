package com.soundcloud.android.paging;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.adapter.SuggestedTracksAdapter;
import com.soundcloud.android.fragment.behavior.AdapterViewAware;
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
        fragment = mock(Fragment.class, withSettings().extraInterfaces(AdapterViewAware.class));
        when(fragment.isAdded()).thenReturn(true);
        when(fragment.getActivity()).thenReturn(new FragmentActivity());
        when(((AdapterViewAware<Track>) fragment).getAdapter()).thenReturn(suggestedTracksAdapter);
    }

    @Test
    public void testShowsErrorState() {
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.<Track>error(new Exception())));
        when(((AdapterViewAware<Track>) fragment).getEmptyView()).thenReturn(emptyListView);

        adapterViewPager.startLoading(fragment);
        verify(emptyListView).setStatus(EmptyListView.Status.ERROR);
    }

    @Test
    public void testShowsEmptyState() {
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.<Track>empty()));
        when(((AdapterViewAware<Track>) fragment).getEmptyView()).thenReturn(emptyListView);

        adapterViewPager.startLoading(fragment);
        verify(emptyListView).setStatus(EmptyListView.Status.OK);
        verify(suggestedTracksAdapter, never()).addItem(any(Track.class));
    }

    @Test
    public void testShowsContent() {
        final Track track = new Track();
        adapterViewPager = new AdapterViewPager(Observable.just(Observable.just(track)));
        when(((AdapterViewAware<Track>) fragment).getEmptyView()).thenReturn(emptyListView);

        adapterViewPager.startLoading(fragment);
        verify(emptyListView).setStatus(EmptyListView.Status.OK);
        verify(suggestedTracksAdapter, times(1)).addItem(track);
    }

}
