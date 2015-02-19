package com.soundcloud.android.explore;

import static org.mockito.Mockito.inOrder;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

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
    public void shouldMakeFirstRowOfEachSectionASectionHeader() {
        final List<ExploreGenre> genres = Arrays.asList(new ExploreGenre(), new ExploreGenre());
        adapter.onNext(genres);
        GenreSection section = new GenreSection(0, 0, genres);
        adapter.demarcateSection(section);
        InOrder inOrder = inOrder(cellPresenter);
        inOrder.verify(cellPresenter).setSectionForPosition(0, section, true);
        inOrder.verify(cellPresenter).setSectionForPosition(1, section, false);
    }
}
