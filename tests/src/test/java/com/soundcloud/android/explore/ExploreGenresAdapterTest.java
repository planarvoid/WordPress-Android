package com.soundcloud.android.explore;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.collections.Section;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class ExploreGenresAdapterTest {

    private ExploreGenresAdapter exploreGenresAdapter;
    @Mock
    private LayoutInflater layoutInflater;
    @Mock
    private ViewGroup parentView;
    @Mock
    private Section section;
    @Mock
    private ExploreGenre exploreGenre;
    @Mock
    private ExploreGenreCategoryRow itemView;


    @Before
    public void setUp() throws Exception {
        exploreGenresAdapter = new ExploreGenresAdapter(layoutInflater);
    }

    @Test
    public void shouldInflateExploreRowWithoutAttachingToParent(){
        exploreGenresAdapter.createItemView(0, parentView);
        verify(layoutInflater).inflate(R.layout.explore_genre_item, parentView, false);
    }

    @Test
    public void shouldSetDisplayNameOnBindItemView (){
        when(exploreGenre.getTitle()).thenReturn("Genre Title");
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));

        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        exploreGenresAdapter.onNext(section);
        exploreGenresAdapter.bindItemView(0, itemView);

        verify(itemView).setDisplayName(eq("Genre Title"));
    }

    @Test
    public void shouldSetExpectedPostfixScreenValueAsTagToView() {

        when(exploreGenre.getTitle()).thenReturn("Religion & Spirituality");
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));

        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        exploreGenresAdapter.onNext(section);
        exploreGenresAdapter.bindItemView(0, itemView);

        verify(itemView).setTag(endsWith("religion_&_spirituality"));
    }

    @Test
    public void shouldSetMusicGenreCategoryAsPartOfScreenValueTag() {
        when(section.getSectionId()).thenReturn(1);

        when(exploreGenre.getTitle()).thenReturn("Religion & Spirituality");
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));

        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        exploreGenresAdapter.onNext(section);
        exploreGenresAdapter.bindItemView(0, itemView);

        verify(itemView).setTag(startsWith("explore:genres:music"));
    }

    @Test
    public void shouldSetAudioGenreCategoryAsPartOfScreenValueTag() {
        when(section.getSectionId()).thenReturn(0);

        when(exploreGenre.getTitle()).thenReturn("Religion & Spirituality");
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));

        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        exploreGenresAdapter.onNext(section);
        exploreGenresAdapter.bindItemView(0, itemView);

        verify(itemView).setTag(startsWith("explore:genres:audio"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnrecognisedSectionValueProvided(){

        when(section.getSectionId()).thenReturn(3);
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        exploreGenresAdapter.onNext(section);
        exploreGenresAdapter.bindItemView(0, itemView);
    }
}
