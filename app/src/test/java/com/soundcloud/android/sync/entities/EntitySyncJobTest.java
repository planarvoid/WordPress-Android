package com.soundcloud.android.sync.entities;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.commands.PublishUpdateEventCommand;
import com.soundcloud.android.testsupport.TrackFixtures;
import io.reactivex.functions.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class EntitySyncJobTest {

    private EntitySyncJob entitySyncJob;

    @Mock private BulkFetchCommand<ApiTrack, ApiTrack> fetchResources;
    @Mock private BulkFetchCommand<ApiTrack, ApiTrack> fetchResources2;
    @Mock private Consumer<Collection<ApiTrack>> storeResources;
    @Mock private Consumer<Collection<ApiTrack>> storeResources2;
    @Mock private PublishUpdateEventCommand<ApiTrack> publishUpdateEventCommand;
    @Mock private PublishUpdateEventCommand<ApiTrack> publishUpdateEventCommand2;
    @Captor private ArgumentCaptor<Collection<ApiTrack>> collectionArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        entitySyncJob = new EntitySyncJob<>(fetchResources, storeResources, publishUpdateEventCommand);
    }

    @Test
    public void resolvesUrnsToFullTracksAndStoresThemLocally() throws Exception {
        final List<ApiTrack> tracks = TrackFixtures.apiTracks(2);
        when(fetchResources.call()).thenReturn(tracks);

        entitySyncJob.setUrns(singletonList(Urn.forTrack(123L)));
        entitySyncJob.run();

        verify(storeResources).accept(tracks);
    }

    @Test
    public void resolvesUrnsToFullTracksAndReturnsThemAsUpdated() throws Exception {
        final List<ApiTrack> tracks = TrackFixtures.apiTracks(2);
        when(fetchResources.call()).thenReturn(tracks);

        entitySyncJob.setUrns(singletonList(Urn.forTrack(123L)));
        entitySyncJob.run();

        entitySyncJob.publishSyncEvent();
        verify(publishUpdateEventCommand).call(collectionArgumentCaptor.capture());
        assertThat(collectionArgumentCaptor.getValue()).containsExactly(tracks.get(0), tracks.get(1));
    }

    @Test
    public void savesFetchException() throws Exception {
        final Exception exception = new IOException();
        when(fetchResources.call()).thenThrow(exception);

        entitySyncJob.setUrns(singletonList(Urn.forTrack(123L)));
        entitySyncJob.run();

        assertThat(entitySyncJob.getException()).isSameAs(exception);
    }

    @Test
    public void didNotResultInAChangeIfExceptionThrown() throws Exception {
        final Exception exception = new IOException();
        when(fetchResources.call()).thenThrow(exception);

        entitySyncJob.setUrns(singletonList(Urn.forTrack(123L)));
        entitySyncJob.run();

        assertThat(entitySyncJob.resultedInAChange()).isSameAs(false);
    }

    @Test
    public void notEqualsWhenComparingToNullUrnList() {
        entitySyncJob.setUrns(asList(Urn.forTrack(123L), Urn.forTrack(456L), Urn.forTrack(789L)));
        EntitySyncJob otherSyncJob = syncJobWithMockedDependencies();
        // never calling otherSyncJob::setUrns

        assertThat(entitySyncJob.equals(otherSyncJob)).isFalse();
    }

    @Test
    public void notEqualsWhenUrnsAreDifferent() {
        EntitySyncJob otherSyncJob = syncJobWithMockedDependencies();
        entitySyncJob.setUrns(asList(Urn.forTrack(123L), Urn.forTrack(456L), Urn.forTrack(789L)));
        otherSyncJob.setUrns(asList(Urn.forTrack(987L), Urn.forTrack(654L), Urn.forTrack(321L)));

        assertThat(entitySyncJob.equals(otherSyncJob)).isFalse();
    }

    @Test
    public void equalsWhenJobUrnsAreTheSameUnordered() {
        EntitySyncJob otherSyncJob = syncJobWithMockedDependencies();
        entitySyncJob.setUrns(asList(Urn.forTrack(123L), Urn.forTrack(456L), Urn.forTrack(789L)));
        otherSyncJob.setUrns(asList(Urn.forTrack(789L), Urn.forTrack(123L), Urn.forTrack(456L)));

        assertThat(entitySyncJob.equals(otherSyncJob)).isTrue();
    }

    private EntitySyncJob<ApiTrack> syncJobWithMockedDependencies() {
        return new EntitySyncJob<>(fetchResources2, storeResources2, publishUpdateEventCommand2);
    }

}
