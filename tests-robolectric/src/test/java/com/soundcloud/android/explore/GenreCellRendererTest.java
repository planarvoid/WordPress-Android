package com.soundcloud.android.explore;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Locale;

@RunWith(SoundCloudTestRunner.class)
public class GenreCellRendererTest {

    @InjectMocks
    private GenreCellRenderer presenter;

    @Mock
    private ViewGroup parentView;
    @Mock
    private View itemView;
    @Mock
    private GenreSection section;
    @Mock
    private TextView genreTitleText;
    @Mock
    private TextView sectionHeaderText;

    @Before
    public void setup() {
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());
        when(itemView.findViewById(android.R.id.text1)).thenReturn(genreTitleText);
        when(itemView.findViewById(R.id.list_section_header)).thenReturn(sectionHeaderText);
        when(section.getTitleId()).thenReturn(R.string.explore_category_trending_audio);
        when(parentView.getContext()).thenReturn(Robolectric.application);
    }

    @Test
    public void shouldShowSectionHeaderForRowIfItemIsSectionHeader() {
        ExploreGenre exploreGenre = new ExploreGenre("Genre Title");

        presenter.setSectionForPosition(0, section, true);

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));
        verify(sectionHeaderText).setText(anyString().toUpperCase(Locale.getDefault()));
        verify(sectionHeaderText).setVisibility(View.VISIBLE);
    }

    @Test
    public void shouldHideSectionHeaderForRowIfItemIsNotASectionHeader() {
        ExploreGenre exploreGenre = new ExploreGenre("Genre Title");

        presenter.setSectionForPosition(0, section, false);

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));
        verify(sectionHeaderText).setVisibility(View.GONE);
    }

    @Test
    public void shouldSetDisplayNameOnBindItemView() {
        presenter.setSectionForPosition(0, section, false);
        ExploreGenre exploreGenre = new ExploreGenre("Genre Title");

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));

        verify(genreTitleText).setText(eq("Genre Title"));
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
        when(section.getSectionId()).thenReturn(GenreCellRenderer.MUSIC_SECTION);
        presenter.setSectionForPosition(0, section, false);
        ExploreGenre exploreGenre = new ExploreGenre("Religion & Spirituality");

        presenter.bindItemView(0, itemView, Arrays.asList(exploreGenre));

        verify(itemView).setTag(startsWith("explore:music"));
    }

    @Test
    public void shouldSetAudioGenreCategoryAsPartOfScreenValueTag() {
        when(section.getSectionId()).thenReturn(GenreCellRenderer.AUDIO_SECTION);
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