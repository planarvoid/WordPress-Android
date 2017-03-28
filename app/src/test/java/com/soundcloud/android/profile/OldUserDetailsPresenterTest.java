package com.soundcloud.android.profile;

import static android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import static com.soundcloud.java.optional.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
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

public class OldUserDetailsPresenterTest extends AndroidUnitTest {

    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String DESCRIPTION = "description";
    private static final Optional<String> WEBSITE_NAME = Optional.of("website-name");
    private static final String WEBSITE_URL = "website-url";
    private static final String DISCOGS_NAME = "discogs-name";
    private static final String MYSPACE_NAME = "myspace-name";

    private OldUserDetailsPresenter presenter;

    @Mock private UserProfileOperations profileOperations;
    @Mock private OldUserDetailsView oldUserDetailsView;
    @Mock private ProfileActivity activity;
    @Mock private OldUserDetailsFragment fragment;
    @Mock private View view;
    @Mock private Resources resources;
    @Mock private MultiSwipeRefreshLayout refreshLayout;
    @Mock private View userDetailsHolder;
    @Mock private EmptyView emptyView;
    @Mock private Intent intent;
    @Captor private ArgumentCaptor<OnRefreshListener> refreshCaptor;

    @Before
    public void setUp() throws Exception {
        presenter = new OldUserDetailsPresenter(profileOperations, oldUserDetailsView);
        final Bundle value = new Bundle();
        value.putParcelable(ProfileArguments.USER_URN_KEY, USER_URN);
        when(fragment.getArguments()).thenReturn(value);
        when(view.getResources()).thenReturn(resources);
        when(view.findViewById(R.id.user_details_holder)).thenReturn(userDetailsHolder);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.findViewById(R.id.str_layout)).thenReturn(refreshLayout);
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.empty());
    }

    @Test
    public void onViewCreatedSetsViewsOnUserDetailsView() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(oldUserDetailsView).setView(view);
    }

    @Test
    public void onViewCreatedShowsEmptyViewAfterSetView() throws Exception {
        presenter.onCreate(fragment, null);
        InOrder inOrder = Mockito.inOrder(oldUserDetailsView);

        presenter.onViewCreated(fragment, view, null);

        inOrder.verify(oldUserDetailsView).setView(view);
        inOrder.verify(oldUserDetailsView, times(2)).showEmptyView(any(EmptyView.Status.class));
    }

    @Test
    public void onViewCreatedShowsEmptyViewWithWaitingStatus() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(oldUserDetailsView).showEmptyView(EmptyView.Status.WAITING);
    }

    @Test
    public void onViewCreatedShowsEmptyViewWithErrorStatus() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.error(new Throwable()));

        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(oldUserDetailsView).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void onViewCreatedShowsEmptyViewWithOkStateWithUserWithoutDetails() throws Exception {
        presenter.onCreate(fragment, null);
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(emptyUser()));

        presenter.onViewCreated(fragment, view, null);

        verify(oldUserDetailsView).showEmptyView(EmptyView.Status.OK);
    }

    @Test
    public void onViewCreatedHidesEmptyViewWithDetails() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithDescription()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        InOrder inOrder = Mockito.inOrder(oldUserDetailsView);
        inOrder.verify(oldUserDetailsView).showEmptyView(EmptyView.Status.WAITING);
        inOrder.verify(oldUserDetailsView, times(2)).hideEmptyView();
        verify(oldUserDetailsView, never()).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void onViewCreatedSetsFullUserDetailsOnView() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithFullDetails()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(oldUserDetailsView).showDescription(DESCRIPTION);
        verify(oldUserDetailsView).showWebsite(WEBSITE_URL, WEBSITE_NAME);
        verify(oldUserDetailsView).showMyspace(MYSPACE_NAME);
        verify(oldUserDetailsView).showDiscogs(DISCOGS_NAME);
    }

    @Test
    public void onViewCreatedSetsFullUserDetailsOnViewAfterOrientationChange() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithFullDetails()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);
        Mockito.reset(oldUserDetailsView);

        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.never());

        presenter.onViewCreated(fragment, view, null);

        verify(oldUserDetailsView, times(2)).showDescription(DESCRIPTION);
        verify(oldUserDetailsView, times(2)).showWebsite(WEBSITE_URL, WEBSITE_NAME);
        verify(oldUserDetailsView, times(2)).showMyspace(MYSPACE_NAME);
        verify(oldUserDetailsView, times(2)).showDiscogs(DISCOGS_NAME);
    }

    @Test
    public void onViewCreatedHidesUserDetailsOnEmptyUser() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(
                userWithBlankDescription()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(oldUserDetailsView).hideDescription();
        verify(oldUserDetailsView).hideWebsite();
        verify(oldUserDetailsView).hideMyspace();
        verify(oldUserDetailsView).hideDiscogs();
    }

    @Test
    public void swipeToRefreshShowsEmptyViewWithErrorStatus() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.error(new Throwable()));

        swipeToRefresh();

        verify(oldUserDetailsView).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void swipeToRefreshShowsEmptyViewWithOkStateWithUserWithoutDetails() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        Mockito.reset(oldUserDetailsView);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(emptyUser()));

        swipeToRefresh();

        verify(oldUserDetailsView, times(2)).showEmptyView(EmptyView.Status.OK);
    }

    @Test
    public void swipeToRefreshHidesEmptyViewWithDetails() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithDescription()));

        swipeToRefresh();

        verify(oldUserDetailsView, times(2)).hideEmptyView();
        verify(oldUserDetailsView, never()).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void swipeToRefreshSetsFullUserDetailsOnView() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithFullDetails()));

        swipeToRefresh();

        verify(oldUserDetailsView).showDescription(DESCRIPTION);
        verify(oldUserDetailsView).showWebsite(WEBSITE_URL, WEBSITE_NAME);
        verify(oldUserDetailsView).showMyspace(MYSPACE_NAME);
        verify(oldUserDetailsView).showDiscogs(DISCOGS_NAME);
    }

    @Test
    public void swipeToRefreshHidesUserDetailsOnEmptyUser() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithBlankDescription()));

        swipeToRefresh();

        verify(oldUserDetailsView).hideDescription();
        verify(oldUserDetailsView).hideWebsite();
        verify(oldUserDetailsView).hideMyspace();
        verify(oldUserDetailsView).hideDiscogs();
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

        verify(oldUserDetailsView).clearViews();
    }

    private void swipeToRefresh() {
        verify(refreshLayout).setOnRefreshListener(refreshCaptor.capture());
        refreshCaptor.getValue().onRefresh();
    }

    private User emptyUser() {
        return ModelFixtures.user();
    }

    private User userWithBlankDescription() {
        return ModelFixtures.userBuilder(false).description(of("")).build();
    }

    private User userWithDescription() {
        return ModelFixtures.userBuilder(false).description(of(DESCRIPTION)).build();
    }

    private User userWithFullDetails() {
        return ModelFixtures.userBuilder(false)
                            .description(of(DESCRIPTION))
                            .websiteName(WEBSITE_NAME)
                            .websiteUrl(of(WEBSITE_URL))
                            .discogsName(of(DISCOGS_NAME))
                            .mySpaceName(of(MYSPACE_NAME))
                            .build();
    }
}