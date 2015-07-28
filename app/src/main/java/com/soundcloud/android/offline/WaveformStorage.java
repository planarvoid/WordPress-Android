package com.soundcloud.android.offline;

import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.android.waveform.WaveformSerializer;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import android.content.ContentValues;

import javax.inject.Inject;

class WaveformStorage {

    private final static String HAS_WAVEFORM = "has_waveform";
    private final PropellerDatabase propeller;
    private final DateProvider dateProvider;
    private final WaveformSerializer serializer;

    @Inject
    WaveformStorage(PropellerDatabase propeller, DateProvider dateProvider, WaveformSerializer serializer) {
        this.propeller = propeller;
        this.dateProvider = dateProvider;
        this.serializer = serializer;
    }

    public boolean hasWaveform(Urn trackUrn) {
        final Query query = Query.apply(exists(Query.from(Table.Waveforms.name())
                .whereEq(TableColumns.Waveforms.TRACK_ID, trackUrn.getNumericId()))
                .as(HAS_WAVEFORM));

        return propeller.query(query).first(Boolean.class);
    }

    public void store(Urn trackUrn, WaveformData data) {
        propeller.upsert(Table.Waveforms, buildContentValues(trackUrn, data));
    }

    private ContentValues buildContentValues(Urn trackUrn, WaveformData data) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Waveforms.TRACK_ID, trackUrn.getNumericId())
                .put(TableColumns.Waveforms.MAX_AMPLITUDE, data.maxAmplitude)
                .put(TableColumns.Waveforms.SAMPLES, serializer.serialize(data.samples))
                .put(TableColumns.Waveforms.CREATED_AT, dateProvider.getCurrentTime())
                .get();
    }

}
