package com.soundcloud.android.adapter;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.model.behavior.InSection;
import com.soundcloud.android.model.behavior.Titled;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.view.adapter.behavior.SectionedListRow;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

@RunWith(SoundCloudTestRunner.class)
public class SectionedAdapterTest {

    private SectionedAdapter adapter;

    @Before
    public void setup() throws CreateModelException {
        adapter = new SectionedAdapter() {
            @Override
            protected View createItemView(int position, ViewGroup parent) {
                return null;
            }
        };
    }

    @Test
    public void shouldSetSectionViewTypes() {
        Titled section1 = getTitledObject("section1");
        Titled section2 = getTitledObject("section2");

        adapter.addItem(createSectionedItem(section1));
        adapter.addItem(createSectionedItem(section1));
        adapter.addItem(createSectionedItem(section2));

        expect(adapter.getItemViewType(0)).toEqual(ExploreTracksCategoriesAdapter.VIEW_TYPE_SECTION);
        expect(adapter.getItemViewType(1)).toEqual(ExploreTracksCategoriesAdapter.VIEW_TYPE_DEFAULT);
        expect(adapter.getItemViewType(2)).toEqual(ExploreTracksCategoriesAdapter.VIEW_TYPE_SECTION);
    }

    @Test
    public void shouldSetSectionHeaderOnViews() {
        Titled section1 = getTitledObject("section1");
        Titled section2 = getTitledObject("section2");

        adapter.addItem(createSectionedItem(section1));
        adapter.addItem(createSectionedItem(section1));
        adapter.addItem(createSectionedItem(section2));

        SectionedTestListRow sectionedRow1 = Mockito.mock(SectionedTestListRow.class);
        adapter.bindItemView(0, sectionedRow1);
        verify(sectionedRow1).showSectionHeaderWithText("section1");

        SectionedTestListRow sectionedRow2 = Mockito.mock(SectionedTestListRow.class);
        adapter.bindItemView(1, sectionedRow2);
        verify(sectionedRow2).hideSectionHeader();
        verify(sectionedRow2, never()).showSectionHeaderWithText(anyString());

        SectionedTestListRow sectionedRow3 = Mockito.mock(SectionedTestListRow.class);
        adapter.bindItemView(2, sectionedRow3);
        verify(sectionedRow3).showSectionHeaderWithText("section2");
    }

    private InSection createSectionedItem(final Titled section) {
        return new InSection() {
            @Override
            public Titled getSection() {
                return section;
            }
        };
    }

    private Titled getTitledObject(final String sectionText) {
        return new Titled() {
            @Override
            public String getTitle(Resources resources) {
                return sectionText;
            }
        };
    }

    private static class SectionedTestListRow extends View implements SectionedListRow{

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
