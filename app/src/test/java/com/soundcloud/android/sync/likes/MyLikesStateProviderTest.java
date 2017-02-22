package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Date;

public class MyLikesStateProviderTest extends AndroidUnitTest {

    private MyLikesStateProvider myLikesStateProvider;

    @Mock private LoadLikesPendingAdditionCommand loadLikesPendingAddition;
    @Mock private LoadLikesPendingRemovalCommand loadLikesPendingRemoval;

    @Before
    public void setUp() throws Exception {
        myLikesStateProvider = new MyLikesStateProvider(loadLikesPendingAddition, loadLikesPendingRemoval);
    }

    @Test
    public void hasLocalChangesIsTrueIfTrackAdditionsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(singletonList(ApiLike.create(PlayableFixtures.fromApiTrack().getUrn(), new Date())));

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueIfTrackRemovalsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(Collections.emptyList());
        when(loadLikesPendingRemoval.call(TYPE_TRACK)).thenReturn(singletonList(ApiLike.create(PlayableFixtures.fromApiTrack().getUrn(), new Date())));
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(Collections.emptyList());

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueIfPlaylistAdditionsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(Collections.emptyList());
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(singletonList(ApiLike.create(PlayableFixtures.fromApiTrack().getUrn(), new Date())));

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsTrueIfPlaylistRemovalsIsNotEmpty() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(Collections.emptyList());
        when(loadLikesPendingRemoval.call(TYPE_TRACK)).thenReturn(Collections.emptyList());
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(Collections.emptyList());
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(singletonList(ApiLike.create(PlayableFixtures.fromApiTrack().getUrn(), new Date())));

        assertThat(myLikesStateProvider.hasLocalChanges()).isTrue();
    }

    @Test
    public void hasLocalChangesIsFalseIfNoPendingAdditionsOrRemovals() throws Exception {
        when(loadLikesPendingAddition.call(TYPE_TRACK)).thenReturn(Collections.emptyList());
        when(loadLikesPendingRemoval.call(TYPE_TRACK)).thenReturn(Collections.emptyList());
        when(loadLikesPendingAddition.call(TYPE_PLAYLIST)).thenReturn(Collections.emptyList());
        when(loadLikesPendingRemoval.call(TYPE_PLAYLIST)).thenReturn(Collections.emptyList());

        assertThat(myLikesStateProvider.hasLocalChanges()).isFalse();
    }
}
