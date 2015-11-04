package com.soundcloud.android.activities;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.ActivityView;
import com.soundcloud.android.storage.TableColumns.SoundView;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;

class ActivitiesStorage {

    private final PropellerRx propellerRx;

    @Inject
    public ActivitiesStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<PropertySet> initialActivityItems(final int limit) {
        final Query query = Query.from(Table.ActivityView)
                .order(ActivityView.CREATED_AT, Query.Order.DESC)
                .limit(limit);
        return propellerRx.query(query).map(new ActivityRowMapper());
    }

    private static class ActivityRowMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader reader) {
            final PropertySet propertySet = PropertySet.create(reader.getRowCount());
            propertySet.put(
                    ActivityProperty.KIND, ActivityKind.fromIdentifier(reader.getString(ActivityView.TYPE)));
            propertySet.put(
                    ActivityProperty.DATE, reader.getDateFromTimestamp(ActivityView.CREATED_AT));
            propertySet.put(
                    ActivityProperty.USER_URN, Urn.forUser(reader.getLong(ActivityView.USER_ID)));
            propertySet.put(
                    ActivityProperty.USER_NAME, reader.getString(ActivityView.USER_USERNAME));
            if (reader.isNotNull(ActivityView.SOUND_ID)) {
                propertySet.put(ActivityProperty.PLAYABLE_TITLE, reader.getString(SoundView.TITLE));
            }
            return propertySet;
        }
    }
}
