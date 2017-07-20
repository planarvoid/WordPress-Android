package com.soundcloud.android.activities;

import static com.soundcloud.propeller.rx.RxResultMapperV2.scalar;

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
import com.soundcloud.propeller.rx.PropellerRxV2;
import com.soundcloud.propeller.rx.RxResultMapperV2;
import io.reactivex.Observable;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class ActivitiesStorage implements TimelineStorage<ActivityItem> {

    private final PropellerDatabase propeller;
    private final PropellerRxV2 propellerRx;

    @Inject
    public ActivitiesStorage(PropellerDatabase propeller, PropellerRxV2 propellerRx) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
    }

    public Observable<Integer> timelineItemCountSince(final long timestamp) {
        Query query = Query.count(Table.ActivityView.name())
                           .whereGt((Table.ActivityView.field(TableColumns.ActivityView.CREATED_AT)), timestamp);

        return propellerRx.queryResult(query)
                          .map(result -> result.toList(scalar(Integer.class)))
                          .flatMap(Observable::fromIterable);
    }

    @NonNull
    private static Query activitiesQuery(int limit) {
        return Query.from(Table.ActivityView)
                    .order(ActivityView.CREATED_AT, Query.Order.DESC)
                    .whereIn(ActivityView.TYPE, (Object[]) ActivityKind.SUPPORTED_IDENTIFIERS)
                    .limit(limit);
    }

    @Override
    public Observable<ActivityItem> timelineItems(final int limit) {
        return propellerRx.queryResult(activitiesQuery(limit))
                          .map(result -> result.toList(new ActivityRowMapper()))
                          .flatMap(Observable::fromIterable);
    }

    @Override
    public Observable<ActivityItem> timelineItemsBefore(long timestamp, int limit) {
        final Query query = activitiesQuery(limit)
                .whereLt(ActivityView.CREATED_AT, timestamp);
        return propellerRx.queryResult(query)
                          .map(result -> result.toList(new ActivityRowMapper()))
                          .flatMap(Observable::fromIterable);
    }

    @Override
    public List<ActivityItem> timelineItemsSince(long timestamp, int limit) {
        final Query query = activitiesQuery(limit)
                .whereGt(ActivityView.CREATED_AT, timestamp);
        return propeller.query(query).toList(new ActivityRowMapper());
    }

    private static class ActivityRowMapper extends RxResultMapperV2<ActivityItem> {
        @Override
        public ActivityItem map(CursorReader reader) {
            final Date createdAt = reader.getDateFromTimestamp(ActivityView.CREATED_AT);
            final ActivityKind kind = ActivityKind.fromIdentifier(reader.getString(ActivityView.TYPE));
            final String userName = reader.getString(ActivityView.USER_USERNAME);
            final Urn userUrn = Urn.forUser(reader.getLong(ActivityView.USER_ID));
            final String avatarUrl = reader.getString(ActivityView.USER_AVATAR_URL);
            final Optional<String> imageUrlTemplate = Optional.fromNullable(avatarUrl);
            final String title = ActivityKind.PLAYABLE_RELATED.contains(kind) ?
                                 reader.getString(SoundView.TITLE) :
                                 Strings.EMPTY;

            // we need to return the track URN for comments so that we can enter the comments screen
            final Optional<Urn> trackUrn = ActivityKind.TRACK_COMMENT.equals(kind) ?
                                           Optional.of(Urn.forTrack(reader.getLong(ActivityView.SOUND_ID))) :
                                           Optional.absent();

            return ActivityItem.create(createdAt, kind, userName, title, trackUrn, userUrn,
                                       imageUrlTemplate);
        }
    }
}
