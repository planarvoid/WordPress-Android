package com.soundcloud.android.sync.posts;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent.RepostStatus;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@RunWith(MockitoJUnitRunner.class)
public class PostsSyncerTest {

    private PostsSyncer syncer;

    @Mock private LoadLocalPostsCommand loadPostedPlaylistUrns;
    @Mock private FetchPostsCommand fetcyMyPlaylists;
    @Mock private StorePostsCommand storePlaylistPosts;
    @Mock private RemovePostsCommand removePlaylistPosts;
    @Mock private BulkFetchCommand<ApiPlaylist, ApiPlaylist> fetchPostResources;
    @Mock private StorePlaylistsCommand storePostResources;
    private TestEventBus eventBus = new TestEventBus();

    private PostRecord post1;
    private PostRecord post2;

    @Before
    public void setUp() throws Exception {
        syncer = new PostsSyncer<>(loadPostedPlaylistUrns,
                                   fetcyMyPlaylists,
                                   storePlaylistPosts,
                                   removePlaylistPosts,
                                   fetchPostResources,
                                   storePostResources,
                                   eventBus);

        post1 = ApiRepost.create(Urn.forPlaylist(123L), new Date(100L));
        post2 = ApiPost.create(Urn.forPlaylist(456L), new Date(200L));
    }

    @Test
    public void doesNothingIfLocalAndRemoteStateTheSame() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post1);

        assertThat(syncer.call()).isFalse();

        verifyZeroInteractions(removePlaylistPosts);
        verifyZeroInteractions(storePlaylistPosts);
    }

    @Test
    public void storesNewPlaylistPost() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post1, post2);

        assertThat(syncer.call()).isTrue();

        verify(storePlaylistPosts).call(singleton(post2));
        verifyZeroInteractions(removePlaylistPosts);
    }

    @Test
    public void removesInvalidPost() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts();

        assertThat(syncer.call()).isTrue();

        verify(storePlaylistPosts, never()).call(any(Collection.class));
        verify(removePlaylistPosts).call(singleton(post1));
    }

    @Test
    public void storesNewPostAndRemovesInvalidPost() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post2);

        assertThat(syncer.call()).isTrue();

        verify(storePlaylistPosts).call(singleton(post2));
        verify(removePlaylistPosts).call(singleton(post1));
    }

    @Test
    public void sendsEntityChangedEventsForRepostAdditionAndRemoval() throws Exception {
        final Urn localPostUrn = Urn.forPlaylist(123L);
        PostRecord post1 = ApiRepost.create(localPostUrn, new Date(100L));
        final Urn remotePostUrn = Urn.forPlaylist(456L);
        PostRecord post2 = ApiRepost.create(remotePostUrn, new Date(200L));

        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post2);

        assertThat(syncer.call()).isTrue();

        final RepostsStatusEvent postedEvent = RepostsStatusEvent.create(createRepostedEntityChangedProperty(true, remotePostUrn));
        final RepostsStatusEvent unpostedEvent = RepostsStatusEvent.create(createRepostedEntityChangedProperty(false, localPostUrn));

        assertThat(eventBus.eventsOn(EventQueue.REPOST_CHANGED)).containsExactly(postedEvent, unpostedEvent);
    }

    @Test
    public void sendsEntityChangedEventsForPostAddition() throws Exception {
        final PostRecord newPost = ApiPost.create(Urn.forPlaylist(7L), new Date(100L));
        final Urn urn = newPost.getTargetUrn();

        withLocalPlaylistPosts();
        withRemotePlaylistPosts(newPost);

        assertThat(syncer.call()).isTrue();
        assertThat(eventBus.eventsOn(EventQueue.URN_STATE_CHANGED).iterator().next().urns().iterator().next())
                .isEqualTo(urn);
    }

    @Test
    public void sendsEntityChangedEventsForPostRemoval() throws Exception {
        final PostRecord postToDelete = ApiPost.create(Urn.forPlaylist(7L), new Date(100L));
        final Urn urn = postToDelete.getTargetUrn();

        withLocalPlaylistPosts(postToDelete);
        withRemotePlaylistPosts();

        assertThat(syncer.call()).isTrue();
        assertThat(eventBus.eventsOn(EventQueue.URN_STATE_CHANGED).iterator().next().urns().iterator().next())
                .isEqualTo(urn);
    }

    private Map<Urn, RepostStatus> createRepostedEntityChangedProperty(boolean reposted, Urn... urns) {
        final Map<Urn, RepostStatus> reposts = new HashMap<>(urns.length);
        for (Urn urn : urns) {
            reposts.put(urn, reposted ? RepostStatus.createReposted(urn) : RepostStatus.createUnposted(urn));
        }
        return reposts;
    }

    @Test
    public void fetchesAndStoresEntitiesForNewAdditions() throws Exception {
        withLocalPlaylistPosts(post1);
        withRemotePlaylistPosts(post1, post2);

        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);
        when(fetchPostResources.call()).thenReturn(playlists);

        assertThat(syncer.call()).isTrue();

        verify(storePostResources).call(playlists);

    }

    private void withLocalPlaylistPosts(PostRecord... playlistPosts) throws Exception {
        when(loadPostedPlaylistUrns.call()).thenReturn(Arrays.asList(playlistPosts));
    }

    private void withRemotePlaylistPosts(PostRecord... playlistPosts) throws Exception {
        final TreeSet<PostRecord> propertySets = new TreeSet<>(PostProperty.COMPARATOR);
        propertySets.addAll(Arrays.asList(playlistPosts));
        when(fetcyMyPlaylists.call()).thenReturn(propertySets);
    }
}
