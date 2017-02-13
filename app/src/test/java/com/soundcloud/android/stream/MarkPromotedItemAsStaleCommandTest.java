package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;
import static java.util.Collections.emptyList;

import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.java.collections.PropertySet;
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

        PropertySet track = PropertySet.create();
        track.put(PromotedItemProperty.AD_URN, adUrn);
        track.put(PromotedItemProperty.TRACK_CLICKED_URLS, emptyList());
        track.put(PromotedItemProperty.TRACK_CLICKED_URLS, emptyList());
        track.put(PromotedItemProperty.TRACK_IMPRESSION_URLS, emptyList());
        track.put(PromotedItemProperty.TRACK_PLAYED_URLS, emptyList());
        track.put(PromotedItemProperty.PROMOTER_CLICKED_URLS, emptyList());
        track.put(PromotedItemProperty.PROMOTER_URN, Optional.absent());
        track.put(PromotedItemProperty.PROMOTER_NAME, Optional.absent());
        PromotedTrackItem promotedItem = PromotedTrackItem.from(track);

        command.call(promotedItem);

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
