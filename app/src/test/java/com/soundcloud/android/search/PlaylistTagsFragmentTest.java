package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.PlaylistTagsCollection;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.RxTestHelper;
import com.soundcloud.android.view.EmptyListView;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistTagsFragmentTest {

    private PlaylistTagsFragment fragment;
    private FragmentActivity activity = new FragmentActivity();

    @Mock
    private SearchOperations searchOperations;

    @Before
    public void setUp() throws Exception {
        final PlaylistTagsCollection tags = new PlaylistTagsCollection();
        tags.setCollection(Arrays.asList("one", "two", "three"));
        when(searchOperations.getPlaylistTags()).thenReturn(Observable.<PlaylistTagsCollection>from(tags));
    }

    @Test
    public void shouldFetchPlaylistTagsAndDisplayThem() throws Exception {
        createFragment();
        ViewGroup tagFlowLayout = (ViewGroup) fragment.getView().findViewById(R.id.tags);
        assertThat(tagFlowLayout.getChildCount(), equalTo(3));
    }

    @Test
    public void shouldShowProgressSpinnerWhileFetchingTags() {
        when(searchOperations.getPlaylistTags()).thenReturn(Observable.<PlaylistTagsCollection>never());
        createFragment();

        EmptyListView emptyView = (EmptyListView) fragment.getView().findViewById(android.R.id.empty);
        expect(emptyView).not.toBeNull();
        expect(emptyView.getVisibility()).toBe(View.VISIBLE);
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.WAITING);
    }

    @Test
    public void shouldHideEmptyViewWhenDoneLoadingTags() {
        createFragment();

        EmptyListView emptyView = (EmptyListView) fragment.getView().findViewById(android.R.id.empty);
        expect(emptyView).not.toBeNull();
        expect(emptyView.getVisibility()).toBe(View.GONE);
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.OK);
    }

    @Test
    public void shouldCacheObservableResults() {
        Observable observable = RxTestHelper.mockObservable().howeverScheduled().get();
        when(searchOperations.getPlaylistTags()).thenReturn(observable);

        createFragment();
        // go through config change; onViewCreated is called again, should not trigger the source sequence again
        fragment.onViewCreated(fragment.getView(), null);

        verify(observable, times(1)).subscribe(Matchers.any(Observer.class));
    }

    private void createFragment() {
        fragment = new PlaylistTagsFragment(searchOperations);
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);
        fragment.onCreate(null);
        View layout = fragment.onCreateView(activity.getLayoutInflater(), new FrameLayout(activity), null);
        Robolectric.shadowOf(fragment).setView(layout);
        fragment.onViewCreated(layout, null);
    }

}
