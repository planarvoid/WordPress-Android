package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Section;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.view.adapter.behavior.SectionedListRow;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class SectionedAdapterTest {

    private SectionedAdapter adapter;
    @Mock
    private ListFragmentObserver listFragmentObserver;

    @Before
    public void setup() {
        adapter = new SectionedAdapter(listFragmentObserver) {
            @Override
            protected View createItemView(int position, ViewGroup parent) {
                return null;
            }
        };
    }

    @Test
    public void onNextShouldCallThroughToFragmentObserver(){
        final Section section = mock(Section.class);
        adapter.onNext(section);
        verify(listFragmentObserver).onNext(section);
    }

    @Test
    public void onCompleteShouldCallThroughToFragmentObserver(){
        adapter.onCompleted();
        verify(listFragmentObserver).onCompleted();
    }

    @Test
    public void onErrorShouldCallThroughToFragmentObserver(){
        final Exception error = mock(Exception.class);
        adapter.onError(error);
        verify(listFragmentObserver).onError(error);
    }

    @Test
    public void shouldClearSectionsArrayOnClear(){
        final SparseArray listPositionsToSections = mock(SparseArray.class);
        adapter = new SectionedAdapter(listPositionsToSections, listFragmentObserver) {
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
    public void getViewTypeCountShouldReturnViewTypesCount(){
        expect(adapter.getViewTypeCount()).toBe(SectionedAdapter.ViewTypes.values().length);
    }

    @Test
    public void shouldSetSectionViewTypes() {
        adapter.onNext(new Section(R.string.discover_category_header_audio, Lists.newArrayList(1,2)));
        adapter.onNext(new Section(R.string.discover_category_header_music, Lists.newArrayList(1)));

        expect(adapter.getItemViewType(0)).toEqual(SectionedAdapter.ViewTypes.SECTION.ordinal());
        expect(adapter.getItemViewType(1)).toEqual(SectionedAdapter.ViewTypes.DEFAULT.ordinal());
        expect(adapter.getItemViewType(2)).toEqual(SectionedAdapter.ViewTypes.SECTION.ordinal());
    }

    @Test
    public void shouldSetSectionHeaderOnViews() {
        adapter.onNext(new Section(R.string.discover_category_header_audio, Lists.newArrayList(1,2)));
        adapter.onNext(new Section(R.string.discover_category_header_music, Lists.newArrayList(1)));

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
