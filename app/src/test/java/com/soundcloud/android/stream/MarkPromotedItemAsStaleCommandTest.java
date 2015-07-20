package com.soundcloud.android.stream;

import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.test.matchers.QueryMatchers;
import org.hamcrest.MatcherAssert;
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
        command.call(track);

        Query query = from(Table.PromotedTracks.name())
                .whereEq(TableColumns.PromotedTracks.AD_URN, adUrn)
                .whereEq(TableColumns.PromotedTracks.CREATED_AT, 0L);

        MatcherAssert.assertThat(select(query), QueryMatchers.counts(1));
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
