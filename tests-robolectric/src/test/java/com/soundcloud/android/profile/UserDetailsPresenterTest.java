package com.soundcloud.android.profile;

import static android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.res.Resources;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class UserDetailsPresenterTest {

    private static final int EXPANDED_HEIGHT = 15;
    private static final String DESCRIPTION = "desciption";
    private static final String WEBSITE_NAME = "website-name";
    private static final String WEBSITE_URL = "website-url";
    private static final String DISCOGS_NAME = "discogs-name";
    private static final String MYSPACE_NAME = "myspace-name";

    private UserDetailsPresenter presenter;

    @Mock private UserDetailsView userDetailsView;
    @Mock private ProfileActivity activity;
    @Mock private UserDetailsFragment fragment;
    @Mock private View view;
    @Mock private Resources resources;
    @Mock private MultiSwipeRefreshLayout refreshLayout;
    @Mock private ProfileUserProvider profileUserProvider;
    @Captor private ArgumentCaptor<OnRefreshListener> refreshCaptor;

    @Before
    public void setUp() throws Exception {
        presenter = new UserDetailsPresenter(userDetailsView);

        when(view.getResources()).thenReturn(resources);
        when(resources.getDimensionPixelSize(R.dimen.profile_header_expanded_height)).thenReturn(EXPANDED_HEIGHT);
        when(fragment.getActivity()).thenReturn(activity);
        when(activity.profileUserProvider()).thenReturn(profileUserProvider);
        when(profileUserProvider.user()).thenReturn(Observable.<ProfileUser>empty());
    }

    @Test
    public void setHeaderSizeSetsTopPaddingUsingView() throws Exception {
        presenter.setHeaderSize(10);

        verify(userDetailsView).setTopPadding(10);
    }

    @Test
    public void onViewCreatedSetsInitialHeaderSize() throws Exception {
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).setTopPadding(EXPANDED_HEIGHT);
    }

    @Test
    public void onViewCreatedShowsEmptyViewWithWaitingStatus() throws Exception {
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).showEmptyView(EmptyView.Status.WAITING);
    }

    @Test
    public void onViewCreatedShowsEmptyViewWithErrorStatus() throws Exception {
        when(profileUserProvider.user()).thenReturn(Observable.<ProfileUser>error(new Throwable()));

        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void onViewCreatedShowsEmptyViewWithOkStateWithUserWithoutDetails() throws Exception {
        when(profileUserProvider.user()).thenReturn(Observable.just(emptyUser()));

        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).showEmptyView(EmptyView.Status.OK);
    }

    @Test
    public void onViewCreatedHidesEmptyViewWithDetails() throws Exception {
        when(profileUserProvider.user()).thenReturn(Observable.just(userWithDescription()));

        presenter.onViewCreated(fragment, view, null);

        InOrder inOrder = Mockito.inOrder(userDetailsView);
        inOrder.verify(userDetailsView).showEmptyView(EmptyView.Status.WAITING);
        inOrder.verify(userDetailsView, times(2)).hideEmptyView();
        verify(userDetailsView, never()).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void onViewCreatedSetsFullUserDetailsOnView() throws Exception {
        when(profileUserProvider.user()).thenReturn(Observable.just(userWithFullDetails()));

        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).showDescription(DESCRIPTION);
        verify(userDetailsView).showWebsite(WEBSITE_URL, WEBSITE_NAME);
        verify(userDetailsView).showMyspace(MYSPACE_NAME);
        verify(userDetailsView).showDiscogs(DISCOGS_NAME);
    }

    @Test
    public void onViewCreatedHidesUserDetailsOnEmptyUser() throws Exception {
        when(profileUserProvider.user()).thenReturn(Observable.just(userWithBlankDescription()));

        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).hideDescription();
        verify(userDetailsView).hideWebsite();
        verify(userDetailsView).hideMyspace();
        verify(userDetailsView).hideDiscogs();
    }

    @Test
    public void swipeToRefreshShowsEmptyViewWithErrorStatus() throws Exception {
        when(profileUserProvider.refreshUser()).thenReturn(Observable.<ProfileUser>error(new Throwable()));

        swipeToRefresh();

        verify(userDetailsView).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void swipeToRefreshShowsEmptyViewWithOkStateWithUserWithoutDetails() throws Exception {
        when(profileUserProvider.refreshUser()).thenReturn(Observable.just(emptyUser()));

        swipeToRefresh();

        verify(userDetailsView).showEmptyView(EmptyView.Status.OK);
    }

    @Test
    public void swipeToRefreshHidesEmptyViewWithDetails() throws Exception {
        when(profileUserProvider.refreshUser()).thenReturn(Observable.just(userWithDescription()));

        swipeToRefresh();

        verify(userDetailsView, times(2)).hideEmptyView();
        verify(userDetailsView, never()).showEmptyView(EmptyView.Status.ERROR);
    }

    @Test
    public void swipeToRefreshSetsFullUserDetailsOnView() throws Exception {
        when(profileUserProvider.refreshUser()).thenReturn(Observable.just(userWithFullDetails()));

        swipeToRefresh();

        verify(userDetailsView).showDescription(DESCRIPTION);
        verify(userDetailsView).showWebsite(WEBSITE_URL, WEBSITE_NAME);
        verify(userDetailsView).showMyspace(MYSPACE_NAME);
        verify(userDetailsView).showDiscogs(DISCOGS_NAME);
    }

    @Test
    public void swipeToRefreshHidesUserDetailsOnEmptyUser() throws Exception {
        when(profileUserProvider.refreshUser()).thenReturn(Observable.just(userWithBlankDescription()));

        swipeToRefresh();

        verify(userDetailsView).hideDescription();
        verify(userDetailsView).hideWebsite();
        verify(userDetailsView).hideMyspace();
        verify(userDetailsView).hideDiscogs();
    }

    @Test
    public void stopsSwipeToRefreshAfterRefreshComplete() throws Exception {
        when(profileUserProvider.refreshUser()).thenReturn(Observable.just(userWithDescription()));

        swipeToRefresh();

        verify(refreshLayout).setRefreshing(false);
    }

    private void swipeToRefresh() {
        presenter.onCreate(fragment, null);
        presenter.attachRefreshLayout(refreshLayout);
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