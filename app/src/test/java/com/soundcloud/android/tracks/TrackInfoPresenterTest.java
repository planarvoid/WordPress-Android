package com.soundcloud.android.tracks;


import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class TrackInfoPresenterTest extends TestCase {
    private View view;
    private PropertySet trackProperties;
    private TrackInfoPresenter presenter;

    @Before
    public void setUp() throws Exception {
        trackProperties = PropertySet.from(
                PlayableProperty.TITLE.bind("Title"),
                PlayableProperty.CREATOR_NAME.bind("Creator"),
                PlayableProperty.CREATED_AT.bind(new Date()),
                PlayableProperty.IS_PRIVATE.bind(false),
                TrackProperty.COMMENTS_COUNT.bind(10));

        presenter = new TrackInfoPresenter(Robolectric.application.getResources());
        view = presenter.create(LayoutInflater.from(Robolectric.application), new FrameLayout(Robolectric.application));
    }

    @Test
    public void bindViewsShowsAllStatsWhenAllStatsAreGreaterZero() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 10)
                .put(PlayableProperty.REPOSTS_COUNT, 10)
                .put(TrackProperty.PLAY_COUNT, 10);

        presenter.bind(view, trackProperties);

        expect(view.findViewById(R.id.plays)).toBeVisible();
        expect(view.findViewById(R.id.divider1)).toBeVisible();
        expect(view.findViewById(R.id.likes)).toBeVisible();
        expect(view.findViewById(R.id.divider2)).toBeVisible();
        expect(view.findViewById(R.id.reposts)).toBeVisible();
    }

    @Test
    public void bindViewsHideAllStatsWhenStatsAreZero() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 0)
                .put(PlayableProperty.REPOSTS_COUNT, 0)
                .put(TrackProperty.PLAY_COUNT, 0);

        presenter.bind(view, trackProperties);

        expect(view.findViewById(R.id.plays)).toBeGone();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeGone();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeGone();
    }

    @Test
    public void bindViewsHidePlaysIfPlaysCountIsZero() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 10)
                .put(PlayableProperty.REPOSTS_COUNT, 10)
                .put(TrackProperty.PLAY_COUNT, 0);

        presenter.bind(view, trackProperties);

        expect(view.findViewById(R.id.plays)).toBeGone();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeVisible();
        expect(view.findViewById(R.id.divider2)).toBeVisible();
        expect(view.findViewById(R.id.reposts)).toBeVisible();
    }

    @Test
    public void bindViewsHideLikesIfLikesCountIsZero() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 0)
                .put(PlayableProperty.REPOSTS_COUNT, 10)
                .put(TrackProperty.PLAY_COUNT, 10);

        presenter.bind(view, trackProperties);

        expect(view.findViewById(R.id.plays)).toBeVisible();
        expect(view.findViewById(R.id.divider1)).toBeVisible();
        expect(view.findViewById(R.id.likes)).toBeGone();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeVisible();
    }

    @Test
    public void bindViewsHideRepostsIfRepostsCountIsZero() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 10)
                .put(PlayableProperty.REPOSTS_COUNT, 0)
                .put(TrackProperty.PLAY_COUNT, 10);

        presenter.bind(view, trackProperties);

        expect(view.findViewById(R.id.plays)).toBeVisible();
        expect(view.findViewById(R.id.divider1)).toBeVisible();
        expect(view.findViewById(R.id.likes)).toBeVisible();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeGone();
    }

    @Test
    public void bindViewsOnlyShowsPlaysWhenLikesAndRepostsAreZero() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 0)
                .put(PlayableProperty.REPOSTS_COUNT, 0)
                .put(TrackProperty.PLAY_COUNT, 10);

        presenter.bind(view, trackProperties);

        expect(view.findViewById(R.id.plays)).toBeVisible();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeGone();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeGone();
    }

    @Test
    public void bindViewsOnlyShowsLikesWhenPlaysAndRepostsAreZero() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 10)
                .put(PlayableProperty.REPOSTS_COUNT, 0)
                .put(TrackProperty.PLAY_COUNT, 0);

        presenter.bind(view, trackProperties);

        expect(view.findViewById(R.id.plays)).toBeGone();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeVisible();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeGone();
    }

    @Test
    public void bindViewsOnlyShowsRepostsWhenPlaysAndLikesAreZero() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 0)
                .put(PlayableProperty.REPOSTS_COUNT, 10)
                .put(TrackProperty.PLAY_COUNT, 0);

        presenter.bind(view, trackProperties);

        expect(view.findViewById(R.id.plays)).toBeGone();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeGone();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeVisible();
    }

    @Test
    public void bindViewsShouldHideCommentsWhenCommentsAreZero() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 0)
                .put(PlayableProperty.REPOSTS_COUNT, 10)
                .put(TrackProperty.PLAY_COUNT, 0)
                .put(TrackProperty.COMMENTS_COUNT, 0);

        presenter.bind(view, trackProperties);

        expect(view.findViewById(R.id.comments)).toBeGone();
    }

    @Test
    public void bindViewsShouldShowNoDescriptionWhenDescriptionIsEmpty() throws Exception {
        trackProperties
                .put(PlayableProperty.LIKES_COUNT, 0)
                .put(PlayableProperty.REPOSTS_COUNT, 10)
                .put(TrackProperty.PLAY_COUNT, 0)
                .put(TrackProperty.DESCRIPTION, "");

        presenter.bindDescription(view, trackProperties);

        expect(view.findViewById(R.id.description)).toBeGone();
        expect(view.findViewById(R.id.no_description)).toBeVisible();
    }
}