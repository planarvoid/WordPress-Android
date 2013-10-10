package com.soundcloud.android.fragment;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksCategoriesAdapter;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.Section;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksCategoriesFragmentTest {

    private ExploreTracksCategoriesFragment fragment;

    @Mock
    private ExploreTracksCategoriesAdapter adapter;

    @Test
    public void shouldAddMusicAndAudioSections(){
        final ExploreTracksCategories sections = new ExploreTracksCategories();
        final ArrayList<ExploreTracksCategory> musicCategories = Lists.newArrayList(new ExploreTracksCategory("electronic"));
        sections.setMusic(musicCategories);
        final ArrayList<ExploreTracksCategory> audioCategories = Lists.newArrayList(new ExploreTracksCategory("electronic"));
        sections.setAudio(audioCategories);

        fragment = new ExploreTracksCategoriesFragment(adapter, Observable.just(sections));
        fragment.onCreate(null);

        verify(adapter).onNext(eq(new Section(R.string.explore_category_header_music, musicCategories)));
        verify(adapter).onNext(eq(new Section(R.string.explore_category_header_audio, audioCategories)));
        verify(adapter).onCompleted();
    }


}
