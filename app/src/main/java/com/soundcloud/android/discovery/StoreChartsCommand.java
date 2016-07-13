package com.soundcloud.android.discovery;

import static com.soundcloud.android.storage.Tables.ChartTracks;
import static com.soundcloud.android.storage.Tables.Charts;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.sync.charts.ApiChartBucket;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;

class StoreChartsCommand extends DefaultWriteStorageCommand<List<ApiChartBucket>, WriteResult> {

    private final PropellerDatabase propeller;

    @Inject
    public StoreChartsCommand(PropellerDatabase database) {
        super(database);
        this.propeller = database;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final List<ApiChartBucket> input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (final ApiChartBucket apiChartBucket : input) {
                    storeChartBucket(propeller, apiChartBucket.getCharts(), apiChartBucket.getBucketType());
                }
            }

            private void storeChartBucket(PropellerDatabase propeller, List<ApiChart> bucket, int bucketType) {
                clearBucket(bucketType);
                for (final ApiChart apiChart : bucket) {
                    //Store the chart
                    final InsertResult chartInsert = propeller.insert(Charts.TABLE,
                                                                      buildChartContentValues(apiChart, bucketType));
                    step(chartInsert);

                    //Store chart tracks
                    for (ApiTrack track : apiChart.tracks()) {
                        writeTrack(propeller, track);
                        step(propeller.upsert(ChartTracks.TABLE,
                                              buildChartTrackContentValues(track, chartInsert.getRowId(), bucketType)));
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

    private ContentValues buildChartTrackContentValues(TrackRecord chartTrack, long chartId, int bucketType) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(ChartTracks.CHART_ID.name(), chartId);
        contentValues.put(ChartTracks.SOUND_ID.name(), chartTrack.getUrn().getNumericId());
        contentValues.put(Charts.BUCKET_TYPE.name(), bucketType);
        return contentValues;
    }

    private ContentValues buildChartContentValues(ApiChart apiChart, int bucketType) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(Charts.DISPLAY_NAME.name(), apiChart.displayName());
        if (apiChart.genre() != null) {
            contentValues.put(Charts.GENRE.name(), apiChart.genre().toString());
        }
        contentValues.put(Charts.TYPE.name(), apiChart.type().value());
        contentValues.put(Charts.CATEGORY.name(), apiChart.category().value());
        contentValues.put(Charts.BUCKET_TYPE.name(), bucketType);
        return contentValues;
    }

    private void clearBucket(int chartBucketType) {
        final Where bucketOfType = filter().whereEq(Charts.BUCKET_TYPE, chartBucketType);
        propeller.delete(ChartTracks.TABLE, bucketOfType);
        propeller.delete(Charts.TABLE, bucketOfType);
    }

    public void clearTables() {
        propeller.delete(ChartTracks.TABLE);
        propeller.delete(Charts.TABLE);
    }
}
