package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.SuggestedTracksAdapter;
import com.soundcloud.android.paging.AdapterViewPager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class SuggestedTracksFragmentTest {

    private SuggestedTracksFragment fragment;

    @Mock
    private SuggestedTracksAdapter adapter;
    @Mock
    private AdapterViewPager adapterViewPager;

    @Before
    public void setUp() throws Exception {
        fragment = new SuggestedTracksFragment(adapter, adapterViewPager);
        SherlockFragmentActivity fragmentActivity = new SherlockFragmentActivity();
        Robolectric.shadowOf(fragment).setActivity(fragmentActivity);
    }

    @Ignore("TODO: breaks while inflating the PTR GridView")
    @Test
    public void shouldLoadFirstPageOfTrackSuggestionsWhenStarted() {
        fragment.onViewCreated(View.inflate(Robolectric.application, R.layout.suggested_tracks_fragment, null), null);
    }
}
