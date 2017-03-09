package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.emptyList;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;

import android.content.ContentValues;

public class MarkPromotedItemAsStaleCommandTest extends StorageIntegrationTest {

    private MarkPromotedItemAsStaleCommand command;

    @Before
    public void setUp() throws Exception {
        command = new MarkPromotedItemAsStaleCommand(propeller());
    }

    @Test
    public void marksPromotedTrackAsStaleBySettingCreatedAtToZero() throws Exception {
        String adUrn = "dfp:ads:123123-8790798432";
        insertPromotedTrackMetadata(adUrn);

        final PromotedProperties promotedProperties = PromotedProperties.create(adUrn, emptyList(), emptyList(), emptyList(), emptyList(), Optional.absent(), Optional.absent());

        final Track track = ModelFixtures.track();
        final StreamEntity streamEntity = StreamEntity.builder(track.urn(), track.createdAt(), Optional.absent(), Optional.absent(), Optional.absent()).promotedProperties(Optional.of(promotedProperties)).build();
        TrackItem promotedItem = ModelFixtures.trackItem(track, streamEntity);

        command.call(promotedItem.adUrn());

        Query query = from(Table.PromotedTracks.name())
                .whereEq(TableColumns.PromotedTracks.AD_URN, adUrn)
                .whereEq(TableColumns.PromotedTracks.CREATED_AT, 0L);

        assertThat(select(query)).counts(1);
    }

    public void insertPromotedTrackMetadata(String adUrn) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PromotedTracks._ID, 123);
        cv.put(TableColumns.PromotedTracks.CREATED_AT, System.currentTimeMillis());
        cv.put(TableColumns.PromotedTracks.AD_URN, adUrn);
        cv.put(TableColumns.PromotedTracks.PROMOTER_ID, 83);
        cv.put(TableColumns.PromotedTracks.PROMOTER_NAME, "SoundCloud");
        cv.put(TableColumns.PromotedTracks.TRACKING_TRACK_CLICKED_URLS, "promoted1 promoted2");
        cv.put(TableColumns.PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS, "promoted3 promoted4");
        cv.put(TableColumns.PromotedTracks.TRACKING_TRACK_PLAYED_URLS, "promoted5 promoted6");
        cv.put(TableColumns.PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS, "promoted7 promoted8");
        testFixtures().insertInto(Table.PromotedTracks, cv);
    }
}
