package com.soundcloud.android.tracks;


import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.collections.PropertySet;
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
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer()
                                                      .put(PlayableProperty.LIKES_COUNT, 10)
                                                      .put(PlayableProperty.REPOSTS_COUNT, 10)
                                                      .put(TrackProperty.PLAY_COUNT, 10);

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsHideAllStatsWhenStatsAreZero() throws Exception {
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer()
                                                      .put(PlayableProperty.LIKES_COUNT, 0)
                                                      .put(PlayableProperty.REPOSTS_COUNT, 0)
                                                      .put(TrackProperty.PLAY_COUNT, 0);

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsHidePlaysIfPlaysCountIsZero() throws Exception {
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer()
                                                      .put(PlayableProperty.LIKES_COUNT, 10)
                                                      .put(PlayableProperty.REPOSTS_COUNT, 10)
                                                      .put(TrackProperty.PLAY_COUNT, 0);

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsHideLikesIfLikesCountIsZero() throws Exception {
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer()
                                                      .put(PlayableProperty.LIKES_COUNT, 0)
                                                      .put(PlayableProperty.REPOSTS_COUNT, 10)
                                                      .put(TrackProperty.PLAY_COUNT, 10);

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsHideRepostsIfRepostsCountIsZero() throws Exception {
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer()
                                                      .put(PlayableProperty.LIKES_COUNT, 10)
                                                      .put(PlayableProperty.REPOSTS_COUNT, 0)
                                                      .put(TrackProperty.PLAY_COUNT, 10);

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsOnlyShowsPlaysWhenLikesAndRepostsAreZero() throws Exception {
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer()
                                                      .put(PlayableProperty.LIKES_COUNT, 0)
                                                      .put(PlayableProperty.REPOSTS_COUNT, 0)
                                                      .put(TrackProperty.PLAY_COUNT, 10);

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsOnlyShowsLikesWhenPlaysAndRepostsAreZero() throws Exception {
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer()
                                                      .put(PlayableProperty.LIKES_COUNT, 10)
                                                      .put(PlayableProperty.REPOSTS_COUNT, 0)
                                                      .put(TrackProperty.PLAY_COUNT, 0);

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsOnlyShowsRepostsWhenPlaysAndLikesAreZero() throws Exception {
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer()
                                                      .put(PlayableProperty.LIKES_COUNT, 0)
                                                      .put(PlayableProperty.REPOSTS_COUNT, 10)
                                                      .put(TrackProperty.PLAY_COUNT, 0);

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.plays).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider1).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.likes).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.divider2).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.reposts).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void bindViewsShouldHideCommentsWhenCommentsAreZero() throws Exception {
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer().put(TrackProperty.COMMENTS_COUNT, 0);

        presenter.bind(view, trackProperties, commentClickListener);

        assertThat(view.findViewById(R.id.comments).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void bindViewsShouldShowNoDescriptionWhenDescriptionIsEmpty() throws Exception {
        PropertySet trackProperties = TestPropertySets.expectedTrackForPlayer().put(TrackProperty.DESCRIPTION, "");

        presenter.bindDescription(view, trackProperties);

        assertThat(view.findViewById(R.id.description).getVisibility()).isEqualTo(View.GONE);
        assertThat(view.findViewById(R.id.no_description).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test // fixes #2863
    public void bindViewsShouldHideNoDescriptionWhenDescriptionIsNotEmpty() throws Exception {
        PropertySet trackProperties = TestPropertySets
                .expectedTrackForPlayer()
                .put(TrackProperty.DESCRIPTION, "some desc");

        presenter.bindDescription(view, trackProperties);

        assertThat(view.findViewById(R.id.description).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(view.findViewById(R.id.no_description).getVisibility()).isEqualTo(View.GONE);
    }
}