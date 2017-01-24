package com.soundcloud.android.activities;

import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.TableColumns.ActivityView;
import com.soundcloud.android.storage.TableColumns.SoundView;
import com.soundcloud.android.sync.timeline.TimelineStorage;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class ActivitiesStorage implements TimelineStorage<ActivityItem> {

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;

    @Inject
    public ActivitiesStorage(PropellerDatabase propeller, PropellerRx propellerRx) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
    }

    public Observable<Integer> timelineItemCountSince(final long timestamp) {
        Query query = Query.count(Table.ActivityView.name())
                           .whereGt((Table.ActivityView.field(TableColumns.ActivityView.CREATED_AT)), timestamp);

        return propellerRx.query(query).map(scalar(Integer.class));
    }

    @NonNull
    private static Query activitiesQuery(int limit) {
        return Query.from(Table.ActivityView)
                    .order(ActivityView.CREATED_AT, Query.Order.DESC)
                    .whereIn(ActivityView.TYPE, ActivityKind.SUPPORTED_IDENTIFIERS)
                    .limit(limit);
    }

    @Override
    public Observable<ActivityItem> timelineItems(final int limit) {
        return propellerRx.query(activitiesQuery(limit)).map(new ActivityRowMapper());
    }

    @Override
    public Observable<ActivityItem> timelineItemsBefore(long timestamp, int limit) {
        final Query query = activitiesQuery(limit)
                .whereLt(ActivityView.CREATED_AT, timestamp);
        return propellerRx.query(query).map(new ActivityRowMapper());
    }

    @Override
    public List<ActivityItem> timelineItemsSince(long timestamp, int limit) {
        final Query query = activitiesQuery(limit)
                .whereGt(ActivityView.CREATED_AT, timestamp);
        return propeller.query(query).toList(new ActivityRowMapper());
    }

    private static class ActivityRowMapper extends RxResultMapper<ActivityItem> {
        @Override
        public ActivityItem map(CursorReader reader) {
            final Date createdAt = reader.getDateFromTimestamp(ActivityView.CREATED_AT);
            final ActivityKind kind = ActivityKind.fromIdentifier(reader.getString(ActivityView.TYPE));
            final String userName = reader.getString(ActivityView.USER_USERNAME);
            final Urn userUrn = Urn.forUser(reader.getLong(ActivityView.USER_ID));

            final String title = ActivityKind.PLAYABLE_RELATED.contains(kind) ?
                                 reader.getString(SoundView.TITLE) :
                                 Strings.EMPTY;

            // we need to return the track URN for comments so that we can enter the comments screen
            final Optional<Urn> trackUrn = ActivityKind.TRACK_COMMENT.equals(kind) ?
                                           Optional.of(Urn.forTrack(reader.getLong(ActivityView.SOUND_ID))) :
                                           Optional.absent();


            return ActivityItem.create(createdAt, kind, userName, title, trackUrn, userUrn);
        }
    }
}
