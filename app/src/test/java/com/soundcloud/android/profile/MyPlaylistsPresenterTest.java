package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.postedPlaylistForPostedPlaylistScreen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PlayableListUpdater;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.android.view.adapters.MixedItemClickListener;
import com.soundcloud.android.view.adapters.MixedPlayableRecyclerItemAdapter;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.os.Bundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyPlaylistsPresenterTest extends AndroidUnitTest {

    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);
    private static final Urn PLAYLIST2_URN = Urn.forPlaylist(456L);

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh,
                                                                    new Bundle());

    @Mock private MixedPlayableRecyclerItemAdapter adapter;
    @Mock private MyProfileOperations operations;
    @Mock private PlayableListUpdater listUpdater;
    @Mock private PlayableListUpdater.Factory factory;
    @Captor private ArgumentCaptor<List<PlayableItem>> captor;

    private TestEventBus eventBus = new TestEventBus();
    private MyPlaylistsPresenter presenter;

    @Before
    public void setUp() {
        fragmentRule.setFragmentArguments(MyPlaylistsFragment.createBundle(Screen.YOU,
                                                                           mock(SearchQuerySourceInfo.class)));
        when(factory.create(adapter, adapter.getTrackRenderer())).thenReturn(listUpdater);

        presenter = new MyPlaylistsPresenter(
                mock(SwipeRefreshAttacher.class),
                mock(ImagePauseOnScrollListener.class),
                adapter,
                mock(MixedItemClickListener.Factory.class),
                factory,
                operations,
                eventBus);
    }

    @Test
    public void refreshesAdapterContentWhenPlaylistDeleted() {
        when(operations.pagedPlaylistItems()).thenReturn(Observable.just(playlists()),
                                                         Observable.just(updatedPlaylists()));

        presenter.onCreate(fragmentRule.getFragment(), null);
        presenter.onViewCreated(fragmentRule.getFragment(), fragmentRule.getView(), null);

        eventBus.publish(EventQueue.ENTITY_STATE_CHANGED,
                         EntityStateChangedEvent.fromEntityDeleted(PLAYLIST_URN));

        final InOrder inOrder = Mockito.inOrder(adapter);
        inOrder.verify(adapter).onNext(captor.capture());
        assertCapturedPlaylistItems(playlistItemsFrom(playlists()));

        inOrder.verify(adapter).clear();

        inOrder.verify(adapter).onNext(captor.capture());
        assertCapturedPlaylistItems(playlistItemsFrom(updatedPlaylists()));
    }

    private void assertCapturedPlaylistItems(List<PlayableItem> playableItems) {
        assertThat(playableItems.size()).isEqualTo(captor.getValue().size());

        for (int i = 0; i < playableItems.size(); i++) {
            assertThat(captor.getValue().get(i).getUrn()).isEqualTo(playableItems.get(i).getUrn());
        }
    }

    private List<PlayableItem> playlistItemsFrom(List<PropertySet> playlists) {
        return Lists.transform(playlists, new Function<PropertySet, PlayableItem>() {
            @Override
            public PlayableItem apply(PropertySet input) {
                return PlayableItem.from(input);
            }
        });
    }

    private List<PropertySet> playlists() {
        return Arrays.asList(
                postedPlaylistForPostedPlaylistScreen(PLAYLIST_URN),
                postedPlaylistForPostedPlaylistScreen(PLAYLIST2_URN));
    }

    private List<PropertySet> updatedPlaylists() {
        return Collections.singletonList(
                postedPlaylistForPostedPlaylistScreen(PLAYLIST2_URN));
    }
}
