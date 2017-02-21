package com.soundcloud.android.search.topresults;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;

import java.util.Collections;

public class TopResultsBucketViewModelTest {

    private static final int TOTAL_RESULTS = 5;
    private static final Urn QUERY_URN = new Urn("soundcloud:query:123");
    private static final Urn TRACKS_BUCKET_URN = new Urn("soundcloud:search-buckets:freetiertracks");
    private static final Urn PLAYLISTS_BUCKET_URN = new Urn("soundcloud:search-buckets:playlists");
    private static final Urn TOP_RESULT_BUCKET_URN = new Urn("soundcloud:search-buckets:top");

    @Test
    public void doesNotShowViewAllForTopResult() throws Exception {
        final TopResultsBucketViewModel viewModel = TopResultsBucketViewModel.create(Collections.emptyList(), TOP_RESULT_BUCKET_URN, TOTAL_RESULTS, QUERY_URN);

        assertThat(viewModel.shouldShowViewAll()).isFalse();
        assertThat(viewModel.kind()).isEqualTo(TopResultsBucketViewModel.Kind.TOP_RESULT);
    }

    @Test
    public void doesNotShowViewAllForZeroResults() throws Exception {
        final TopResultsBucketViewModel viewModel = TopResultsBucketViewModel.create(Collections.emptyList(), TRACKS_BUCKET_URN, 0, QUERY_URN);

        assertThat(viewModel.shouldShowViewAll()).isFalse();
        assertThat(viewModel.kind()).isEqualTo(TopResultsBucketViewModel.Kind.TRACKS);
    }

    @Test
    public void showsViewAllForPlaylistsWithMoreResults() throws Exception {
        final TopResultsBucketViewModel viewModel = TopResultsBucketViewModel.create(Collections.emptyList(), PLAYLISTS_BUCKET_URN, 1, QUERY_URN);

        assertThat(viewModel.shouldShowViewAll()).isTrue();
        assertThat(viewModel.kind()).isEqualTo(TopResultsBucketViewModel.Kind.PLAYLISTS);
    }
}
