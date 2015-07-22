package com.soundcloud.android.profile;

import static android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class UserDetailsPresenterTest {
    private static final Urn USER_URN = Urn.forUser(123L);
    private static final int EXPANDED_HEIGHT = 15;
    private static final String DESCRIPTION = "desciption";
    private static final String WEBSITE_NAME = "website-name";
    private static final String WEBSITE_URL = "website-url";
    private static final String DISCOGS_NAME = "discogs-name";
    private static final String MYSPACE_NAME = "myspace-name";

    private UserDetailsPresenter presenter;

    @Mock private ProfileOperations profileOperations;
    @Mock private UserDetailsView userDetailsView;
    @Mock private ProfileActivity activity;
    @Mock private UserDetailsFragment fragment;
    @Mock private View view;
    @Mock private Resources resources;
    @Mock private MultiSwipeRefreshLayout refreshLayout;
    @Mock private View userDetailsHolder;
    @Mock private EmptyView emptyView;
    @Mock private Intent intent;
    @Captor private ArgumentCaptor<OnRefreshListener> refreshCaptor;

    @Before
    public void setUp() throws Exception {
        presenter = new UserDetailsPresenter(profileOperations, userDetailsView);
        final Bundle value = new Bundle();
        value.putParcelable(ProfileArguments.USER_URN_KEY, USER_URN);
        when(fragment.getArguments()).thenReturn(value);
        when(view.getResources()).thenReturn(resources);
        when(view.findViewById(R.id.user_details_holder)).thenReturn(userDetailsHolder);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.findViewById(R.id.str_layout)).thenReturn(refreshLayout);
        when(resources.getDimensionPixelSize(R.dimen.profile_header_expanded_height)).thenReturn(EXPANDED_HEIGHT);
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.<ProfileUser>empty());
    }

    @Test
    public void onViewCreatedSetsViewsOnUserDetailsView() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).setView(view);
    }

    @Test
    public void onViewCreatedShowsEmptyViewAfterSetView() throws Exception {
        presenter.onCreate(fragment, null);
        InOrder inOrder = Mockito.inOrder(userDetailsView);

        presenter.onViewCreated(fragment, view, null);

        inOrder.verify(userDetailsView).setView(view);
        inOrder.verify(userDetailsView, times(2)).showEmptyView(any(EmptyView.Status.class));
    }

    @Test
    public void onViewCreatedShowsEmptyViewWithWaitingStatus() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).showEmptyView(EmptyView.Status.WAITING);
    }

    @Test
    public void onViewCreatedShowsEmptyViewWithErrorStatus() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.<ProfileUser>error(new Throwable()));

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void onViewCreatedShowsEmptyViewWithOkStateWithUserWithoutDetails() throws Exception {
        presenter.onCreate(fragment, null);
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(emptyUser()));

        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).showEmptyView(EmptyView.Status.OK);
    }

    @Test
    public void onViewCreatedHidesEmptyViewWithDetails() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithDescription()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        InOrder inOrder = Mockito.inOrder(userDetailsView);
        inOrder.verify(userDetailsView).showEmptyView(EmptyView.Status.WAITING);
        inOrder.verify(userDetailsView, times(2)).hideEmptyView();
        verify(userDetailsView, never()).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void onViewCreatedSetsFullUserDetailsOnView() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithFullDetails()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).showDescription(DESCRIPTION);
        verify(userDetailsView).showWebsite(WEBSITE_URL, WEBSITE_NAME);
        verify(userDetailsView).showMyspace(MYSPACE_NAME);
        verify(userDetailsView).showDiscogs(DISCOGS_NAME);
    }

    @Test
    public void onViewCreatedSetsFullUserDetailsOnViewAfterOrientationChange() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithFullDetails()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);
        Mockito.reset(userDetailsView);

        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.<ProfileUser>never());

        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView, times(2)).showDescription(DESCRIPTION);
        verify(userDetailsView, times(2)).showWebsite(WEBSITE_URL, WEBSITE_NAME);
        verify(userDetailsView, times(2)).showMyspace(MYSPACE_NAME);
        verify(userDetailsView, times(2)).showDiscogs(DISCOGS_NAME);
    }

    @Test
    public void onViewCreatedHidesUserDetailsOnEmptyUser() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithBlankDescription()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).hideDescription();
        verify(userDetailsView).hideWebsite();
        verify(userDetailsView).hideMyspace();
        verify(userDetailsView).hideDiscogs();
    }

    @Test
    public void swipeToRefreshShowsEmptyViewWithErrorStatus() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.<ProfileUser>error(new Throwable()));

        swipeToRefresh();

        verify(userDetailsView).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void swipeToRefreshShowsEmptyViewWithOkStateWithUserWithoutDetails() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        Mockito.reset(userDetailsView);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(emptyUser()));

        swipeToRefresh();

        verify(userDetailsView).showEmptyView(EmptyView.Status.OK);
    }

    @Test
    public void swipeToRefreshHidesEmptyViewWithDetails() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithDescription()));

        swipeToRefresh();

        verify(userDetailsView, times(2)).hideEmptyView();
        verify(userDetailsView, never()).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void swipeToRefreshSetsFullUserDetailsOnView() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithFullDetails()));

        swipeToRefresh();

        verify(userDetailsView).showDescription(DESCRIPTION);
        verify(userDetailsView).showWebsite(WEBSITE_URL, WEBSITE_NAME);
        verify(userDetailsView).showMyspace(MYSPACE_NAME);
        verify(userDetailsView).showDiscogs(DISCOGS_NAME);
    }

    @Test
    public void swipeToRefreshHidesUserDetailsOnEmptyUser() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithBlankDescription()));

        swipeToRefresh();

        verify(userDetailsView).hideDescription();
        verify(userDetailsView).hideWebsite();
        verify(userDetailsView).hideMyspace();
        verify(userDetailsView).hideDiscogs();
    }

    @Test
    public void stopsSwipeToRefreshAfterRefreshComplete() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithDescription()));

        swipeToRefresh();

        verify(refreshLayout, times(2)).setRefreshing(false);
    }

    @Test
    public void onDestroyViewClearsViewsOnUserDetailsView() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithFullDetails()));

        presenter.onDestroyView(fragment);

        verify(userDetailsView).clearViews();
    }

    private void swipeToRefresh() {
        verify(refreshLayout).setOnRefreshListener(refreshCaptor.capture());
        refreshCaptor.getValue().onRefresh();
    }

    private ProfileUser emptyUser() {
        return new ProfileUser(PropertySet.create());
    }

    private ProfileUser userWithBlankDescription() {
        return new ProfileUser(PropertySet.from(UserProperty.DESCRIPTION.bind("")));
    }

    private ProfileUser userWithDescription() {
        return new ProfileUser(PropertySet.from(UserProperty.DESCRIPTION.bind(DESCRIPTION)));
    }

    private ProfileUser userWithFullDetails() {
        return new ProfileUser(
                PropertySet.from(
                        UserProperty.DESCRIPTION.bind(DESCRIPTION),
                        UserProperty.WEBSITE_NAME.bind(WEBSITE_NAME),
                        UserProperty.WEBSITE_URL.bind(WEBSITE_URL),
                        UserProperty.DISCOGS_NAME.bind(DISCOGS_NAME),
                        UserProperty.MYSPACE_NAME.bind(MYSPACE_NAME)
                )
        );
    }
}