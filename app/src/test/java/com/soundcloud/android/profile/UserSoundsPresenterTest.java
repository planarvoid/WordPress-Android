package com.soundcloud.android.profile;

import static com.soundcloud.android.R.layout.default_recyclerview_with_refresh;
import static com.soundcloud.android.profile.UserSoundsItem.fromPlaylistItem;
import static com.soundcloud.android.profile.UserSoundsItem.fromTrackItem;
import static com.soundcloud.android.profile.UserSoundsTypes.TRACKS;
import static com.soundcloud.android.profile.UserSoundsTypes.fromModule;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import java.util.List;

public class UserSoundsPresenterTest extends AndroidUnitTest {

    private static final Urn USER_URN = Urn.forUser(123L);
    private static final SearchQuerySourceInfo SEARCH_QUERY_SOURCE_INFO = new SearchQuerySourceInfo(Urn.forTrack(123L),
                                                                                                    "query");

    @Rule public final FragmentRule fragmentRule = new FragmentRule(default_recyclerview_with_refresh, new Bundle());

    private UserSoundsPresenter presenter;
    private UserProfile userProfileResponse;
    private Bundle fragmentArgs;
    private TestEventBus eventBus = new TestEventBus();
    private ProfileUser profileUser;

    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private SwipeRefreshAttacher swipeRefreshAttacker;
    @Mock private UserSoundsAdapter adapter;
    @Mock private UserProfileOperations operations;
    @Mock private UserSoundsItemMapper userSoundsItemMapper;
    @Mock private UserSoundsItemClickListener.Factory clickListenerFactory;
    @Mock private UserSoundsItemClickListener clickListener;
    @Mock private Resources resources;
    @Mock private UserSoundsItem userSoundsItem;
    @Mock private View view;
    @Mock private RecyclerView recyclerView;
    @Mock private SimpleItemAnimator itemAnimator;

    @Captor private ArgumentCaptor<Observable<List<PlayableItem>>> argumentCaptor;

    @Before
    public void setUp() throws Exception {
        userProfileResponse = new UserProfileFixtures.Builder().build();
        fragmentArgs = new Bundle();
        profileUser = ProfileUser.from(ModelFixtures.create(ApiUser.class));

        fragmentArgs.putParcelable(ProfileArguments.USER_URN_KEY, USER_URN);
        fragmentArgs.putParcelable(ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY, SEARCH_QUERY_SOURCE_INFO);
        fragmentRule.setFragmentArguments(fragmentArgs);

        presenter = new UserSoundsPresenter(imagePauseOnScrollListener, swipeRefreshAttacker, adapter, operations,
                                            userSoundsItemMapper, clickListenerFactory, eventBus, resources);

        doReturn(Observable.just(userProfileResponse)).when(operations).userProfile(USER_URN);
        doReturn(clickListener).when(clickListenerFactory).create(SEARCH_QUERY_SOURCE_INFO);
        when(operations.getLocalProfileUser(USER_URN)).thenReturn(Observable.just(profileUser));
        when(resources.getInteger(R.integer.user_profile_card_grid_span_count)).thenReturn(1);
        when(recyclerView.getItemAnimator()).thenReturn(itemAnimator);
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() throws Exception {
        when(userSoundsItemMapper.call(userProfileResponse)).thenReturn(singletonList(userSoundsItem));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(adapter).onNext(singletonList(userSoundsItem));
    }

    @Test
    public void configuresEmptyViewInOnViewCreatedForOthersProfile() throws Exception {
        EmptyView emptyView = mock(EmptyView.class);

        when(resources.getString(R.string.empty_user_sounds_message_secondary, profileUser.getName()))
                .thenReturn("Secondary Text");

        fragmentArgs.putBoolean(UserSoundsFragment.IS_CURRENT_USER, false);
        fragmentRule.setFragmentArguments(fragmentArgs);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), view(emptyView), null);

        verify(emptyView).setImage(R.drawable.empty_lists_sounds);
        verify(emptyView).setMessageText(R.string.empty_user_sounds_message);
        verify(emptyView).setSecondaryText("Secondary Text");
    }

    @Test
    public void configuresEmptyViewInOnViewCreatedForOwnProfile() throws Exception {
        EmptyView emptyView = mock(EmptyView.class);

        fragmentArgs.putBoolean(UserSoundsFragment.IS_CURRENT_USER, true);
        fragmentRule.setFragmentArguments(fragmentArgs);

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), view(emptyView), null);

        verify(emptyView).setImage(R.drawable.empty_lists_sounds);
        verify(emptyView).setMessageText(R.string.empty_you_sounds_message);
        verify(emptyView).setSecondaryText(R.string.empty_you_sounds_message_secondary);
    }

    @Test
    public void shouldNotifyAdapterWhenPlaylistEntityStateChanged() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        when(adapter.getItems()).thenReturn(Lists.newArrayList(fromPlaylistItem(PlaylistItem.from(playlist), UserSoundsTypes.SPOTLIGHT)));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.LIKE_CHANGED, fakeLikePlaylistEvent(playlist));

        verify(adapter).notifyItemChanged(0);
    }

    @Test
    public void shouldDelegateClickEventsToClickListener() {
        final TrackItem trackItem = TrackItem.from(ModelFixtures.create(ApiTrack.class));
        final UserSoundsItem userSoundsItem = fromTrackItem(trackItem, TRACKS);

        when(adapter.getItem(0)).thenReturn(userSoundsItem);
        when(adapter.getItems()).thenReturn(singletonList(userSoundsItem));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onItemClicked(view, 0);

        verify(clickListener).onItemClick(argumentCaptor.capture(),
                                          same(view),
                                          eq(0),
                                          same(userSoundsItem),
                                          eq(USER_URN),
                                          same(SEARCH_QUERY_SOURCE_INFO),
                                          eq(fromModule(TRACKS, 0)));

        final TestSubscriber<List<PlayableItem>> subscriber = new TestSubscriber<>();
        argumentCaptor.getValue().subscribe(subscriber);
        subscriber.assertValue(singletonList(trackItem));
    }

    @Test
    public void shouldNotDelegateClickEventsToClickListenerForDivider() {
        when(adapter.getItem(0)).thenReturn(UserSoundsItem.fromDivider());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onItemClicked(view, 0);

        verify(clickListener, never()).onItemClick(any(Observable.class),
                                                   eq(view),
                                                   eq(0),
                                                   any(UserSoundsItem.class),
                                                   any(Urn.class),
                                                   any(SearchQuerySourceInfo.class),
                                                   any(Module.class));
    }

    @Test
    public void shouldNotDelegateClickEventsToClickListenerForEndOfListDivider() {
        when(adapter.getItem(0)).thenReturn(UserSoundsItem.fromEndOfListDivider());

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onItemClicked(view, 0);

        verify(clickListener, never()).onItemClick(any(Observable.class),
                                                   eq(view),
                                                   eq(0),
                                                   any(UserSoundsItem.class),
                                                   any(Urn.class),
                                                   any(SearchQuerySourceInfo.class),
                                                   any(Module.class));
    }

    private LikesStatusEvent fakeLikePlaylistEvent(ApiPlaylist apiPlaylist) {
        return LikesStatusEvent.create(apiPlaylist.getUrn(), true, apiPlaylist.getLikesCount() + 1);
    }

    private View view(EmptyView emptyView) {
        View view = mock(View.class);
        when(view.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(view.findViewById(R.id.ak_recycler_view)).thenReturn(recyclerView);
        when(view.getResources()).thenReturn(resources);
        return view;
    }
}
