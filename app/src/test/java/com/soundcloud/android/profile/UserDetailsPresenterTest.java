package com.soundcloud.android.profile;

import static android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import static com.soundcloud.java.optional.Optional.of;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.User;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import java.util.Locale;

public class UserDetailsPresenterTest extends AndroidUnitTest {

    private static final Urn USER_URN = Urn.forUser(123L);
    private static final String BIO = "bio";
    private static final String WEBSITE_NAME = "website-name";
    private static final String WEBSITE_URL = "website-url";
    private static final String DISCOGS_NAME = "discogs-name";
    private static final String MYSPACE_NAME = "myspace-name";
    private static final int FOLLOWERS_COUNT = 3;
    private static final int FOLLOWINGS_COUNT = 6;

    private UserDetailsPresenter presenter;

    @Mock private UserProfileOperations profileOperations;
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
    private CondensedNumberFormatter numberFormatter;

    @Before
    public void setUp() throws Exception {
        final Bundle value = new Bundle();
        value.putParcelable(ProfileArguments.USER_URN_KEY, USER_URN);
        when(fragment.getArguments()).thenReturn(value);
        when(view.getResources()).thenReturn(resources);
        when(view.findViewById(R.id.user_details_holder)).thenReturn(userDetailsHolder);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.findViewById(R.id.str_layout)).thenReturn(refreshLayout);
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.empty());

        when(resources.getStringArray(R.array.ak_number_suffixes)).thenReturn(new String[]{"", "K", "M", "B"});
        numberFormatter = CondensedNumberFormatter.create(Locale.GERMAN, resources);
        presenter = new UserDetailsPresenter(profileOperations, userDetailsView, numberFormatter);
    }

    @Test
    public void onViewCreatedSetsViewsOnUserDetailsView() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).setView(view);
    }

    @Test
    public void onViewCreatedSetsFullUserDetailsOnView() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithFullDetails()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).setFollowersCount(numberFormatter.format(FOLLOWERS_COUNT));
        verify(userDetailsView).setFollowingsCount(numberFormatter.format(FOLLOWINGS_COUNT));
        verify(userDetailsView).showBio(BIO);
        verify(userDetailsView).showLinksSection();
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

        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.never());

        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView, times(2)).setFollowersCount(numberFormatter.format(FOLLOWERS_COUNT));
        verify(userDetailsView, times(2)).setFollowingsCount(numberFormatter.format(FOLLOWINGS_COUNT));
        verify(userDetailsView, times(2)).showBio(BIO);
        verify(userDetailsView, times(2)).showLinksSection();
        verify(userDetailsView, times(2)).showWebsite(WEBSITE_URL, WEBSITE_NAME);
        verify(userDetailsView, times(2)).showMyspace(MYSPACE_NAME);
        verify(userDetailsView, times(2)).showDiscogs(DISCOGS_NAME);
    }

    @Test
    public void onViewCreatedHidesUserDetailsOnEmptyUser() throws Exception {
        when(profileOperations.getLocalAndSyncedProfileUser(USER_URN)).thenReturn(Observable.just(
                userWithBlankDescription()));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).hideBio();
        verify(userDetailsView).hideLinksSection();
        verify(userDetailsView).hideWebsite();
        verify(userDetailsView).hideMyspace();
        verify(userDetailsView).hideDiscogs();
    }

    @Test
    public void swipeToRefreshSetsFullUserDetailsOnView() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.getSyncedProfileUser(USER_URN)).thenReturn(Observable.just(userWithFullDetails()));

        swipeToRefresh();

        verify(userDetailsView).setFollowersCount(numberFormatter.format(FOLLOWERS_COUNT));
        verify(userDetailsView).setFollowingsCount(numberFormatter.format(FOLLOWINGS_COUNT));
        verify(userDetailsView).showBio(BIO);
        verify(userDetailsView).showLinksSection();
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

        verify(userDetailsView).hideBio();
        verify(userDetailsView).hideLinksSection();
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

    private User userWithBlankDescription() {
        return ModelFixtures.userBuilder(false).description(of("")).build();
    }

    private User userWithDescription() {
        return ModelFixtures.userBuilder(false).description(of(BIO)).build();
    }

    private User userWithFullDetails() {
        return ModelFixtures.userBuilder(false)
                            .description(of(BIO))
                            .websiteName(of(WEBSITE_NAME))
                            .websiteUrl(of(WEBSITE_URL))
                            .discogsName(of(DISCOGS_NAME))
                            .mySpaceName(of(MYSPACE_NAME))
                            .followersCount(FOLLOWERS_COUNT)
                            .followingsCount(FOLLOWINGS_COUNT)
                            .build();
    }
}
