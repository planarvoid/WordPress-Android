package com.soundcloud.android.stations;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LikedStationsSyncerTest {

    private static final List<Urn> LIKED_URNS = newArrayList(Urn.forArtistStation(1));
    private static final List<Urn> UNLIKED_URNS = newArrayList(Urn.forArtistStation(2));
    private static final ModelCollection<Urn> RETURNED_URNS = new ModelCollection<>(newArrayList(Urn.forArtistStation(3),
                                                                                                 Urn.forArtistStation(4)));
    private static final List<ApiStationMetadata> API_STATION_METADATAS = asList(
            getStationMetadata(Urn.forArtistStation(3)), getStationMetadata(Urn.forArtistStation(4)));


    @Mock private StationsApi stationsApi;
    @Mock private StationsStorage storage;
    private LikedStationsSyncer likedStationsSyncer;

    @Before
    public void setUp() throws ApiRequestException, IOException, ApiMapperException {
        MockitoAnnotations.initMocks(this);
        likedStationsSyncer = new LikedStationsSyncer(stationsApi, storage);
        when(stationsApi.updateLikedStations(any(LikedStationsPostBody.class))).thenReturn(RETURNED_URNS);
        when(stationsApi.fetchStations(asList(Urn.forArtistStation(3),
                                              Urn.forArtistStation(4)))).thenReturn(API_STATION_METADATAS);
        when(storage.getStations()).thenReturn(Collections.emptyList());
        when(storage.getLocalLikedStations()).thenReturn(LIKED_URNS);
        when(storage.getLocalUnlikedStations()).thenReturn(UNLIKED_URNS);
    }

    @Test
    public void shouldDeleteAndUpsertLikedStations() throws Exception {
        assertTrue(likedStationsSyncer.call());

        verify(storage).getLocalLikedStations();
        verify(storage).getLocalUnlikedStations();
        verify(stationsApi).updateLikedStations(eq(LikedStationsPostBody.create(UNLIKED_URNS, LIKED_URNS)));
        verify(storage).setLikedStationsAndAddNewMetaData(eq(RETURNED_URNS.getCollection()),
                                                          eq(API_STATION_METADATAS));
    }

    @Test
    public void shouldUpdateApiWhenNoLocalChanges() throws Exception {
        when(storage.getLocalLikedStations()).thenReturn(Collections.emptyList());
        when(storage.getLocalUnlikedStations()).thenReturn(Collections.emptyList());

        assertTrue(likedStationsSyncer.call());

        verify(storage).getLocalLikedStations();
        verify(storage).getLocalUnlikedStations();
        verify(stationsApi).updateLikedStations(eq(LikedStationsPostBody.create(Collections.emptyList(),
                                                                                Collections.emptyList())));
        verify(storage).setLikedStationsAndAddNewMetaData(eq(RETURNED_URNS.getCollection()),
                                                          eq(API_STATION_METADATAS));
    }

    @Test
    public void shouldAddUnknownStations() throws Exception {
        final Urn known = Urn.forArtistStation(1111);
        final Urn unknown = Urn.forArtistStation(2222);
        final ApiStationMetadata newMetadata = new ApiStationMetadata(unknown, "", "", "", "");

        when(storage.getStations()).thenReturn(singletonList(known));
        when(stationsApi.updateLikedStations(any(LikedStationsPostBody.class))).thenReturn(new ModelCollection<>(asList(
                known,
                unknown)));
        when(stationsApi.fetchStations(singletonList(unknown))).thenReturn(singletonList(newMetadata));

        assertTrue(likedStationsSyncer.call());

        verify(storage).setLikedStationsAndAddNewMetaData(eq(asList(known, unknown)), eq(singletonList(newMetadata)));
    }

    @Test
    public void shouldNotLikeUnknownStations() throws Exception {
        final Urn station = Urn.forArtistStation(123L);
        final Urn stationWithoutMetadata = Urn.forArtistStation(124L);

        final ApiStationMetadata newMetadata = getStationMetadata(station);

        when(storage.getStations()).thenReturn(Collections.emptyList());
        when(stationsApi.updateLikedStations(any(LikedStationsPostBody.class))).thenReturn(new ModelCollection<>(asList(
                station,
                stationWithoutMetadata)));

        when(stationsApi.fetchStations(asList(station, stationWithoutMetadata))).thenReturn(singletonList(newMetadata));

        assertTrue(likedStationsSyncer.call());

        verify(storage).setLikedStationsAndAddNewMetaData(eq(singletonList(station)),
                                                          eq(singletonList(newMetadata)));

    }

    private static ApiStationMetadata getStationMetadata(Urn stationUrn) {
        return new ApiStationMetadata(stationUrn, "", "", "", "");
    }

}
