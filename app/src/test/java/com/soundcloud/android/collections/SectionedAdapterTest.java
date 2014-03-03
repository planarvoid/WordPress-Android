package com.soundcloud.android.collections;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.observers.ListFragmentSubscriber;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class SectionedAdapterTest {

    private SectionedAdapter adapter;
    @Mock
    private ListFragmentSubscriber listFragmentSubscriber;

    @Before
    public void setup() {
        adapter = new SectionedAdapter() {
            @Override
            protected View createItemView(int position, ViewGroup parent) {
                return null;
            }
        };
    }

    @Test
    public void shouldClearSectionsArrayOnClear(){
        final SparseArray listPositionsToSections = mock(SparseArray.class);
        adapter = new SectionedAdapter(listPositionsToSections) {
            @Override
            protected View createItemView(int position, ViewGroup parent) {
                return null;
            }
        };
        adapter.clear();
        verify(listPositionsToSections).clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionIfNotBindingToSectionedListRow(){
        adapter.bindItemView(0, new View(Robolectric.application));
    }

    @Test
    public void getViewTypeCountShouldReturn2ToDistinguishBetweenHeadersAndNormalItems() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldSetSectionViewTypes() {
        adapter.onNext(new Section(0, R.string.explore_genre_header_audio,
                Lists.newArrayList(mock(Parcelable.class), mock(Parcelable.class))));
        adapter.onNext(new Section(1, R.string.explore_genre_header_music,
                Lists.newArrayList(mock(Parcelable.class))));

        expect(adapter.getItemViewType(0)).toEqual(SectionedAdapter.ITEM_VIEW_TYPE_HEADER);
        expect(adapter.getItemViewType(1)).toEqual(SectionedAdapter.ITEM_VIEW_TYPE_DEFAULT);
        expect(adapter.getItemViewType(2)).toEqual(SectionedAdapter.ITEM_VIEW_TYPE_HEADER);
    }

    @Test
    public void shouldReturnSectionAssociatedToGivenPosition() {
        Section section1 = new Section(0, R.string.explore_genre_header_audio,
                Lists.newArrayList(mock(Parcelable.class), mock(Parcelable.class)));
        Section section2 = new Section(1, R.string.explore_genre_header_music,
                Lists.newArrayList(mock(Parcelable.class)));

        adapter.onNext(section1);
        adapter.onNext(section2);

        expect(adapter.getSection(0)).toBe(section1);
        expect(adapter.getSection(1)).toBe(section1);
        expect(adapter.getSection(2)).toBe(section2);
    }

    @Test
    public void shouldSetSectionHeaderOnViews() {
        adapter.onNext(new Section(0, R.string.explore_genre_header_audio,
                Lists.newArrayList(mock(Parcelable.class), mock(Parcelable.class))));
        adapter.onNext(new Section(1, R.string.explore_genre_header_music,
                Lists.newArrayList(mock(Parcelable.class))));

        SectionedTestListRow sectionedRow = mock(SectionedTestListRow.class);
        when(sectionedRow.getResources()).thenReturn(Robolectric.application.getResources());

        adapter.bindItemView(0, sectionedRow);
        verify(sectionedRow).showSectionHeaderWithText("Audio");

        adapter.bindItemView(2, sectionedRow);
        verify(sectionedRow).showSectionHeaderWithText("Music");

        SectionedTestListRow sectionedRowNoHeader = mock(SectionedTestListRow.class);
        when(sectionedRowNoHeader.getResources()).thenReturn(Robolectric.application.getResources());

        adapter.bindItemView(1, sectionedRowNoHeader);
        verify(sectionedRowNoHeader).hideSectionHeader();
        verify(sectionedRowNoHeader, never()).showSectionHeaderWithText(anyString());
    }

    private static class SectionedTestListRow extends View implements SectionedListRow {

        public SectionedTestListRow(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public void showSectionHeaderWithText(String text) {
        }

        @Override
        public void hideSectionHeader() {
        }
    }

}
