package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.android.util.TestFragmentManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class ExplorePagerAdapterTest {

    private ExplorePagerAdapter adapter;

    @Before
    public void setup() {
        adapter = new ExplorePagerAdapter(
                Robolectric.application.getResources(), new TestFragmentManager(new FragmentActivity()));
    }

    @Test
    public void shouldCreateCategoriesFragmentForFirstPagerPage() {
        expect(adapter.getItem(0)).toBeInstanceOf(ExploreTracksCategoriesFragment.class);
    }

    @Test
    public void shouldCreateTrendingMusicFragmentForSecondPagerPage() {
        final Fragment fragment = adapter.getItem(1);
        expect(fragment).toBeInstanceOf(ExploreTracksFragment.class);
        expect(fragment.getArguments().getParcelable(ExploreTracksCategory.EXTRA)).toBe(ExploreTracksCategory.POPULAR_MUSIC_CATEGORY);
        expect(fragment.getArguments().getString(ExploreTracksFragment.SCREEN_TRACKING_TAG_EXTRA)).toEqual("explore:trending_music");
    }

    @Test
    public void shouldCreateTrendingAudioFragmentForThirdPagerPage() {
        final Fragment fragment = adapter.getItem(2);
        expect(fragment).toBeInstanceOf(ExploreTracksFragment.class);
        expect(fragment.getArguments().getParcelable(ExploreTracksCategory.EXTRA)).toBe(ExploreTracksCategory.POPULAR_AUDIO_CATEGORY);
        expect(fragment.getArguments().getString(ExploreTracksFragment.SCREEN_TRACKING_TAG_EXTRA)).toEqual("explore:trending_audio");
    }
}
