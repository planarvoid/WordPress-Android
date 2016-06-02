package com.soundcloud.android.sync.charts;

import static com.soundcloud.android.storage.Tables.ChartTracks;
import static com.soundcloud.android.storage.Tables.Charts;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;

public class StoreChartsCommand extends DefaultWriteStorageCommand<ApiChartBucket, WriteResult> {

    private final PropellerDatabase propeller;

    @Inject
    public StoreChartsCommand(PropellerDatabase database) {
        super(database);
        this.propeller = database;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final ApiChartBucket input) {
        clearTables();
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                storeChartBucket(propeller, input.getGlobal(), Charts.BUCKET_TYPE_GLOBAL);
                storeChartBucket(propeller, input.getFeaturedGenres(), Charts.BUCKET_TYPE_FEATURED_GENRE);
            }

            private void storeChartBucket(PropellerDatabase propeller, ModelCollection<ApiChart> bucket, int bucketType) {
                for (final ApiChart apiChart : bucket) {
                    //Store the chart
                    final InsertResult chartInsert = propeller.insert(Charts.TABLE, buildChartContentValues(apiChart, bucketType));
                    step(chartInsert);

                    //Store chart tracks
                    for (ApiTrack chartTrack : apiChart.getTracks()) {
                        writeTrack(propeller, chartTrack);
                        step(propeller.upsert(ChartTracks.TABLE, buildChartTrackContentValues(chartTrack, chartInsert.getRowId())));
                    }
                }
            }

            //TODO: Create a way of sharing the track writing logic, with a base class (e.g. WriteTrackTransaction)
            private void writeTrack(PropellerDatabase propeller, ApiTrack seedTrack) {
                step(propeller.upsert(Table.Users, StoreUsersCommand.buildUserContentValues(seedTrack.getUser())));
                step(propeller.upsert(Table.Sounds, StoreTracksCommand.buildTrackContentValues(seedTrack)));
                step(propeller.upsert(Table.TrackPolicies, StoreTracksCommand.buildPolicyContentValues(seedTrack)));
            }
        });
    }

    private ContentValues buildChartTrackContentValues(ApiTrack chartTrack, long chartId) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(ChartTracks.CHART_ID.name(), chartId);
        contentValues.put(ChartTracks.SOUND_ID.name(), chartTrack.getUrn().getNumericId());
        return contentValues;
    }

    private ContentValues buildChartContentValues(ApiChart apiChart, int bucketType) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(Charts.DISPLAY_NAME.name(), apiChart.getDisplayName());
        if (apiChart.getGenre() != null) {
            contentValues.put(Charts.GENRE.name(), apiChart.getGenre().toString());
        }
        contentValues.put(Charts.TYPE.name(), apiChart.getType().value());
        contentValues.put(Charts.CATEGORY.name(), apiChart.getCategory().value());
        contentValues.put(Charts.BUCKET_TYPE.name(), bucketType);
        return contentValues;
    }

    public void clearTables() {
        propeller.delete(ChartTracks.TABLE);
        propeller.delete(Charts.TABLE);
    }
}
