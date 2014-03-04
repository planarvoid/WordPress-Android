package com.soundcloud.android.actionbar;

import static com.soundcloud.android.actionbar.SearchActionBarController.SearchCallback;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.search.CombinedSearchActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class SearchActionBarControllerTest {

    @Mock
    private CombinedSearchActivity activity;
    @Mock
    private PublicCloudAPI cloudAPI;
    @Mock
    private SearchCallback callback;

    private SearchActionBarController actionBarController;

    @Before
    public void setUp() throws Exception {
        actionBarController = new SearchActionBarController(activity, cloudAPI, callback);
    }

    @Test
    public void shouldPerformTagSearchWhenQueryBeginsWithHashtag() {
        actionBarController.performSearch("#deep house");

        verify(callback).performTagSearch(eq("deep house"));
    }

    @Test
    public void shouldPerformNormalSearchWhenQueryDoesNotStartWithHashTag() {
        actionBarController.performSearch("skrillex");

        verify(callback).performTextSearch(eq("skrillex"));
    }

    @Test
    public void shouldNotPerformPlaylistTagSearchForSingleHashtagQuery() throws Exception {
        actionBarController.performSearch("#");
        verify(callback, never()).performTagSearch(anyString());
    }

    @Test
    public void shouldStripFirstHastagsWhenPerformingPlaylistTagSearch() throws Exception {
        actionBarController.performSearch("###clownstep");
        verify(callback).performTagSearch(eq("clownstep"));

        actionBarController.performSearch("####clownstep #dub");
        verify(callback).performTagSearch(eq("clownstep #dub"));
    }

    @Test
    public void shouldTrimQueryForTextSearch() throws Exception {
        actionBarController.performSearch("  a normal search ");
        verify(callback).performTextSearch(eq("a normal search"));

        actionBarController.performSearch("  a normal   search");
        verify(callback).performTextSearch(eq("a normal   search"));
    }

    @Test
    public void shouldTrimQueryForPlaylistTagSearch() throws Exception {
        actionBarController.performSearch("  #tag   ");
        verify(callback).performTagSearch(eq("tag"));

        actionBarController.performSearch("  ##tag1 #tag2  ");
        verify(callback).performTagSearch(eq("tag1 #tag2"));
    }
}
