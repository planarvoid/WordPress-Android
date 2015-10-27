package com.soundcloud.android.explore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.support.v4.app.FragmentManager;

public class ExplorePagerAdapterTest extends AndroidUnitTest {

    private ExplorePagerAdapter adapter;

    @Before
    public void setup() {
        adapter = new ExplorePagerAdapter(resources(), mock(FragmentManager.class));
    }

    @Test
    public void shouldHave3Fragments() {
        assertThat(adapter.getCount()).isEqualTo(3);
    }

    @Test
    public void shouldCreateGenresFragmentForFirstPage() {
        assertThat(adapter.getPageTitle(0)).isEqualTo("GENRES");
    }

    @Test
    public void shouldCreateMusicFragmentForSecondPage() {
        assertThat(adapter.getPageTitle(1)).isEqualTo("MUSIC");
    }

    @Test
    public void shouldCreateAudioFragmentForThirdPage() {
        assertThat(adapter.getPageTitle(2)).isEqualTo("AUDIO");
    }

}
