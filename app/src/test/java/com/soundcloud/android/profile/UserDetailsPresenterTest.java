package com.soundcloud.android.profile;

import static android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import static com.google.common.collect.Lists.newArrayList;
import static com.soundcloud.java.optional.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.users.SocialMediaLinkItem;
import com.soundcloud.android.users.User;
import com.soundcloud.android.users.UserProfileInfo;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.java.optional.Optional;
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

import java.io.IOException;
import java.util.List;
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
    @Mock private Navigator navigator;
    @Mock private SocialMediaLinkItem socialMediaLink;
    @Mock private SearchQuerySourceInfo searchQuerySourceInfo;
    @Captor private ArgumentCaptor<OnRefreshListener> refreshCaptor;
    @Captor private ArgumentCaptor<UserDetailsView.UserDetailsListener> listenerCaptor;
    @Captor private ArgumentCaptor<Intent> intentCaptor;
    private CondensedNumberFormatter numberFormatter;
    private UserProfileInfo fullUserProfileInfo;
    private UserProfileInfo emptyUserProfileInfo;
    private List<SocialMediaLinkItem> links;

    @Before
    public void setUp() throws Exception {
        final Bundle value = new Bundle();
        value.putParcelable(ProfileArguments.USER_URN_KEY, USER_URN);
        value.putParcelable(ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);
        links = newArrayList(
                SocialMediaLinkItem.create(Optional.of("firstTitle"), "firstNetwork", "http://www.first.com"),
                SocialMediaLinkItem.create(Optional.of("secondTitle"), "secondNetwork", "http://www.second.com")
        );
        fullUserProfileInfo = UserProfileInfo.create(new ModelCollection<>(links), of(BIO), fullUser());
        emptyUserProfileInfo = UserProfileInfo.create(new ModelCollection<>(newArrayList()), Optional.absent(), emptyUser());
        when(fragment.getArguments()).thenReturn(value);
        when(view.getResources()).thenReturn(resources);
        when(view.findViewById(R.id.user_details_holder)).thenReturn(userDetailsHolder);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.findViewById(R.id.str_layout)).thenReturn(refreshLayout);
        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.empty());

        when(resources.getStringArray(R.array.ak_number_suffixes)).thenReturn(new String[]{"", "K", "M", "B"});
        numberFormatter = CondensedNumberFormatter.create(Locale.GERMAN, resources);
        presenter = new UserDetailsPresenter(profileOperations, userDetailsView, numberFormatter, navigator);
    }

    @Test
    public void onViewCreatedShowsWaitingEmptyView() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).showEmptyView(EmptyView.Status.WAITING);
    }

    @Test
    public void onViewCreatedSetsViewsOnUserDetailsView() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).setView(view);
    }

    @Test
    public void onViewCreatedSetsFullUserDetailsOnView() throws Exception {
        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.just(fullUserProfileInfo));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).hideEmptyView();
        verify(userDetailsView).setFollowersCount(numberFormatter.format(FOLLOWERS_COUNT));
        verify(userDetailsView).setFollowingsCount(numberFormatter.format(FOLLOWINGS_COUNT));
        verify(userDetailsView).showBio(BIO);
        verify(userDetailsView).showLinks(links);
    }

    @Test
    public void onViewCreatedSetsFullUserDetailsOnViewAfterOrientationChange() throws Exception {
        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.just(fullUserProfileInfo));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        presenter.onDestroyView(fragment);
        Mockito.reset(userDetailsView);

        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.never());

        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).hideEmptyView();
        verify(userDetailsView, times(2)).setFollowersCount(numberFormatter.format(FOLLOWERS_COUNT));
        verify(userDetailsView, times(2)).setFollowingsCount(numberFormatter.format(FOLLOWINGS_COUNT));
        verify(userDetailsView, times(2)).showBio(BIO);
        verify(userDetailsView, times(2)).showLinks(links);
    }

    @Test
    public void onViewCreatedHidesUserDetailsOnEmptyUser() throws Exception {
        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.just(emptyUserProfileInfo));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).hideEmptyView();
        verify(userDetailsView).hideBio();
        verify(userDetailsView).hideLinks();
    }

    @Test
    public void swipeToRefreshSetsFullUserDetailsOnView() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.just(fullUserProfileInfo));

        swipeToRefresh();

        verify(userDetailsView).hideEmptyView();
        verify(userDetailsView).setFollowersCount(numberFormatter.format(FOLLOWERS_COUNT));
        verify(userDetailsView).setFollowingsCount(numberFormatter.format(FOLLOWINGS_COUNT));
        verify(userDetailsView).showBio(BIO);
        verify(userDetailsView).showLinks(links);
    }

    @Test
    public void swipeToRefreshHidesUserDetailsOnEmptyUser() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.just(emptyUserProfileInfo));

        swipeToRefresh();

        verify(userDetailsView).hideEmptyView();
        verify(userDetailsView).hideBio();
        verify(userDetailsView).hideLinks();
        verify(userDetailsView).hideLinks();
    }

    @Test
    public void hidesLoadersAfterRefreshComplete() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);
        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.just(fullUserProfileInfo));

        swipeToRefresh();

        verify(refreshLayout).setRefreshing(false);
        verify(userDetailsView).hideEmptyView();
    }

    @Test
    public void onDestroyViewClearsViewsOnUserDetailsView() throws Exception {
        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.just(fullUserProfileInfo));

        presenter.onDestroyView(fragment);

        verify(userDetailsView).clearViews();
    }

    @Test
    public void clickingLinkStartsActivityForIntent() {
        Intent socialMediaLinkIntent = new Intent();
        when(socialMediaLink.toIntent()).thenReturn(socialMediaLinkIntent);

        presenter.onCreate(fragment, null);
        doNothing().when(userDetailsView).setListener(listenerCaptor.capture());
        doNothing().when(fragment).startActivity(intentCaptor.capture());
        presenter.onViewCreated(fragment, view, null);

        listenerCaptor.getValue().onLinkClicked(socialMediaLink);

        assertThat(intentCaptor.getValue()).isEqualTo(socialMediaLinkIntent);
    }

    @Test
    public void clickingViewFollowersNavigatesToFollowers() {
        presenter.onCreate(fragment, null);
        doNothing().when(userDetailsView).setListener(listenerCaptor.capture());
        presenter.onViewCreated(fragment, view, null);

        listenerCaptor.getValue().onViewFollowersClicked();
        verify(navigator).openFollowers(view.getContext(), USER_URN, searchQuerySourceInfo);
    }

    @Test
    public void clickingViewFollowingNavigatesToFollowings() {
        presenter.onCreate(fragment, null);
        doNothing().when(userDetailsView).setListener(listenerCaptor.capture());
        presenter.onViewCreated(fragment, view, null);

        listenerCaptor.getValue().onViewFollowingClicked();
        verify(navigator).openFollowings(view.getContext(), USER_URN, searchQuerySourceInfo);
    }

    @Test
    public void errorFetchingUserProfileInfoContinuesToShowOldData() {
        when(profileOperations.userProfileInfo(USER_URN)).thenReturn(Observable.error(new IOException("expected")));
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, view, null);

        verify(userDetailsView).hideEmptyView();
        verify(userDetailsView, never()).setFollowersCount(any());
        verify(userDetailsView, never()).setFollowersCount(any());
        verify(userDetailsView, never()).showBio(any());
        verify(userDetailsView, never()).showLinks(any());
        verify(userDetailsView, never()).hideBio();
        verify(userDetailsView, never()).hideLinks();
    }

    private void swipeToRefresh() {
        verify(refreshLayout).setOnRefreshListener(refreshCaptor.capture());
        refreshCaptor.getValue().onRefresh();
    }

    private User emptyUser() {
        return ModelFixtures.userBuilder(false).build();
    }

    private User fullUser() {
        return ModelFixtures.userBuilder(false)
                            .websiteName(of(WEBSITE_NAME))
                            .websiteUrl(of(WEBSITE_URL))
                            .discogsName(of(DISCOGS_NAME))
                            .mySpaceName(of(MYSPACE_NAME))
                            .followersCount(FOLLOWERS_COUNT)
                            .followingsCount(FOLLOWINGS_COUNT)
                            .build();
    }
}
