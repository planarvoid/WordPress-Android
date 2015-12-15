package com.soundcloud.android.explore;

import static org.mockito.Mockito.inOrder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ExploreGenresAdapterTest {

    @Mock private GenreCellRenderer renderer;

    private ExploreGenresAdapter adapter;

    @Before
    public void setUp() throws Exception {
        adapter = new ExploreGenresAdapter(renderer);
    }

    @Test
    public void shouldMakeFirstRowOfEachSectionASectionHeader() {
        final List<ExploreGenre> genres = Arrays.asList(new ExploreGenre(), new ExploreGenre());
        adapter.onNext(genres);

        final GenreSection section = new GenreSection<>(0, 0, genres);
        adapter.demarcateSection(section);

        InOrder inOrder = inOrder(renderer);
        inOrder.verify(renderer).setSectionForPosition(0, section, true);
        inOrder.verify(renderer).setSectionForPosition(1, section, false);
    }
}
