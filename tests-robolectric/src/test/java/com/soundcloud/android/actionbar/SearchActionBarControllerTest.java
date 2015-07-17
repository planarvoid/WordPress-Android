package com.soundcloud.android.actionbar;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.actionbar.SearchActionBarController.SearchCallback;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.utils.BugReporter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v7.app.ActionBar;

@RunWith(SoundCloudTestRunner.class)
public class SearchActionBarControllerTest {

    @Mock private SearchActivity activity;
    @Mock private ActionBar actionBar;
    @Mock private PublicApi cloudAPI;
    @Mock private SearchCallback callback;
    @Mock private PlaybackOperations playbackOps;
    @Mock private BugReporter bugReporter;
    @Mock private Navigator navigator;

    private TestEventBus eventBus = new TestEventBus();
    private SearchActionBarController actionBarController;

    @Before
    public void setUp() throws Exception {
        actionBarController = new SearchActionBarController(cloudAPI, playbackOps, eventBus,
                TestSubscribers.expandPlayerSubscriber(), bugReporter, navigator);
        actionBarController.setSearchCallback(callback);
    }

    @Test
    public void shouldPerformTagSearchWhenQueryBeginsWithHashtag() {
        actionBarController.performSearch("#deep house", true);

        verify(callback).performTagSearch(eq("deep house"));
    }

    @Test
    public void shouldPerformNormalSearchWhenQueryDoesNotStartWithHashTag() {
        actionBarController.performSearch("skrillex", true);

        verify(callback).performTextSearch(eq("skrillex"));
    }

    @Test
    public void shouldNotPerformPlaylistTagSearchForSingleHashtagQuery() throws Exception {
        actionBarController.performSearch("#", true);
        verify(callback, never()).performTagSearch(anyString());
    }

    @Test
    public void shouldStripFirstHastagsWhenPerformingPlaylistTagSearch() throws Exception {
        actionBarController.performSearch("###clownstep", true);
        verify(callback).performTagSearch(eq("clownstep"));

        actionBarController.performSearch("####clownstep #dub", true);
        verify(callback).performTagSearch(eq("clownstep #dub"));
    }

    @Test
    public void shouldTrimQueryForTextSearch() throws Exception {
        actionBarController.performSearch("  a normal search ", true);
        verify(callback).performTextSearch(eq("a normal search"));

        actionBarController.performSearch("  a normal   search", true);
        verify(callback).performTextSearch(eq("a normal   search"));
    }

    @Test
    public void shouldTrimQueryForPlaylistTagSearch() throws Exception {
        actionBarController.performSearch("  #tag   ", true);
        verify(callback).performTagSearch(eq("tag"));

        actionBarController.performSearch("  ##tag1 #tag2  ", true);
        verify(callback).performTagSearch(eq("tag1 #tag2"));
    }

    @Test
    public void shouldPublishSearchEventForNormalSearchInSearchField() throws Exception {
        actionBarController.performSearch("query", false);
        SearchEvent event = (SearchEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(event.getAttributes().get("type")).toEqual("normal");
        expect(event.getAttributes().get("location")).toEqual("search_field");
        expect(event.getAttributes().get("content")).toEqual("query");
    }

    @Test
    public void shouldPublishSearchEventForNormalSearchViaShortcut() throws Exception {
        actionBarController.performSearch("query", true);
        SearchEvent event = (SearchEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(event.getAttributes().get("type")).toEqual("normal");
        expect(event.getAttributes().get("location")).toEqual("search_suggestion");
        expect(event.getAttributes().get("content")).toEqual("query");
    }

    @Test
    public void shouldPublishSearchEventForTagSearchViaSearchField() throws Exception {
        actionBarController.performSearch("#query", false);
        SearchEvent event = (SearchEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(event.getAttributes().get("type")).toEqual("tag");
        expect(event.getAttributes().get("location")).toEqual("search_field");
        expect(event.getAttributes().get("content")).toEqual("#query");
    }

    @Test
    public void shouldPublishSearchEventForTagSearchViaShortcut() throws Exception {
        actionBarController.performSearch("#query", true);
        SearchEvent event = (SearchEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        expect(event.getKind()).toBe(SearchEvent.KIND_SUBMIT);
        expect(event.getAttributes().get("type")).toEqual("tag");
        expect(event.getAttributes().get("location")).toEqual("search_suggestion");
        expect(event.getAttributes().get("content")).toEqual("#query");
    }
}
