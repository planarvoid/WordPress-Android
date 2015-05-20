package com.soundcloud.android.profile;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.PullToRefreshWrapper;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.adapters.MixedPlayableAdapter;
import com.soundcloud.android.view.adapters.MixedPlayableItemClickListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ListView;

import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
// TODO : Extend New List Presenter test
public class UserPostsPresenterTest {

    private UserPostsPresenter presenter;

    @Mock private PlaybackOperations playbackOperations;
    @Mock private ImageOperations imageOperations;
    @Mock private PullToRefreshWrapper pullToRefreshWrapper;
    @Mock private ProfileOperations profileOperations;
    @Mock private MixedPlayableAdapter adapter;
    @Mock private MixedPlayableItemClickListener.Factory mixedClickListenerFactory;
    @Mock private MixedPlayableItemClickListener mixedPlayableItemClickListener;
    @Mock private ExpandPlayerSubscriber expandPlayerSubscriber;
    @Mock private Fragment fragment;
    @Mock private View fragmentView;
    @Mock private ListView listView;
    @Mock private EmptyView emptyView;
    @Mock private TrackItemPresenter trackPresenter;
    @Mock private PlayableListUpdater.Factory playableListUpdaterFactory;
    @Mock private PlayableListUpdater playableListUpdater;

    private final Bundle arguments = new Bundle();
    private final Screen screen = Screen.USER_POSTS;
    private final SearchQuerySourceInfo searchQuerySourceInfo = new SearchQuerySourceInfo(Urn.forTrack(123L));
    private final Urn user = Urn.forUser(123L);

    @Before
    public void setUp() throws Exception {
        when(fragmentView.findViewById(android.R.id.list)).thenReturn(listView);
        when(fragmentView.findViewById(android.R.id.empty)).thenReturn(emptyView);
        when(fragment.getArguments()).thenReturn(arguments);
        when(mixedClickListenerFactory.create(screen, searchQuerySourceInfo)).thenReturn(mixedPlayableItemClickListener);
        when(profileOperations.pagedPostItems(user)).thenReturn(Observable.just(new PagedRemoteCollection(Collections.<PropertySetSource>emptyList(), "next-href")));
        when(adapter.getTrackPresenter()).thenReturn(trackPresenter);
        when(playableListUpdaterFactory.create(adapter, trackPresenter)).thenReturn(playableListUpdater);

        arguments.putParcelable(UserPostsFragment.USER_URN_KEY, user);
        arguments.putSerializable(UserPostsFragment.SCREEN_KEY, screen);
        arguments.putParcelable(UserPostsFragment.SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);
        presenter = new UserPostsPresenter(imageOperations, pullToRefreshWrapper, profileOperations, adapter, mixedClickListenerFactory, playableListUpdaterFactory);
    }

    @Test
    public void presenterSetsMixedPlayableClickListener() throws Exception {
        presenter.onCreate(fragment, null);
        presenter.onViewCreated(fragment, fragmentView, null);

        verify(listView).setOnItemClickListener(mixedPlayableItemClickListener);
    }

}