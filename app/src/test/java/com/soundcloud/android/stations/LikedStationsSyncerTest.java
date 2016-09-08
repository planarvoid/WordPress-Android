package com.soundcloud.android.stations;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.DateProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LikedStationsSyncerTest {

    private static final List<Urn> LIKED_URNS = newArrayList(Urn.forArtistStation(1));
    private static final List<Urn> UNLIKED_URNS = newArrayList(Urn.forArtistStation(2));
    private static final ModelCollection<Urn> RETURNED_URNS = new ModelCollection(newArrayList(Urn.forArtistStation(3),
                                                                                               Urn.forArtistStation(4)));

    @Mock private StationsApi stationsApi;
    @Mock private StationsStorage storage;
    private LikedStationsSyncer likedStationsSyncer;

    @Before
    public void setUp() throws ApiRequestException, IOException, ApiMapperException {
        MockitoAnnotations.initMocks(this);
        likedStationsSyncer = new LikedStationsSyncer(stationsApi, storage);
        when(stationsApi.updateLikedStations(any(LikedStationsPostBody.class))).thenReturn(RETURNED_URNS);
    }

    @Test
    public void shouldDeleteAndUpsertLikedStations() throws Exception {
        when(storage.getLocalLikedStations()).thenReturn(LIKED_URNS);
        when(storage.getLocalUnlikedStations()).thenReturn(UNLIKED_URNS);

        assertTrue(likedStationsSyncer.call());
        verify(storage).getLocalLikedStations();
        verify(storage).getLocalUnlikedStations();
        verify(stationsApi).updateLikedStations(eq(LikedStationsPostBody.create(UNLIKED_URNS, LIKED_URNS)));
        verify(storage).setLikedStations(eq(RETURNED_URNS.getCollection()));
    }

    @Test
    public void shouldUpdateApiWhenNoLocalChanges() throws Exception {
        when(storage.getLocalLikedStations()).thenReturn(Collections.<Urn>emptyList());
        when(storage.getLocalUnlikedStations()).thenReturn(Collections.<Urn>emptyList());

        assertTrue(likedStationsSyncer.call());
        verify(storage).getLocalLikedStations();
        verify(storage).getLocalUnlikedStations();
        verify(stationsApi).updateLikedStations(eq(
                LikedStationsPostBody.create(Collections.<Urn>emptyList(), Collections.<Urn>emptyList())));
        verify(storage).setLikedStations(eq(RETURNED_URNS.getCollection()));
    }

}
