package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.storage.Tables.ChartTracks;
import static com.soundcloud.android.storage.Tables.Charts;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.sync.charts.ApiChartBucket;
import com.soundcloud.android.sync.charts.ApiImageResource;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.schema.BulkInsertValues;
import com.soundcloud.propeller.schema.Column;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Arrays;
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

            private void storeChartBucket(PropellerDatabase propeller,
                                          List<ApiChart<ApiImageResource>> bucket,
                                          int bucketType) {
                clearBucket(bucketType);

                final BulkInsertValues.Builder chartTracks = new BulkInsertValues.Builder(getChartTrackColumns());

                for (final ApiChart<ApiImageResource> apiChart : bucket) {
                    //Store the chart
                    final InsertResult chartInsert = propeller.insert(Charts.TABLE,
                                                                      buildChartContentValues(apiChart, bucketType));
                    step(chartInsert);

                    //Store chart tracks
                    for (ImageResource track : apiChart.tracks()) {
                        chartTracks.addRow(buildChartTrackContentValues(track, chartInsert.getRowId(), bucketType));
                    }
                }
                step(propeller.bulkInsert(ChartTracks.TABLE, chartTracks.build()));
            }
        });
    }

    private List<Column> getChartTrackColumns() {
        return Arrays.asList(
                ChartTracks.CHART_ID,
                ChartTracks.TRACK_ID,
                ChartTracks.TRACK_ARTWORK,
                ChartTracks.BUCKET_TYPE
        );
    }

    private List<Object> buildChartTrackContentValues(ImageResource chartTrack, long chartId, int bucketType) {
        return Arrays.asList(
                chartId,
                chartTrack.getUrn().getNumericId(),
                chartTrack.getImageUrlTemplate().orNull(),
                bucketType
        );
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
        propeller.delete(ChartTracks.TABLE, filter().whereEq(ChartTracks.BUCKET_TYPE, chartBucketType));
        propeller.delete(Charts.TABLE, filter().whereEq(Charts.BUCKET_TYPE, chartBucketType));
    }

    public void clearTables() {
        propeller.delete(ChartTracks.TABLE);
        propeller.delete(Charts.TABLE);
    }
}
