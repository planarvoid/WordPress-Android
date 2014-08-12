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
    private TrackInfoPresenter presenter;
    private View view;

    @Before
    public void setUp() throws Exception {
        presenter = new TrackInfoPresenter(Robolectric.application.getResources());

        view = presenter.create(LayoutInflater.from(Robolectric.application), new FrameLayout(Robolectric.application));
    }

    @Test
    public void bindViewsShowsAllStatsWhenAllStatsAreGreaterZero() throws Exception {
        presenter.bind(view, PropertySet.from(
                PlayableProperty.TITLE.bind("Title"),
                PlayableProperty.CREATOR_NAME.bind("Creator"),
                PlayableProperty.LIKES_COUNT.bind(10),
                PlayableProperty.REPOSTS_COUNT.bind(10),
                TrackProperty.PLAY_COUNT.bind(10),
                TrackProperty.COMMENTS_COUNT.bind(10),
                PlayableProperty.CREATED_AT.bind(new Date())
        ));

        expect(view.findViewById(R.id.plays)).toBeVisible();
        expect(view.findViewById(R.id.divider1)).toBeVisible();
        expect(view.findViewById(R.id.likes)).toBeVisible();
        expect(view.findViewById(R.id.divider2)).toBeVisible();
        expect(view.findViewById(R.id.reposts)).toBeVisible();
    }

    @Test
    public void bindViewsHideAllStatsWhenStatsAreZero() throws Exception {
        presenter.bind(view, PropertySet.from(
                PlayableProperty.TITLE.bind("Title"),
                PlayableProperty.CREATOR_NAME.bind("Creator"),
                PlayableProperty.LIKES_COUNT.bind(0),
                PlayableProperty.REPOSTS_COUNT.bind(0),
                TrackProperty.PLAY_COUNT.bind(0),
                TrackProperty.COMMENTS_COUNT.bind(10),
                PlayableProperty.CREATED_AT.bind(new Date())
        ));

        expect(view.findViewById(R.id.plays)).toBeGone();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeGone();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeGone();
    }

    @Test
    public void bindViewsHidePlaysIfPlaysCountIsZero() throws Exception {
        presenter.bind(view, PropertySet.from(
                PlayableProperty.TITLE.bind("Title"),
                PlayableProperty.CREATOR_NAME.bind("Creator"),
                TrackProperty.PLAY_COUNT.bind(0),
                PlayableProperty.LIKES_COUNT.bind(10),
                PlayableProperty.REPOSTS_COUNT.bind(10),
                TrackProperty.COMMENTS_COUNT.bind(10),
                PlayableProperty.CREATED_AT.bind(new Date())
        ));

        expect(view.findViewById(R.id.plays)).toBeGone();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeVisible();
        expect(view.findViewById(R.id.divider2)).toBeVisible();
        expect(view.findViewById(R.id.reposts)).toBeVisible();
    }

    @Test
    public void bindViewsHideLikesIfLikesCountIsZero() throws Exception {
        presenter.bind(view, PropertySet.from(
                PlayableProperty.TITLE.bind("Title"),
                PlayableProperty.CREATOR_NAME.bind("Creator"),
                TrackProperty.PLAY_COUNT.bind(10),
                PlayableProperty.LIKES_COUNT.bind(0),
                PlayableProperty.REPOSTS_COUNT.bind(10),
                TrackProperty.COMMENTS_COUNT.bind(10),
                PlayableProperty.CREATED_AT.bind(new Date())
        ));

        expect(view.findViewById(R.id.plays)).toBeVisible();
        expect(view.findViewById(R.id.divider1)).toBeVisible();
        expect(view.findViewById(R.id.likes)).toBeGone();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeVisible();
    }

    @Test
    public void bindViewsHideRepostsIfRepostsCountIsZero() throws Exception {
        presenter.bind(view, PropertySet.from(
                PlayableProperty.TITLE.bind("Title"),
                PlayableProperty.CREATOR_NAME.bind("Creator"),
                TrackProperty.PLAY_COUNT.bind(10),
                PlayableProperty.LIKES_COUNT.bind(10),
                PlayableProperty.REPOSTS_COUNT.bind(0),
                TrackProperty.COMMENTS_COUNT.bind(10),
                PlayableProperty.CREATED_AT.bind(new Date())
        ));

        expect(view.findViewById(R.id.plays)).toBeVisible();
        expect(view.findViewById(R.id.divider1)).toBeVisible();
        expect(view.findViewById(R.id.likes)).toBeVisible();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeGone();
    }

    @Test
    public void bindViewsOnlyShowsPlaysWhenLikesAndRepostsAreZero() throws Exception {
        presenter.bind(view, PropertySet.from(
                PlayableProperty.TITLE.bind("Title"),
                PlayableProperty.CREATOR_NAME.bind("Creator"),
                TrackProperty.PLAY_COUNT.bind(10),
                PlayableProperty.LIKES_COUNT.bind(0),
                PlayableProperty.REPOSTS_COUNT.bind(0),
                TrackProperty.COMMENTS_COUNT.bind(10),
                PlayableProperty.CREATED_AT.bind(new Date())
        ));

        expect(view.findViewById(R.id.plays)).toBeVisible();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeGone();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeGone();
    }

    @Test
    public void bindViewsOnlyShowsLikesWhenPlaysAndRepostsAreZero() throws Exception {
        presenter.bind(view, PropertySet.from(
                PlayableProperty.TITLE.bind("Title"),
                PlayableProperty.CREATOR_NAME.bind("Creator"),
                TrackProperty.PLAY_COUNT.bind(0),
                PlayableProperty.LIKES_COUNT.bind(10),
                PlayableProperty.REPOSTS_COUNT.bind(0),
                TrackProperty.COMMENTS_COUNT.bind(10),
                PlayableProperty.CREATED_AT.bind(new Date())
        ));

        expect(view.findViewById(R.id.plays)).toBeGone();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeVisible();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeGone();
    }

    @Test
    public void bindViewsOnlyShowsRepostsWhenPlaysAndLikesAreZero() throws Exception {
        presenter.bind(view, PropertySet.from(
                PlayableProperty.TITLE.bind("Title"),
                PlayableProperty.CREATOR_NAME.bind("Creator"),
                TrackProperty.PLAY_COUNT.bind(0),
                PlayableProperty.LIKES_COUNT.bind(0),
                PlayableProperty.REPOSTS_COUNT.bind(10),
                TrackProperty.COMMENTS_COUNT.bind(10),
                PlayableProperty.CREATED_AT.bind(new Date())
        ));

        expect(view.findViewById(R.id.plays)).toBeGone();
        expect(view.findViewById(R.id.divider1)).toBeGone();
        expect(view.findViewById(R.id.likes)).toBeGone();
        expect(view.findViewById(R.id.divider2)).toBeGone();
        expect(view.findViewById(R.id.reposts)).toBeVisible();
    }
}