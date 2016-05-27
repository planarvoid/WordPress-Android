package com.soundcloud.android.discovery;

import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.SoundView.ARTWORK_URL;
import static com.soundcloud.android.storage.TableColumns.SoundView._ID;
import static com.soundcloud.android.storage.Tables.Charts;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.ChartTracks;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class ChartsStorage {
    private final PropellerRx propellerRx;
    private final Scheduler scheduler;

    private final Func1<Chart, Observable<Chart>> addTracksToChart = new Func1<Chart, Observable<Chart>>() {
        @Override
        public Observable<Chart> call(final Chart chart) {
            return chartTracks(chart.localId())
                    .map(new Func1<List<ChartTrack>, Chart>() {
                        @Override
                        public Chart call(List<ChartTrack> chartTracks) {
                            return chart.copyWithTracks(chartTracks);
                        }
                    });
        }
    };

    @Inject
    ChartsStorage(PropellerRx propellerRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.propellerRx = propellerRx;
        this.scheduler = scheduler;
    }

    Observable<List<Chart>> charts() {
        final Query query = Query.from(Charts.TABLE)
                                 .select(Charts._ID,
                                         Charts.TYPE,
                                         Charts.TITLE,
                                         Charts.CATEGORY,
                                         Charts.GENRE,
                                         Charts.PAGE);

        return propellerRx.query(query)
                          .map(new ChartMapper())
                          .flatMap(addTracksToChart)
                          .toList()
                          .subscribeOn(scheduler);
    }


    private Observable<List<ChartTrack>> chartTracks(final Long chartId) {

        final Where soundsViewJoin = filter().whereEq(ChartTracks.SOUND_ID, SoundView.field(_ID));
        final Query query = Query.from(ChartTracks.TABLE)
                                 .select(ChartTracks.SOUND_ID, field(SoundView.field(ARTWORK_URL)).as(ARTWORK_URL))
                                 .innerJoin(SoundView.name(), soundsViewJoin)
                                 .whereEq(ChartTracks.CHART_ID, chartId);

        return propellerRx.query(query).map(new TrackMapper()).toList();
    }


    private static final class TrackMapper extends RxResultMapper<ChartTrack> {
        @Override
        public ChartTrack map(CursorReader cursorReader) {
            return ChartTrack.create(Urn.forTrack(cursorReader.getLong(ChartTracks.SOUND_ID)),
                                     Optional.fromNullable(cursorReader.getString(ARTWORK_URL)));
        }
    }

    private static final class ChartMapper extends RxResultMapper<Chart> {
        @Override
        public Chart map(CursorReader cursorReader) {
            final String genre = cursorReader.getString(Charts.GENRE);
            return Chart.create(cursorReader.getLong(Charts._ID),
                                ChartType.from(cursorReader.getString(Charts.TYPE)),
                                ChartCategory.from(cursorReader.getString(Charts.CATEGORY)),
                                cursorReader.getString(Charts.TITLE),
                                cursorReader.getString(Charts.PAGE),
                                genre != null ? Optional.of(new Urn(genre)) : Optional.<Urn>absent());
        }
    }
}
