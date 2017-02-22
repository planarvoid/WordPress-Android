package com.soundcloud.android.tracks;


import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
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

    @Mock TrackInfoPresenter.CommentClickListener commentClickListener;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());

    @Before
    public void setUp() throws Exception {
        presenter = new TrackInfoPresenter(resources(), numberFormatter);
        view = presenter.create(LayoutInflater.from(context()), new FrameLayout(context()));
    }

    @Test
    public void bindViewsShowsAllStatsWhenAllStatsAreGreaterZero() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().likesCount(10).repostsCount(10).playCount(10).build();

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsHideAllStatsWhenStatsAreZero() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().likesCount(0).repostsCount(0).playCount(0).build();

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsHidePlaysIfPlaysCountIsZero() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().likesCount(10).repostsCount(10).playCount(0).build();

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsHideLikesIfLikesCountIsZero() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().likesCount(0).repostsCount(10).playCount(10).build();

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsHideRepostsIfRepostsCountIsZero() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().likesCount(10).repostsCount(0).playCount(10).build();

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsOnlyShowsPlaysWhenLikesAndRepostsAreZero() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().likesCount(0).repostsCount(0).playCount(10).build();

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsOnlyShowsLikesWhenPlaysAndRepostsAreZero() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().likesCount(10).repostsCount(0).playCount(0).build();

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsOnlyShowsRepostsWhenPlaysAndLikesAreZero() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().likesCount(0).repostsCount(10).playCount(0).build();

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsShouldHideCommentsWhenCommentsAreZero() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().commentsCount(0).build();

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.comments).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsShouldShowNoDescriptionWhenDescriptionIsEmpty() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().description(Optional.of("")).build();

        presenter.bindDescription(view, trackProperties);

        assertThat(view.findViewById(R.id.description).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.no_description).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test // fixes #2863
    public void bindViewsShouldHideNoDescriptionWhenDescriptionIsNotEmpty() throws Exception {
        TrackItem trackProperties = PlayableFixtures.expectedTrackBuilderForPlayer().description(Optional.of("some desc")).build();

        presenter.bindDescription(view, trackProperties);

        assertThat(view.findViewById(R.id.description).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.no_description).getVisibility()).isEqualTo(View.GONE);
    }
}
