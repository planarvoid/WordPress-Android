package com.soundcloud.android.sync.entities;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.List;

public class EntitySyncJobTest extends AndroidUnitTest {

    private EntitySyncJob entitySyncJob;

    @Mock private BulkFetchCommand fetchResources;
    @Mock private StoreTracksCommand storeResources;

    @Before
    public void setUp() throws Exception {
        entitySyncJob = new EntitySyncJob(fetchResources, storeResources);
    }

    @Test
    public void resolvesUrnsToFullTracksAndStoresThemLocally() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        when(fetchResources.call()).thenReturn(tracks);

        entitySyncJob.setUrns(singletonList(Urn.forTrack(123L)));
        entitySyncJob.run();

        verify(storeResources).call(tracks);
    }

    @Test
    public void resolvesUrnsToFullTracksAndReturnsThemAsUpdated() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        when(fetchResources.call()).thenReturn(tracks);

        entitySyncJob.setUrns(singletonList(Urn.forTrack(123L)));
        entitySyncJob.run();

        assertThat(entitySyncJob.getUpdatedEntities()).containsExactly(
                tracks.get(0).toPropertySet(),
                tracks.get(1).toPropertySet());
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


}
