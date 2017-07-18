package com.soundcloud.android.activities;

import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;
import android.view.View;

import java.util.Collections;
import java.util.Date;

public class ActivitiesPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private ActivitiesPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private ActivitiesOperations operations;
    @Mock private ActivitiesAdapter adapter;
    @Mock private TrackRepository trackRepository;
    @Mock private NavigationExecutor navigationExecutor;
    @Mock private View itemView;
    @Mock private NewItemsIndicator newItemsIndicator;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;
    @Mock private Navigator navigator;

    @Before
    public void setUp() throws Exception {
        when(itemView.getContext()).thenReturn(context());
        presenter = new ActivitiesPresenter(
                swipeRefreshAttacher,
                operations,
                adapter,
                trackRepository,
                navigationExecutor,
                newItemsIndicator,
                performanceMetricsEngine,
                navigator);

        when(operations.updatedTimelineItemsForStart()).thenReturn(Maybe.empty());
        when(operations.pagingFunction()).thenReturn(TestPager.singlePageFunction());
        when(operations.initialActivities()).thenReturn(Single.just(emptyList()));
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() {
        ActivityItem activityItem = PlayableFixtures.activityTrackLike();
        when(operations.initialActivities()).thenReturn(Single.just(singletonList(activityItem)));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(adapter).onNext(singletonList(activityItem));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingFollower() {
        final ActivityItem activity = PlayableFixtures.activityUserFollow();
        when(adapter.getItem(0)).thenReturn(activity);
        AppCompatActivity viewActivity = activity();
        when(itemView.getContext()).thenReturn(viewActivity);

        presenter.onItemClicked(itemView, 0);

        verify(navigator).navigateTo(eq(viewActivity), argThat(matchesNavigationTarget(NavigationTarget.forProfile(activity.getUrn()))));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingTrackLike() {
        final ActivityItem activity = PlayableFixtures.activityTrackLike();
        when(adapter.getItem(0)).thenReturn(activity);
        AppCompatActivity viewActivity = activity();
        when(itemView.getContext()).thenReturn(viewActivity);

        presenter.onItemClicked(itemView, 0);

        verify(navigator).navigateTo(eq(viewActivity), argThat(matchesNavigationTarget(NavigationTarget.forProfile(activity.getUrn()))));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingTrackRepost() {
        final ActivityItem activity = PlayableFixtures.activityTrackRepost();
        when(adapter.getItem(0)).thenReturn(activity);
        AppCompatActivity viewActivity = activity();
        when(itemView.getContext()).thenReturn(viewActivity);

        presenter.onItemClicked(itemView, 0);

        verify(navigator).navigateTo(eq(viewActivity), argThat(matchesNavigationTarget(NavigationTarget.forProfile(activity.getUrn()))));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingPlaylistLike() {
        final ActivityItem activity = PlayableFixtures.activityPlaylistLike();
        when(adapter.getItem(0)).thenReturn(activity);
        AppCompatActivity viewActivity = activity();
        when(itemView.getContext()).thenReturn(viewActivity);

        presenter.onItemClicked(itemView, 0);

        verify(navigator).navigateTo(eq(viewActivity), argThat(matchesNavigationTarget(NavigationTarget.forProfile(activity.getUrn()))));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingPlaylistRepost() {
        final ActivityItem activity = PlayableFixtures.activityPlaylistRepost();
        when(adapter.getItem(0)).thenReturn(activity);
        AppCompatActivity viewActivity = activity();
        when(itemView.getContext()).thenReturn(viewActivity);

        presenter.onItemClicked(itemView, 0);

        verify(navigator).navigateTo(eq(viewActivity), argThat(matchesNavigationTarget(NavigationTarget.forProfile(activity.getUrn()))));
    }

    @Test
    public void shouldGoToTrackCommentsWhenClickingTrackComment() {
        final Track track = ModelFixtures.trackBuilder().build();
        final ActivityItem activity = PlayableFixtures.activityTrackComment(track.urn());
        when(trackRepository.track(activity.getCommentedTrackUrn().get()))
                .thenReturn(Maybe.just(track));
        when(adapter.getItem(0)).thenReturn(activity);

        presenter.onItemClicked(itemView, 0);

        verify(navigationExecutor).openTrackComments(context(), track.urn());
    }

    @Test
    public void onNewItemsIndicatorClickedUpdatesActivitiesAgain() {
        when(operations.initialActivities())
                .thenReturn(Single.just(Collections.emptyList()));
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onNewItemsIndicatorClicked();

        verify(operations, times(2)).initialActivities();
    }

    @Test
    public void shouldRefreshOnCreate() {
        when(operations.updatedTimelineItemsForStart()).thenReturn(Maybe.just(Collections.emptyList()));
        when(operations.getFirstItemTimestamp(anyListOf(ActivityItem.class))).thenReturn(Optional.of(new Date(123L)));
        when(operations.newItemsSince(123L)).thenReturn(io.reactivex.Observable.just(5));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(newItemsIndicator).update(5);
    }

    @Test
    public void shouldEndMeasuringLoadingPerformance() {
        when(operations.initialActivities())
                .thenReturn(Single.just(Collections.emptyList()));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(performanceMetricsEngine).endMeasuring(MetricType.ACTIVITIES_LOAD);
    }
}
