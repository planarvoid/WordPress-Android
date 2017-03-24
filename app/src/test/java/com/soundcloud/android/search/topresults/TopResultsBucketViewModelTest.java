package com.soundcloud.android.search.topresults;

import static com.soundcloud.android.search.topresults.TopResults.Bucket.Kind.TOP_RESULT;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.search.topresults.TopResults.Bucket.Kind;
import org.junit.Test;

import java.util.Collections;

public class TopResultsBucketViewModelTest {

    private static final int TOTAL_RESULTS = 5;

    @Test
    public void doesNotShowViewAllForTopResult() throws Exception {
        final TopResultsBucketViewModel viewModel = TopResultsBucketViewModel.create(Collections.emptyList(), TOP_RESULT, TOTAL_RESULTS);

        assertThat(viewModel.shouldShowViewAll()).isFalse();
        assertThat(viewModel.kind()).isEqualTo(Kind.TOP_RESULT);
    }

    @Test
    public void doesNotShowViewAllForLessThanLimitResults() throws Exception {
        final TopResultsBucketViewModel viewModel = TopResultsBucketViewModel.create(Collections.emptyList(), Kind.TRACKS, 2);

        assertThat(viewModel.shouldShowViewAll()).isFalse();
        assertThat(viewModel.kind()).isEqualTo(Kind.TRACKS);
    }

    @Test
    public void showsViewAllForPlaylistsWithMoreResults() throws Exception {
        final TopResultsBucketViewModel viewModel = TopResultsBucketViewModel.create(Collections.emptyList(), Kind.PLAYLISTS, 3);

        assertThat(viewModel.shouldShowViewAll()).isTrue();
        assertThat(viewModel.kind()).isEqualTo(Kind.PLAYLISTS);
    }
}
