package com.soundcloud.android.sync.track;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.entities.EntitySyncJob;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class EntitySyncJobTest {

    private EntitySyncJob entitySyncJob;

    @Mock private BulkFetchCommand fetchResources;
    @Mock private StoreCommand storeResources;

    @Before
    public void setUp() throws Exception {
        entitySyncJob = new EntitySyncJob(fetchResources, storeResources);
    }

    @Test
    public void resolvesUrnsToFullTracksAndStoresThemLocally() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        when(fetchResources.call()).thenReturn(tracks);

        entitySyncJob.setUrns(Arrays.asList(Urn.forTrack(123L)));
        entitySyncJob.run();

        verify(storeResources).call();
        expect(storeResources.getInput()).toEqual(tracks);
    }

    @Test
    public void resolvesUrnsToFullTracksAndReturnsThemAsUpdated() throws Exception {
        final List<ApiTrack> tracks = ModelFixtures.create(ApiTrack.class, 2);
        when(fetchResources.call()).thenReturn(tracks);

        entitySyncJob.setUrns(Arrays.asList(Urn.forTrack(123L)));
        entitySyncJob.run();

        expect(entitySyncJob.getUpdatedEntities()).toContainExactly(tracks.get(0).toPropertySet(), tracks.get(1).toPropertySet());
    }


}