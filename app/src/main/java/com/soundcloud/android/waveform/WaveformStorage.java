package com.soundcloud.android.waveform;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;

class WaveformStorage {

    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;
    private final WaveformSerializer serializer;

    @Inject
    public WaveformStorage(PropellerRx propellerRx, CurrentDateProvider dateProvider, WaveformSerializer serializer) {
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
        this.serializer = serializer;
    }

    public Observable<ChangeResult> store(Urn trackUrn, WaveformData data) {
        return propellerRx.upsert(Table.Waveforms, buildContentValues(trackUrn, data));
    }

    public Observable<WaveformData> load(Urn trackUrn) {
        return propellerRx
                .query(waveformQuery(trackUrn))
                .map(new WaveformMapper(serializer));
    }

    private ContentValues buildContentValues(Urn trackUrn, WaveformData data) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Waveforms.TRACK_ID, trackUrn.getNumericId())
                .put(TableColumns.Waveforms.MAX_AMPLITUDE, data.maxAmplitude)
                .put(TableColumns.Waveforms.SAMPLES, serializer.serialize(data.samples))
                .put(TableColumns.Waveforms.CREATED_AT, dateProvider.getCurrentTime())
                .get();
    }

    private Query waveformQuery(Urn trackUrn) {
        return Query.from(Table.Waveforms.name())
                .select(
                        TableColumns.Waveforms.MAX_AMPLITUDE,
                        TableColumns.Waveforms.SAMPLES
                ).whereEq(TableColumns.Waveforms.TRACK_ID, trackUrn.getNumericId());
    }

    private static class WaveformMapper extends RxResultMapper<WaveformData> {

        private final WaveformSerializer serializer;

        private WaveformMapper(WaveformSerializer serializer) {
            this.serializer = serializer;
        }

        @Override
        public WaveformData map(CursorReader reader) {
            return new WaveformData(
                    reader.getInt(TableColumns.Waveforms.MAX_AMPLITUDE),
                    serializer.deserialize(reader.getString(TableColumns.Waveforms.SAMPLES)));
        }
    }
}
