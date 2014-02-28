package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.search.PlaylistTagsFragment.TagClickListener;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.model.PlaylistTagsCollection;
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
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistTagsFragmentTest {

    private PlaylistTagsFragment fragment;
    private FragmentActivity activity = new CombinedSearchActivity();

    @Mock
    private SearchOperations searchOperations;

    @Before
    public void setUp() throws Exception {
        final PlaylistTagsCollection tags = new PlaylistTagsCollection();
        tags.setCollection(Arrays.asList("one", "two", "three"));
        when(searchOperations.getPlaylistTags()).thenReturn(Observable.<PlaylistTagsCollection>from(tags));
        when(searchOperations.getRecentPlaylistTags()).thenReturn(Observable.<PlaylistTagsCollection>empty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void onAttachShouldThrowIllegalArgumentIfParentActivityIsNotTagClickListener() {
        fragment = new PlaylistTagsFragment(searchOperations);
        fragment.onAttach(new FragmentActivity());
    }

    @Test
    public void shouldFetchPlaylistTagsAndDisplayThem() throws Exception {
        createFragment();
        ViewGroup tagFlowLayout = (ViewGroup) fragment.getView().findViewById(R.id.all_tags);
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

    @Test
    public void shouldShowErrorScreenOnLoadingTagsError() throws Exception {
        when(searchOperations.getPlaylistTags()).thenReturn(Observable.<PlaylistTagsCollection>error(new Exception()));

        createFragment();
        EmptyListView emptyView = (EmptyListView) fragment.getView().findViewById(android.R.id.empty);
        expect(emptyView).not.toBeNull();
        expect(emptyView.getStatus()).toEqual(EmptyListView.Status.ERROR);
    }

    @Test
    public void shouldShowRecentTagsIfRecentTagsExist() throws Exception {
        PlaylistTagsCollection collection = new PlaylistTagsCollection(Lists.newArrayList("#tag1"));
        Observable<PlaylistTagsCollection> observable = Observable.<PlaylistTagsCollection>from(collection);
        when(searchOperations.getRecentPlaylistTags()).thenReturn(observable);

        createFragment();
        View recentTagsLayout = fragment.getView().findViewById(R.id.recent_tags);
        expect(recentTagsLayout.getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldNotShowRecentTagsIfRecentTagsDoNotExist() throws Exception {
        PlaylistTagsCollection collection = new PlaylistTagsCollection(Collections.<String>emptyList());
        Observable<PlaylistTagsCollection> observable = Observable.<PlaylistTagsCollection>from(collection);
        when(searchOperations.getRecentPlaylistTags()).thenReturn(observable);

        createFragment();
        View recentTagsLayout = fragment.getView().findViewById(R.id.recent_tags_container);
        expect(recentTagsLayout.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldNotShowRecentTagsOnError() throws Exception {
        when(searchOperations.getRecentPlaylistTags()).thenReturn(Observable.<PlaylistTagsCollection>error(new Exception()));

        createFragment();
        View recentTagsLayout = fragment.getView().findViewById(R.id.recent_tags_container);
        expect(recentTagsLayout.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void clickingTagShouldCallTagListenerWithCorrectTag() {
        createFragment();

        FragmentActivity listener = mock(FragmentActivity.class, withSettings().extraInterfaces(TagClickListener.class));
        Robolectric.shadowOf(fragment).setActivity(listener);

        ViewGroup tagFlowLayout = (ViewGroup) fragment.getView().findViewById(R.id.all_tags);
        tagFlowLayout.getChildAt(0).performClick();

        verify((TagClickListener) listener).onTagSelected("one");
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
