package com.soundcloud.android.suggestedcreators;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.users.User;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRxV2;
import com.soundcloud.propeller.rx.RxResultMapperV2;
import io.reactivex.Completable;
import io.reactivex.Single;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class SuggestedCreatorsStorage {
    private final PropellerRxV2 propellerRx;
    private final PropellerDatabase propeller;
    private final DateProvider dateProvider;

    @Inject
    SuggestedCreatorsStorage(PropellerRxV2 propellerRx, PropellerDatabase propeller, CurrentDateProvider dateProvider) {
        this.propellerRx = propellerRx;
        this.propeller = propeller;
        this.dateProvider = dateProvider;
    }

    Single<List<SuggestedCreator>> suggestedCreators() {
        return propellerRx.queryResult(buildSuggestedCreatorsQuery()).map(result -> result.toList(new SuggestedCreatorMapper())).singleOrError();
    }

    Completable toggleFollowSuggestedCreator(Urn urn, boolean isFollowing) {
        return propellerRx.update(Tables.SuggestedCreators.TABLE,
                                  buildContentValuesForFollowingCreator(isFollowing),
                                  filter().whereEq(Tables.SuggestedCreators.SUGGESTED_USER_ID, urn.getNumericId()))
                          .map(changeResult -> changeResult.getNumRowsAffected() > 0)
                          .first(false)
                          .toCompletable();
    }

    public void clear() {
        propeller.delete(Tables.SuggestedCreators.TABLE);
    }

    private Query buildSuggestedCreatorsQuery() {
        return Query.from(Tables.SuggestedCreators.TABLE)
                    .leftJoin(Tables.UsersView.TABLE, Tables.SuggestedCreators.SEED_USER_ID, Tables.UsersView.ID)
                    .select("*");
    }

    private ContentValues buildContentValuesForFollowingCreator(boolean isFollowing) {
        final ContentValues values = new ContentValues(1);
        if (isFollowing) {
            values.put(Tables.SuggestedCreators.FOLLOWED_AT.name(), dateProvider.getCurrentTime());
        } else {
            values.putNull(Tables.SuggestedCreators.FOLLOWED_AT.name());
        }
        return values;
    }

    private class SuggestedCreatorMapper extends RxResultMapperV2<SuggestedCreator> {

        @Override
        public SuggestedCreator map(CursorReader reader) {
            final User userRecord = User.fromCursorReader(reader);
            final String relation = reader.getString(Tables.SuggestedCreators.RELATION_KEY.name());
            Optional<Date> followedAt = Optional.absent();
            if (reader.isNotNull(Tables.SuggestedCreators.FOLLOWED_AT.name())) {
                followedAt = Optional.of(reader.getDateFromTimestamp(Tables.SuggestedCreators.FOLLOWED_AT.name()));
            }
            return SuggestedCreator.create(userRecord, SuggestedCreatorRelation.from(relation), followedAt);
        }
    }
}
