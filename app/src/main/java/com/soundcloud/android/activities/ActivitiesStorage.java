package com.soundcloud.android.activities;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.ActivityView;
import com.soundcloud.android.storage.TableColumns.SoundView;
import com.soundcloud.android.sync.timeline.TimelineStorage;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

public class ActivitiesStorage implements TimelineStorage {

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;

    @Inject
    public ActivitiesStorage(PropellerDatabase propeller, PropellerRx propellerRx) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
    }

    @NonNull
    private static Query activitiesQuery(int limit) {
        return Query.from(Table.ActivityView)
                    .order(ActivityView.CREATED_AT, Query.Order.DESC)
                    .whereIn(ActivityView.TYPE, ActivityKind.SUPPORTED_IDENTIFIERS)
                    .limit(limit);
    }

    @Override
    public Observable<PropertySet> timelineItems(final int limit) {
        return propellerRx.query(activitiesQuery(limit)).map(new ActivityRowMapper());
    }

    @Override
    public Observable<PropertySet> timelineItemsBefore(long timestamp, int limit) {
        final Query query = activitiesQuery(limit)
                .whereLt(ActivityView.CREATED_AT, timestamp);
        return propellerRx.query(query).map(new ActivityRowMapper());
    }

    @Override
    public List<PropertySet> timelineItemsSince(long timestamp, int limit) {
        final Query query = activitiesQuery(limit)
                .whereGt(ActivityView.CREATED_AT, timestamp);
        return propeller.query(query).toList(new ActivityRowMapper());
    }

    private static class ActivityRowMapper extends RxResultMapper<PropertySet> {
        @Override
        public PropertySet map(CursorReader reader) {
            final PropertySet propertySet = PropertySet.create(reader.getRowCount());
            final ActivityKind activityKind = ActivityKind.fromIdentifier(reader.getString(ActivityView.TYPE));
            propertySet.put(ActivityProperty.KIND, activityKind);
            propertySet.put(ActivityProperty.DATE, reader.getDateFromTimestamp(ActivityView.CREATED_AT));
            propertySet.put(ActivityProperty.USER_URN, Urn.forUser(reader.getLong(ActivityView.USER_ID)));
            propertySet.put(ActivityProperty.USER_NAME, reader.getString(ActivityView.USER_USERNAME));

            if (ActivityKind.PLAYABLE_RELATED.contains(activityKind)) {
                propertySet.put(ActivityProperty.PLAYABLE_TITLE, reader.getString(SoundView.TITLE));
            }
            // we need to return the track URN for comments so that we can enter the comments screen
            if (ActivityKind.TRACK_COMMENT.equals(activityKind)) {
                final Urn trackUrn = Urn.forTrack(reader.getLong(ActivityView.SOUND_ID));
                propertySet.put(ActivityProperty.COMMENTED_TRACK_URN, trackUrn);
            }
            return propertySet;
        }
    }
}
