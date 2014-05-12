package com.soundcloud.android.explore;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksFragmentTest {

    private ExploreTracksFragment fragment;

    @Mock
    private FragmentActivity activity;
    @Mock
    private ExploreTracksAdapter adapter;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private ImageOperations imageOperations;
    @Mock
    private ExploreTracksOperations exploreTracksOperations;
    @Mock
    private PullToRefreshController pullToRefreshController;

    @Before
    public void setUp() throws Exception {
        fragment = new ExploreTracksFragment(adapter, playbackOperations, imageOperations,
                exploreTracksOperations, pullToRefreshController);
        Robolectric.shadowOf(fragment).setActivity(activity);
        createFragmentView();
    }

    @Ignore("TODO: breaks while inflating the PTR GridView")
    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWhenStarted() {
        fragment.onViewCreated(View.inflate(Robolectric.application, R.layout.suggested_tracks_fragment, null), null);
    }

    @Test
    public void shouldDetachPullToRefreshControllerOnDestroyView() {
        fragment.onDestroyView();
        verify(pullToRefreshController).detach();
    }

    private void createFragmentView() {
        View layout = fragment.onCreateView(LayoutInflater.from(Robolectric.application), null, null);
        Robolectric.shadowOf(fragment).setView(layout);
        fragment.onViewCreated(layout, null);
    }

}
