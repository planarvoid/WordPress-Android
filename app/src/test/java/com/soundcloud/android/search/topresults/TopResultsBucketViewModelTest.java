package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResultsBucketViewModel.Kind.TOP_RESULT;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

import java.util.Collections;

public class TopResultsBucketViewModelTest {

    private static final int TOTAL_RESULTS = 5;
    private static final String SEARCH_QUERY = "fdsa";
    private static final ClickParams CLICK_PARAMS = ClickParams.create(Urn.NOT_SET, "", Optional.absent(), 0, Module.create("module", 1),
                                                                       Screen.SEARCH_TOP_RESULTS,
                                                                       SearchEvent.ClickSource.GO_TRACKS_BUCKET);

    @Test
    public void doesNotShowViewAllForTopResult() throws Exception {
        final TopResultsBucketViewModel viewModel = TopResultsBucketViewModel.create(Collections.emptyList(), TOP_RESULT, TOTAL_RESULTS, SEARCH_QUERY,
                                                                                     CLICK_PARAMS);

        assertThat(viewModel.viewAllAction().isPresent()).isFalse();
        assertThat(viewModel.kind()).isEqualTo(TopResultsBucketViewModel.Kind.TOP_RESULT);
    }

    @Test
    public void doesNotShowViewAllForLessThanLimitResults() throws Exception {
        final TopResultsBucketViewModel viewModel = TopResultsBucketViewModel.create(Collections.emptyList(), TopResultsBucketViewModel.Kind.TRACKS, 2, SEARCH_QUERY, CLICK_PARAMS);

        assertThat(viewModel.viewAllAction().isPresent()).isFalse();
        assertThat(viewModel.kind()).isEqualTo(TopResultsBucketViewModel.Kind.TRACKS);
    }

    @Test
    public void showsViewAllForPlaylistsWithMoreResults() throws Exception {
        final TopResultsBucketViewModel viewModel = TopResultsBucketViewModel.create(Collections.emptyList(), TopResultsBucketViewModel.Kind.PLAYLISTS, 3, SEARCH_QUERY, CLICK_PARAMS);

        assertThat(viewModel.viewAllAction().isPresent()).isTrue();
        assertThat(viewModel.kind()).isEqualTo(TopResultsBucketViewModel.Kind.PLAYLISTS);
    }
}
