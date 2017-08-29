package com.soundcloud.android.tracks;


import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.trackItem;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Locale;

public class TrackInfoPresenterTest extends AndroidUnitTest {

    private View view;
    private TrackInfoPresenter presenter;

    @Mock ChangeLikeToSaveExperiment changeLikeToSaveExperiment;
    @Mock TrackInfoPresenter.CommentClickListener commentClickListener;
    @Mock TrackStatsDisplayPolicy trackStatsDisplayPolicy;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());
    private TrackItem trackItem;

    @Before
    public void setUp() throws Exception {
        trackItem = trackItem(PlayableFixtures.expectedTrackBuilderForPlayer().likesCount(10).repostsCount(10).playCount(10).build());
        presenter = new TrackInfoPresenter(resources(), numberFormatter, changeLikeToSaveExperiment, trackStatsDisplayPolicy);
        view = presenter.create(LayoutInflater.from(context()), new FrameLayout(context()));
    }

    @Test
    public void bindViewsShowsAllStatsWhenAllStatsAreVisible() throws Exception {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(true);
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(true);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(true);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsHideAllViewsWhenAllStatsAreHidden() throws Exception {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(false);
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(false);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(false);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsHidePlaysViewIfPlaysCountIsHidden() throws Exception {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(false);
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(true);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(true);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsHideLikesViewIfLikesCountIsHidden() throws Exception {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(true);
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(false);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(true);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsHideRepostsViewIfRepostsCountIsHidden() throws Exception {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(true);
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(true);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(false);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsOnlyShowsPlaysViewWhenLikesAndRepostsAreHidden() throws Exception {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(true);
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(false);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(false);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsOnlyShowsLikesViewWhenPlaysAndRepostsAreHidden() throws Exception {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(false);
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(true);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(false);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsOnlyShowsRepostsWhenPlaysAndLikesAreHidden() throws Exception {
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(false);
        when(trackStatsDisplayPolicy.displayLikesCount(any())).thenReturn(false);
        when(trackStatsDisplayPolicy.displayRepostsCount(any())).thenReturn(true);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsShouldHideCommentsViewWhenCommentsAreHidden() throws Exception {
        when(trackStatsDisplayPolicy.displayCommentsCount(any())).thenReturn(false);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.comments).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsShouldShowCommentsViewWhenCommentsAreShown() throws Exception {
        when(trackStatsDisplayPolicy.displayCommentsCount(any())).thenReturn(true);

        presenter.bind(view, trackItem, commentClickListener);

        assertThat(view.findViewById(R.id.comments).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsShouldShowNoDescriptionWhenDescriptionIsEmpty() throws Exception {
        TrackItem trackItem = trackItem(PlayableFixtures.expectedTrackBuilderForPlayer().description(Optional.of("")).build());

        presenter.bindDescription(view, trackItem);

        assertThat(view.findViewById(R.id.description).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.no_description).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test // fixes #2863
    public void bindViewsShouldHideNoDescriptionWhenDescriptionIsNotEmpty() throws Exception {
        TrackItem trackItem = trackItem(PlayableFixtures.expectedTrackBuilderForPlayer().description(Optional.of("some desc")).build());

        presenter.bindDescription(view, trackItem);

        assertThat(view.findViewById(R.id.description).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.no_description).getVisibility()).isEqualTo(View.GONE);
    }
}
