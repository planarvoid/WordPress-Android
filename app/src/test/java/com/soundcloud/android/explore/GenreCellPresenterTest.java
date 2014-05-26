package com.soundcloud.android.explore;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class GenreCellPresenterTest {

    @InjectMocks
    private GenreCellPresenter presenter;

    @Mock
    private LayoutInflater layoutInflater;
    @Mock
    private ViewGroup parentView;
    @Mock
    private GenreRow itemView;
    @Mock
    private GenreSection section;

    @Before
    public void setup() {
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());
    }

    @Test
    public void shouldInflateExploreRowWithoutAttachingToParent() {
        presenter.createItemView(0, parentView, ItemAdapter.DEFAULT_ITEM_VIEW_TYPE);
        verify(layoutInflater).inflate(R.layout.explore_genre_item, parentView, false);
    }

    @Test
    public void shouldShowSectionHeaderForRowIfItemIsSectionHeader() {
        ExploreGenre exploreGenre = new ExploreGenre("Genre Title");

        presenter.setSectionForPosition(0, section, true);

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));
        verify(itemView).showSectionHeaderWithText(anyString());
    }

    @Test
    public void shouldHideSectionHeaderForRowIfItemIsNotASectionHeader() {
        ExploreGenre exploreGenre = new ExploreGenre("Genre Title");

        presenter.setSectionForPosition(0, section, false);

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));
        verify(itemView).hideSectionHeader();
    }

    @Test
    public void shouldSetDisplayNameOnBindItemView() {
        presenter.setSectionForPosition(0, section, false);
        ExploreGenre exploreGenre = new ExploreGenre("Genre Title");

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));

        verify(itemView).setDisplayName(eq("Genre Title"));
    }

    @Test
    public void shouldSetExpectedPostfixScreenValueAsTagToView() {
        presenter.setSectionForPosition(0, section, false);
        ExploreGenre exploreGenre = new ExploreGenre("Religion & Spirituality");

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));

        verify(itemView).setTag(endsWith("religion_&_spirituality"));
    }

    @Test
    public void shouldSetMusicGenreCategoryAsPartOfScreenValueTag() {
        when(section.getSectionId()).thenReturn(GenreCellPresenter.MUSIC_SECTION);
        presenter.setSectionForPosition(0, section, false);
        ExploreGenre exploreGenre = new ExploreGenre("Religion & Spirituality");

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));

        verify(itemView).setTag(startsWith("explore:music"));
    }

    @Test
    public void shouldSetAudioGenreCategoryAsPartOfScreenValueTag() {
        when(section.getSectionId()).thenReturn(GenreCellPresenter.AUDIO_SECTION);
        presenter.setSectionForPosition(0, section, false);
        ExploreGenre exploreGenre = new ExploreGenre("Religion & Spirituality");

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));

        verify(itemView).setTag(startsWith("explore:audio"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnrecognisedSectionValueProvided() {
        when(section.getSectionId()).thenReturn(3);
        presenter.setSectionForPosition(0, section, false);
        ExploreGenre exploreGenre = new ExploreGenre("Religion & Spirituality");

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));
    }

}