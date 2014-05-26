package com.soundcloud.android.explore;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class ExploreGenresAdapterTest {

    private ExploreGenresAdapter adapter;

    @Mock
    private GenreCellPresenter cellPresenter;

    @Before
    public void setUp() throws Exception {
        adapter = new ExploreGenresAdapter(cellPresenter);
    }

    @Test
    public void shouldSetSectionViewTypes() {
        when(cellPresenter.isSectionHeader(0)).thenReturn(true);
        when(cellPresenter.isSectionHeader(1)).thenReturn(false);
        when(cellPresenter.isSectionHeader(2)).thenReturn(true);
        expect(adapter.getItemViewType(0)).toEqual(ExploreGenresAdapter.ITEM_VIEW_TYPE_HEADER);
        expect(adapter.getItemViewType(1)).toEqual(ExploreGenresAdapter.ITEM_VIEW_TYPE_DEFAULT);
        expect(adapter.getItemViewType(2)).toEqual(ExploreGenresAdapter.ITEM_VIEW_TYPE_HEADER);
    }

    @Test
    public void getViewTypeCountShouldReturn2ToDistinguishBetweenHeadersAndNormalItems() {
        expect(adapter.getViewTypeCount()).toBe(2);
    }

    @Test
    public void shouldMakeFirstRowOfEachSectionASectionHeader() {
        GenreSection section = new GenreSection(0, 0, Arrays.asList(new ExploreGenre(), new ExploreGenre()));
        adapter.onNext(section);
        InOrder inOrder = inOrder(cellPresenter);
        inOrder.verify(cellPresenter).setSectionForPosition(0, section, true);
        inOrder.verify(cellPresenter).setSectionForPosition(1, section, false);
    }
}
