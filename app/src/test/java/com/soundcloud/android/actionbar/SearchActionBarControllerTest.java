package com.soundcloud.android.actionbar;

import static com.soundcloud.android.actionbar.SearchActionBarController.SearchCallback;
import static org.mockito.Matchers.eq;
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
    public void shouldStripFirstHastagsWhenPerformingPlaylistTagSearch() throws Exception {
        actionBarController.performSearch("#");
        verify(callback).performTagSearch(eq(""));

        actionBarController.performSearch("###clowstep");
        verify(callback).performTagSearch(eq("clowstep"));

        actionBarController.performSearch("####clowstep #dub");
        verify(callback).performTagSearch(eq("clowstep #dub"));
    }
}
