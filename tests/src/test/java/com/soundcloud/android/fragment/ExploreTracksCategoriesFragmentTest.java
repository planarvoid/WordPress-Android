package com.soundcloud.android.fragment;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksCategoriesAdapter;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksCategoriesFragmentTest {

    private ExploreTracksCategoriesFragment fragment;

    @Mock
    private ExploreTracksCategoriesAdapter adapter;

    @Test
    public void shouldAddMusicAndAudioSections(){
        final ExploreTracksCategories sections = new ExploreTracksCategories();
        final ExploreTracksCategory electronicCategory = new ExploreTracksCategory("electronic");
        final ArrayList<ExploreTracksCategory> musicCategories = Lists.newArrayList(electronicCategory);
        sections.setMusic(musicCategories);
        final ExploreTracksCategory comedyCategory = new ExploreTracksCategory("comedy");
        final ArrayList<ExploreTracksCategory> audioCategories = Lists.newArrayList(comedyCategory);
        sections.setAudio(audioCategories);

        fragment = new ExploreTracksCategoriesFragment(Observable.just(sections));
        fragment.onCreate(null);
        View v = fragment.onCreateView(LayoutInflater.from(Robolectric.application), new FrameLayout(Robolectric.application), null);

        Robolectric.shadowOf(fragment).setView(v);
        fragment.onViewCreated(v, null);

        final ListView listView = (ListView) v.findViewById(R.id.suggested_tracks_categories_list);
        final ListAdapter adapter1 = listView.getAdapter();
        expect(adapter1.getCount()).toBe(2); // should have 2 sections
        expect(adapter1.getItem(0)).toBe(electronicCategory);
        expect(adapter1.getItem(1)).toBe(comedyCategory);
    }


}
