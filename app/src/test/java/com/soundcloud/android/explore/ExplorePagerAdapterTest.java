package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.android.util.TestFragmentManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    public void shouldHave3Fragments() {
        expect(adapter.getCount()).toBe(3);
    }

    @Test
    public void shouldCreateGenresFragmentForFirstPage() {
        expect(adapter.getPageTitle(0)).toEqual("Genres");
    }

    @Test
    public void shouldCreateMusicFragmentForSecondPage() {
        expect(adapter.getPageTitle(1)).toEqual("Music");
    }

    @Test
    public void shouldCreateAudioFragmentForThirdPage() {
        expect(adapter.getPageTitle(2)).toEqual("Audio");
    }

}
