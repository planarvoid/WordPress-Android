package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.ApiDateFormat;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class StationsSyncPostBodyTest extends AndroidUnitTest {
    private PropertySet station;
    private StationsSyncPostBody postBody;

    @Before
    public void setUp() {
        station = StationFixtures.stationProperties();
        List<PropertySet> recentStationsToSync = StationFixtures.getRecentStationsToSync(station);
        postBody = new StationsSyncPostBody(recentStationsToSync);
    }

    @Test
    public void shouldHaveRecentStationsToSync() {
        final StationsSyncPostBody.RecentStations.RecentStation actual = postBody.getRecent().getCollection().get(0);
        assertThat(actual.getLastPlayed()).isEqualTo(ApiDateFormat.formatDate(station.get(StationProperty.UPDATED_LOCALLY_AT)));
        assertThat(actual.getUrn()).isEqualTo(station.get(StationProperty.URN).toString());
    }

    @Test
    public void shouldHaveEmptySavedStations() {
        final List<StationsSyncPostBody.SavedStations.SavedStation> actual = postBody.getSaved().getCollection();
        assertThat(actual).isEqualTo(Collections.emptyList());
    }
}