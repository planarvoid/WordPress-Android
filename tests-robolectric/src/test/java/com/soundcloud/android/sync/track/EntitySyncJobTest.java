package com.soundcloud.android.sync.track;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.entities.EntitySyncJob;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class EntitySyncJobTest {

    private EntitySyncJob entitySyncJob;

    @Mock private BulkFetchCommand fetchResources;
    @Mock private StoreTracksCommand storeResources;

    @Before
    public void setUp() throws Exception {
        entitySyncJob = new EntitySyncJob(fetchResources, storeResources);
    }

    @Test
    public void removesInvalidUrnsFromEntityFetches() throws Exception {
        entitySyncJob.setUrns(Arrays.asList(Urn.forTrack(123L), Urn.forTrack(-321L)));
        entitySyncJob.run();

        expect(fetchResources.getInput()).toEqual(Arrays.asList(Urn.forTrack(123L)));
    }

    @Test
    public void resolvesUrnsToFullTracksAndStoresThemLocally() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        when(fetchResources.call()).thenReturn(tracks);

        entitySyncJob.setUrns(Arrays.asList(Urn.forTrack(123L)));
        entitySyncJob.run();

        verify(storeResources).call(tracks);
    }

    @Test
    public void resolvesUrnsToFullTracksAndReturnsThemAsUpdated() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        when(fetchResources.call()).thenReturn(tracks);

        entitySyncJob.setUrns(Arrays.asList(Urn.forTrack(123L)));
        entitySyncJob.run();

        expect(entitySyncJob.getUpdatedEntities()).toContainExactly(tracks.get(0).toPropertySet(), tracks.get(1).toPropertySet());
    }

    @Test
    public void savesFetchException() throws Exception {
        final Exception exception = new IOException();
        when(fetchResources.call()).thenThrow(exception);

        entitySyncJob.setUrns(Arrays.asList(Urn.forTrack(123L)));
        entitySyncJob.run();

        expect(entitySyncJob.getException()).toBe(exception);
    }

    @Test
    public void didNotResultInAChangeIfExceptionThrown() throws Exception {
        final Exception exception = new IOException();
        when(fetchResources.call()).thenThrow(exception);

        entitySyncJob.setUrns(Arrays.asList(Urn.forTrack(123L)));
        entitySyncJob.run();

        expect(entitySyncJob.resultedInAChange()).toBe(false);
    }


}