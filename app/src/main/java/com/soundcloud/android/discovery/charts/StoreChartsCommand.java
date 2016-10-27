package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.storage.Tables.ChartTracks;
import static com.soundcloud.android.storage.Tables.Charts;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.sync.charts.ApiChartBucket;
import com.soundcloud.android.sync.charts.ApiImageResource;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            private void storeChartBucket(PropellerDatabase propeller, List<ApiChart<ApiImageResource>> bucket, int bucketType) {
                clearBucket(bucketType);

                final ArrayList<ContentValues> chartTracks = new ArrayList<>();

                for (final ApiChart<ApiImageResource> apiChart : bucket) {
                    //Store the chart
                    final InsertResult chartInsert = propeller.insert(Charts.TABLE, buildChartContentValues(apiChart, bucketType));
                    step(chartInsert);

                    //Store chart tracks
                    for (ImageResource track : apiChart.tracks()) {
                        chartTracks.add(buildChartTrackContentValues(track, chartInsert.getRowId(), bucketType));
                    }
                }
                step(propeller.bulkInsert_experimental(ChartTracks.TABLE,getChartTrackColumns(), chartTracks));
            }
        });
    }

    private Map<String, Class> getChartTrackColumns() {
        final HashMap<String, Class> columns = new HashMap<>(4);
        columns.put(ChartTracks.CHART_ID.name(), Long.class);
        columns.put(ChartTracks.TRACK_ID.name(), Long.class);
        columns.put(ChartTracks.TRACK_ARTWORK.name(), String.class);
        columns.put(ChartTracks.BUCKET_TYPE.name(), Long.class);
        return columns;
    }

    private ContentValues buildChartTrackContentValues(ImageResource chartTrack, long chartId, int bucketType) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(ChartTracks.CHART_ID.name(), chartId);
        contentValues.put(ChartTracks.TRACK_ID.name(), chartTrack.getUrn().getNumericId());
        final Optional<String> imageUrlTemplate = chartTrack.getImageUrlTemplate();
        contentValues.put(ChartTracks.TRACK_ARTWORK.name(), imageUrlTemplate.orNull());
        contentValues.put(ChartTracks.BUCKET_TYPE.name(), bucketType);
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
        propeller.delete(ChartTracks.TABLE, filter().whereEq(ChartTracks.BUCKET_TYPE, chartBucketType));
        propeller.delete(Charts.TABLE, filter().whereEq(Charts.BUCKET_TYPE, chartBucketType));
    }

    public void clearTables() {
        propeller.delete(ChartTracks.TABLE);
        propeller.delete(Charts.TABLE);
    }
}
