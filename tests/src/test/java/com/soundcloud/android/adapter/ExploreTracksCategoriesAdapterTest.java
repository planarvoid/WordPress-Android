package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.model.ExploreTracksCategories;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.adapter.ExploreTracksCategoryRow;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class ExploreTracksCategoriesAdapterTest {


    private ExploreTracksCategoriesAdapter adapter;
    @Mock
    ExploreTracksCategories exploreTracksCategories;
    @Mock
    ExploreTracksCategoryRow exploreTracksCategoryRow;

    @Before
    public void setup() throws CreateModelException {
        adapter = new ExploreTracksCategoriesAdapter();
        when(exploreTracksCategoryRow.getResources()).thenReturn(Robolectric.application.getResources());
    }

    @Test
    public void shouldSetSectionViewTypes() {
        when(exploreTracksCategories.getMusic()).thenReturn(Lists.newArrayList(new ExploreTracksCategory(), new ExploreTracksCategory()));
        when(exploreTracksCategories.getAudio()).thenReturn(Lists.newArrayList(new ExploreTracksCategory()));
        adapter.setExploreTracksCategories(exploreTracksCategories);

        expect(adapter.getItemViewType(0)).toEqual(ExploreTracksCategoriesAdapter.VIEW_TYPE_SECTION);
        expect(adapter.getItemViewType(1)).toEqual(ExploreTracksCategoriesAdapter.VIEW_TYPE_DEFAULT);
        expect(adapter.getItemViewType(2)).toEqual(ExploreTracksCategoriesAdapter.VIEW_TYPE_SECTION);
    }

    @Test
    public void shouldSetSectionHeaderOnViews() {
        when(exploreTracksCategories.getMusic()).thenReturn(Lists.newArrayList(new ExploreTracksCategory(), new ExploreTracksCategory()));
        when(exploreTracksCategories.getAudio()).thenReturn(Lists.newArrayList(new ExploreTracksCategory()));
        adapter.setExploreTracksCategories(exploreTracksCategories);

        adapter.bindItemView(0, exploreTracksCategoryRow);
        verify(exploreTracksCategoryRow).showSectionHeader(Robolectric.application.getResources().getString(R.string.explore_category_header_music));
        adapter.bindItemView(1, exploreTracksCategoryRow);
        verify(exploreTracksCategoryRow).hideSectionHeader();
        adapter.bindItemView(2, exploreTracksCategoryRow);
        verify(exploreTracksCategoryRow).showSectionHeader(Robolectric.application.getResources().getString(R.string.explore_category_header_audio));
    }

    @Test
    public void shouldSetTitlesForMusicAndAudio() {
        ExploreTracksCategory exploreTracksCategory1 = new ExploreTracksCategory("Cat1");
        ExploreTracksCategory exploreTracksCategory2 = new ExploreTracksCategory("Cat2");
        when(exploreTracksCategories.getMusic()).thenReturn(Lists.newArrayList(exploreTracksCategory1));
        when(exploreTracksCategories.getAudio()).thenReturn(Lists.newArrayList(exploreTracksCategory2));
        adapter.setExploreTracksCategories(exploreTracksCategories);

        adapter.bindItemView(0, exploreTracksCategoryRow);
        verify(exploreTracksCategoryRow).setDisplayName(exploreTracksCategory1.getDisplayName(Robolectric.application));
        adapter.bindItemView(1, exploreTracksCategoryRow);
        verify(exploreTracksCategoryRow).setDisplayName(exploreTracksCategory2.getDisplayName(Robolectric.application));
    }

}
