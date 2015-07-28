package com.soundcloud.android.waveform;

import static com.soundcloud.android.waveform.WaveformStorage.WaveformDataHelper.deserialize;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
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

public class WaveformStorage {

    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;

    @Inject
    public WaveformStorage(PropellerRx propellerRx, DateProvider dateProvider) {
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
    }

    public Observable<ChangeResult> store(Urn trackUrn, WaveformData data) {
        return propellerRx.upsert(Table.Waveforms, buildContentValues(trackUrn, data));
    }

    public Observable<WaveformData> load(Urn trackUrn) {
        return propellerRx
                .query(waveformQuery(trackUrn))
                .map(new WaveformMapper());
    }

    private ContentValues buildContentValues(Urn trackUrn, WaveformData data) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Waveforms.TRACK_ID, trackUrn.getNumericId())
                .put(TableColumns.Waveforms.MAX_AMPLITUDE, data.maxAmplitude)
                .put(TableColumns.Waveforms.SAMPLES, WaveformDataHelper.serializedSamples(data.samples))
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

        @Override
        public WaveformData map(CursorReader reader) {
            return new WaveformData(
                    reader.getInt(TableColumns.Waveforms.MAX_AMPLITUDE),
                    deserialize(reader.getString(TableColumns.Waveforms.SAMPLES)));
        }
    }

    public static class WaveformDataHelper {

        public static int[] deserialize(String serializedSamples) throws NumberFormatException {
            final String[] array = serializedSamples.split(",");
            int[] samples = new int[array.length];
            for (int i = 0; i < array.length; i++) {
                samples[i] = Integer.parseInt(array[i]);
            }
            return samples;
        }

        public static String serializedSamples(int[] samples) {
            final StringBuilder sb = new StringBuilder();
            boolean firstTime = true;

            for (int token : samples) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    sb.append(',');
                }
                sb.append(token);
            }
            return sb.toString();
        }
    }
}
