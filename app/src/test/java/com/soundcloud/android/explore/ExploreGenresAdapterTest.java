package com.soundcloud.android.explore;

import static com.google.common.collect.Lists.newArrayList;
import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
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

    private ExploreGenresAdapter adapter;
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
        adapter = new ExploreGenresAdapter(layoutInflater);
    }

    @Test
    public void shouldInflateExploreRowWithoutAttachingToParent() {
        adapter.createItemView(0, parentView);
        verify(layoutInflater).inflate(R.layout.explore_genre_item, parentView, false);
    }

    @Test
    public void shouldSetDisplayNameOnBindItemView() {
        when(exploreGenre.getTitle()).thenReturn("Genre Title");
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));

        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        adapter.onNext(section);
        adapter.bindItemView(0, itemView);

        verify(itemView).setDisplayName(eq("Genre Title"));
    }

    @Test
    public void shouldSetExpectedPostfixScreenValueAsTagToView() {

        when(exploreGenre.getTitle()).thenReturn("Religion & Spirituality");
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));

        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        adapter.onNext(section);
        adapter.bindItemView(0, itemView);

        verify(itemView).setTag(endsWith("religion_&_spirituality"));
    }

    @Test
    public void shouldSetMusicGenreCategoryAsPartOfScreenValueTag() {
        when(section.getSectionId()).thenReturn(1);

        when(exploreGenre.getTitle()).thenReturn("Religion & Spirituality");
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));

        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        adapter.onNext(section);
        adapter.bindItemView(0, itemView);

        verify(itemView).setTag(startsWith("explore:music"));
    }

    @Test
    public void shouldSetAudioGenreCategoryAsPartOfScreenValueTag() {
        when(section.getSectionId()).thenReturn(0);

        when(exploreGenre.getTitle()).thenReturn("Religion & Spirituality");
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));

        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        adapter.onNext(section);
        adapter.bindItemView(0, itemView);

        verify(itemView).setTag(startsWith("explore:audio"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUnrecognisedSectionValueProvided() {
        when(section.getSectionId()).thenReturn(3);
        when(section.getItems()).thenReturn(newArrayList(exploreGenre));
        when(itemView.getResources()).thenReturn(Robolectric.application.getResources());

        adapter.onNext(section);
        adapter.bindItemView(0, itemView);
    }

    @Test
    public void getViewTypeCountShouldReturn2ToDistinguishBetweenHeadersAndNormalItems() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldSetSectionViewTypes() {
        adapter.onNext(new Section(0, R.string.explore_genre_header_audio,
                Lists.newArrayList(new ExploreGenre("a"), new ExploreGenre("b"))));
        adapter.onNext(new Section(1, R.string.explore_genre_header_music,
                Lists.newArrayList(new ExploreGenre("c"))));

        expect(adapter.getItemViewType(0)).toEqual(ExploreGenresAdapter.ITEM_VIEW_TYPE_HEADER);
        expect(adapter.getItemViewType(1)).toEqual(ExploreGenresAdapter.ITEM_VIEW_TYPE_DEFAULT);
        expect(adapter.getItemViewType(2)).toEqual(ExploreGenresAdapter.ITEM_VIEW_TYPE_HEADER);
    }

    @Test
    public void shouldReturnSectionAssociatedToGivenPosition() {
        Section section1 = new Section(0, R.string.explore_genre_header_audio,
                Lists.newArrayList(mock(ExploreGenre.class), mock(ExploreGenre.class)));
        Section section2 = new Section(1, R.string.explore_genre_header_music,
                Lists.newArrayList(mock(ExploreGenre.class)));

        adapter.onNext(section1);
        adapter.onNext(section2);

        expect(adapter.getSection(0)).toBe(section1);
        expect(adapter.getSection(1)).toBe(section1);
        expect(adapter.getSection(2)).toBe(section2);
    }

    @Test
    public void shouldSetSectionHeaderOnViews() {
        adapter.onNext(new Section(0, R.string.explore_genre_header_audio,
                Lists.newArrayList(new ExploreGenre("a"), new ExploreGenre("b"))));
        adapter.onNext(new Section(1, R.string.explore_genre_header_music,
                Lists.newArrayList(new ExploreGenre("c"))));

        ExploreGenreCategoryRow sectionedRow = mock(ExploreGenreCategoryRow.class);
        when(sectionedRow.getResources()).thenReturn(Robolectric.application.getResources());

        adapter.bindItemView(0, sectionedRow);
        verify(sectionedRow).showSectionHeaderWithText("Audio");

        adapter.bindItemView(2, sectionedRow);
        verify(sectionedRow).showSectionHeaderWithText("Music");

        ExploreGenreCategoryRow sectionedRowNoHeader = mock(ExploreGenreCategoryRow.class);
        when(sectionedRowNoHeader.getResources()).thenReturn(Robolectric.application.getResources());

        adapter.bindItemView(1, sectionedRowNoHeader);
        verify(sectionedRowNoHeader).hideSectionHeader();
        verify(sectionedRowNoHeader, never()).showSectionHeaderWithText(anyString());
    }
}
