package com.soundcloud.android.search;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.search.PlaylistTagsFragment.PlaylistTagsFragmentListener;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.SearchActionBarController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.view.EmptyViewController;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import rx.Observable;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Ignore // RL1 doesn't support dealing with resources from AARs
@RunWith(SoundCloudTestRunner.class)
public class PlaylistTagsFragmentTest {

    @Mock private PlaylistDiscoveryOperations operations;
    @Mock private SearchActionBarController actionBarController;
    @Mock private EmptyViewController emptyViewController;

    @InjectMocks private PlaylistTagsFragment fragment;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        fragment.eventBus = eventBus;

        final List<String> popularTags = Arrays.asList("popular1", "popular2", "popular3");
        final List<String> recentTags = Arrays.asList("recent1", "recent2", "recent3");

        when(operations.popularPlaylistTags()).thenReturn(Observable.just(popularTags));
        when(operations.recentPlaylistTags()).thenReturn(Observable.just(recentTags));
    }

    @Test(expected = IllegalArgumentException.class)
    public void onAttachShouldThrowIllegalArgumentIfParentActivityIsNotTagClickListener() {
        fragment.onAttach(new FragmentActivity());
    }

    @Test
    public void shouldFetchPlaylistTagsAndDisplayThem() throws Exception {
        createFragment();
        ViewGroup tagFlowLayout = (ViewGroup) fragment.getView().findViewById(R.id.all_tags);
        assertThat(tagFlowLayout.getChildCount(), equalTo(3));
    }

    @Test
    public void shouldCacheObservableResults() {
        TestObservables.MockObservable observable = TestObservables.emptyObservable();
        when(operations.popularPlaylistTags()).thenReturn(observable);

        createFragment();
        // go through config change; onViewCreated is called again, should not trigger the source sequence again
        fragment.onDestroyView();
        fragment.onViewCreated(fragment.getView(), null);

        expect(observable.subscribers()).toNumber(1);
    }

    @Test
    public void shouldShowRecentTagsIfRecentTagsExist() throws Exception {
        createFragment();
        View recentTagsLayout = fragment.getView().findViewById(R.id.recent_tags);
        expect(recentTagsLayout.getVisibility()).toEqual(View.VISIBLE);
    }

    @Test
    public void shouldDisplayTagsWithHashSymbolPrepended() throws Exception {
        createFragment();
        ViewGroup recentTagsLayout = (ViewGroup) fragment.getView().findViewById(R.id.recent_tags);
        TextView tagView = (TextView) recentTagsLayout.getChildAt(0);
        expect(tagView.getText()).toEqual("#recent1");
    }

    @Test
    public void shouldNotShowRecentTagsIfRecentTagsDoNotExist() throws Exception {
        Observable<List<String>> observable = Observable.just(Collections.<String>emptyList());
        when(operations.recentPlaylistTags()).thenReturn(observable);

        createFragment();
        View recentTagsLayout = fragment.getView().findViewById(R.id.recent_tags_container);
        expect(recentTagsLayout.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void shouldNotShowRecentTagsOnError() throws Exception {
        when(operations.recentPlaylistTags()).thenReturn(Observable.<List<String>>error(new Exception()));

        createFragment();
        View recentTagsLayout = fragment.getView().findViewById(R.id.recent_tags_container);
        expect(recentTagsLayout.getVisibility()).toEqual(View.GONE);
    }

    @Test
    public void clickingTagShouldCallTagListenerWithCorrectTag() {
        createFragment();

        FragmentActivity listener = mock(FragmentActivity.class, withSettings().extraInterfaces(PlaylistTagsFragmentListener.class));
        Robolectric.shadowOf(fragment).setActivity(listener);

        ViewGroup tagFlowLayout = (ViewGroup) fragment.getView().findViewById(R.id.all_tags);
        tagFlowLayout.getChildAt(0).performClick();

        verify((PlaylistTagsFragmentListener) listener).onTagSelected("popular1");
    }
    
    @Test
    public void shouldCallBackToActivityWhenScrollingTags() {
        createFragment();
        FragmentActivity listener = mock(FragmentActivity.class, withSettings().extraInterfaces(PlaylistTagsFragmentListener.class));
        Robolectric.shadowOf(fragment).setActivity(listener);

        fragment.onScroll(1, 2);

        verify((PlaylistTagsFragmentListener) listener).onTagsScrolled();
    }

    @Test
    public void shouldTrackSearchSubmitEventForRecentTag() throws Exception {
        createFragment();

        ViewGroup tagFlowLayout = (ViewGroup) fragment.getView().findViewById(R.id.recent_tags);
        tagFlowLayout.getChildAt(0).performClick();

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toEqual(SearchEvent.KIND_SUBMIT);
        expect(event.getAttributes().get("type")).toEqual("tag");
        expect(event.getAttributes().get("location")).toEqual("recent_tags");
        expect(event.getAttributes().get("content")).toEqual("recent1");
    }

    @Test
    public void shouldTrackSearchSubmitEventForPopularTag() throws Exception {
        createFragment();

        ViewGroup tagFlowLayout = (ViewGroup) fragment.getView().findViewById(R.id.all_tags);
        tagFlowLayout.getChildAt(0).performClick();

        SearchEvent event = (SearchEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toEqual(SearchEvent.KIND_SUBMIT);
        expect(event.getAttributes().get("type")).toEqual("tag");
        expect(event.getAttributes().get("location")).toEqual("popular_tags");
        expect(event.getAttributes().get("content")).toEqual("popular1");
    }

    private void createFragment() {
        FragmentActivity activity = new SearchActivity(actionBarController);
        Robolectric.shadowOf(fragment).setActivity(activity);
        Robolectric.shadowOf(fragment).setAttached(true);
        fragment.onAttach(activity);
        fragment.onCreate(null);
        View layout = fragment.onCreateView(activity.getLayoutInflater(), new FrameLayout(activity), null);
        Robolectric.shadowOf(fragment).setView(layout);
        fragment.onViewCreated(layout, null);
    }

}
