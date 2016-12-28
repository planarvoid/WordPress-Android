package com.soundcloud.android.activities;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.TestPager;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.view.NewItemsIndicator;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.view.View;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ActivitiesPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh);

    private ActivitiesPresenter presenter;

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private ActivitiesOperations operations;
    @Mock private ActivitiesAdapter adapter;
    @Mock private TrackRepository trackRepository;
    @Mock private Navigator navigator;
    @Mock private View itemView;
    @Mock private NewItemsIndicator newItemsIndicator;

    @Before
    public void setUp() throws Exception {
        when(itemView.getContext()).thenReturn(context());
        presenter = new ActivitiesPresenter(
                swipeRefreshAttacher,
                operations,
                adapter,
                trackRepository,
                navigator,
                newItemsIndicator);
        when(operations.updatedTimelineItemsForStart()).thenReturn(Observable.<List<ActivityItem>>empty());
        when(operations.pagingFunction()).thenReturn(TestPager.<List<ActivityItem>>singlePageFunction());
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() {
        ActivityItem activityItem = ActivityItem.fromPropertySet(TestPropertySets.activityTrackLike());
        when(operations.initialActivities()).thenReturn(Observable.just(singletonList(activityItem)));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(adapter).onNext(singletonList(activityItem));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingFollower() {
        final PropertySet activity = TestPropertySets.activityUserFollow();
        when(adapter.getItem(0)).thenReturn(ActivityItem.fromPropertySet(activity));

        presenter.onItemClicked(itemView, 0);

        verify(navigator).legacyOpenProfile(context(), activity.get(ActivityProperty.USER_URN));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingTrackLike() {
        final PropertySet activity = TestPropertySets.activityTrackLike();
        when(adapter.getItem(0)).thenReturn(ActivityItem.fromPropertySet(activity));

        presenter.onItemClicked(itemView, 0);

        verify(navigator).legacyOpenProfile(context(), activity.get(ActivityProperty.USER_URN));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingTrackRepost() {
        final PropertySet activity = TestPropertySets.activityTrackRepost();
        when(adapter.getItem(0)).thenReturn(ActivityItem.fromPropertySet(activity));

        presenter.onItemClicked(itemView, 0);

        verify(navigator).legacyOpenProfile(context(), activity.get(ActivityProperty.USER_URN));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingPlaylistLike() {
        final PropertySet activity = TestPropertySets.activityPlaylistLike();
        when(adapter.getItem(0)).thenReturn(ActivityItem.fromPropertySet(activity));

        presenter.onItemClicked(itemView, 0);

        verify(navigator).legacyOpenProfile(context(), activity.get(ActivityProperty.USER_URN));
    }

    @Test
    public void shouldGoToUserProfileWhenClickingPlaylistRepost() {
        final PropertySet activity = TestPropertySets.activityPlaylistRepost();
        when(adapter.getItem(0)).thenReturn(ActivityItem.fromPropertySet(activity));

        presenter.onItemClicked(itemView, 0);

        verify(navigator).legacyOpenProfile(context(), activity.get(ActivityProperty.USER_URN));
    }

    @Test
    public void shouldGoToTrackCommentsWhenClickingTrackComment() {
        final TrackItem track = TestPropertySets.expectedTrackForPlayer();
        final PropertySet activity = TestPropertySets.activityTrackComment();
        when(trackRepository.track(activity.get(ActivityProperty.COMMENTED_TRACK_URN)))
                .thenReturn(Observable.just(track));
        when(adapter.getItem(0)).thenReturn(ActivityItem.fromPropertySet(activity));

        presenter.onItemClicked(itemView, 0);

        verify(navigator).openTrackComments(context(), track.getUrn());
    }

    @Test
    public void onNewItemsIndicatorClickedUpdatesActivitiesAgain() {
        when(operations.initialActivities())
                .thenReturn(Observable.just(Collections.<ActivityItem>emptyList()));
        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        presenter.onNewItemsIndicatorClicked();

        verify(operations, times(2)).initialActivities();
    }

    @Test
    public void shouldRefreshOnCreate() {
        when(operations.updatedTimelineItemsForStart()).thenReturn(Observable.just(Collections.<ActivityItem>emptyList()));
        when(operations.getFirstItemTimestamp(anyListOf(ActivityItem.class))).thenReturn(Optional.of(new Date(123L)));
        when(operations.newItemsSince(123L)).thenReturn(Observable.just(5));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(newItemsIndicator).update(5);
    }


}
