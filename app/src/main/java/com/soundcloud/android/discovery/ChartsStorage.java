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
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.ChartTracks;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

class ChartsStorage {

    private final PropellerRx propellerRx;
    private final Scheduler scheduler;

    @Inject
    ChartsStorage(PropellerRx propellerRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.propellerRx = propellerRx;
        this.scheduler = scheduler;
    }

    Observable<PropertySet> charts() {
        final Query query = Query.from(Charts.TABLE)
                                 .select(Charts._ID,
                                         Charts.TYPE,
                                         Charts.TITLE,
                                         Charts.CATEGORY,
                                         Charts.GENRE,
                                         Charts.PAGE);

        return propellerRx.query(query)
                          .map(new ChartMapper())
                          .switchIfEmpty(Observable.<PropertySet>empty())
                          .subscribeOn(scheduler);
    }

    Observable<PropertySet> chartTracks(final Long chartId) {

        final Where soundsViewJoin = filter().whereEq(ChartTracks.SOUND_ID, SoundView.field(_ID));
        final Query query = Query.from(ChartTracks.TABLE)
                                 .select(ChartTracks.SOUND_ID, field(SoundView.field(ARTWORK_URL)).as(ARTWORK_URL))
                                 .innerJoin(SoundView.name(), soundsViewJoin)
                                 .whereEq(ChartTracks.CHART_ID, chartId);

        return propellerRx.query(query)
                          .map(new TrackMapper())
                          .switchIfEmpty(Observable.<PropertySet>empty())
                          .subscribeOn(scheduler);
    }


    private static final class TrackMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(EntityProperty.URN, Urn.forTrack(cursorReader.getLong(ChartTracks.SOUND_ID)));
            propertySet.put(EntityProperty.IMAGE_URL_TEMPLATE,
                            Optional.fromNullable(cursorReader.getString(ARTWORK_URL)));
            return propertySet;
        }
    }

    private static final class ChartMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(ChartsProperty.LOCAL_ID, cursorReader.getLong(Charts._ID));
            propertySet.put(ChartsProperty.CHART_TYPE, ChartType.from(cursorReader.getString(Charts.TYPE)));
            propertySet.put(ChartsProperty.CHART_CATEGORY,
                            ChartCategory.from(cursorReader.getString(Charts.CATEGORY)));
            final String genre = cursorReader.getString(Charts.GENRE);
            propertySet.put(ChartsProperty.GENRE,
                            genre != null ? Optional.of(new Urn(genre)) : Optional.<Urn>absent());
            propertySet.put(ChartsProperty.TITLE, cursorReader.getString(Charts.TITLE));
            propertySet.put(ChartsProperty.PAGE, cursorReader.getString(Charts.PAGE));
            return propertySet;
        }
    }
}
