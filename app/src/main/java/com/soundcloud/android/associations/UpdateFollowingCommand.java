package com.soundcloud.android.associations;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Date;

class UpdateFollowingCommand
        extends WriteStorageCommand<UpdateFollowingCommand.UpdateFollowingParams, WriteResult, Integer> {

    private int updatedFollowersCount;

    @Inject
    UpdateFollowingCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final UpdateFollowingParams params) {
        updatedFollowersCount = obtainNewFollowersCount(propeller, params);

        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.update(Tables.Users.TABLE,
                                      buildContentValuesForFollowersCount(),
                                      buildWhereClauseForFollowersCount(params)));
                step(propeller.upsert(Tables.UserAssociations.TABLE, buildContentValuesForFollowing(params)));
            }
        });
    }

    @Override
    protected Integer transform(WriteResult result) {
        return updatedFollowersCount;
    }

    private int obtainNewFollowersCount(PropellerDatabase propeller, UpdateFollowingParams params) {
        int count = propeller.query(Query.from(Tables.Users.TABLE)
                                         .select(Tables.Users.FOLLOWERS_COUNT)
                                         .whereEq(Tables.Users._ID, params.targetUrn.getNumericId()))
                             .first(Integer.class);

        if (isFollowing(propeller, params.targetUrn) == params.following || count == Consts.NOT_SET) {
            return count;
        } else {
            return params.following ? count + 1 : count - 1;
        }
    }

    private boolean isFollowing(PropellerDatabase propeller, Urn targetUrn) {
        final QueryResult queryResult = propeller.query(Query.from(Tables.UserAssociations.TABLE)
                                                                .select(Tables.UserAssociations.TARGET_ID)
                                                                .whereEq(Tables.UserAssociations.TARGET_ID,
                                                                         targetUrn.getNumericId())
                                                                .whereEq(Tables.UserAssociations.RESOURCE_TYPE,
                                                                         Tables.UserAssociations.TYPE_RESOURCE_USER)
                                                                .whereEq(Tables.UserAssociations.ASSOCIATION_TYPE,
                                                                         Tables.UserAssociations.TYPE_FOLLOWING)
                                                                .whereNull(Tables.UserAssociations.REMOVED_AT));

        final int followingCount = queryResult.getResultCount();
        queryResult.release();

        return followingCount == 1;
    }

    private ContentValues buildContentValuesForFollowersCount() {
        return ContentValuesBuilder
                .values()
                .put(Tables.Users.FOLLOWERS_COUNT, updatedFollowersCount)
                .get();
    }

    private Where buildWhereClauseForFollowersCount(UpdateFollowingParams params) {
        return filter().whereEq(Tables.Users._ID, params.targetUrn.getNumericId());
    }

    private ContentValues buildContentValuesForFollowing(UpdateFollowingParams params) {
        final long now = new Date().getTime();
        final ContentValues cv = new ContentValues();

        cv.put(Tables.UserAssociations.TARGET_ID.name(), params.targetUrn.getNumericId());
        cv.put(Tables.UserAssociations.ASSOCIATION_TYPE.name(), Tables.UserAssociations.TYPE_FOLLOWING);
        cv.put(Tables.UserAssociations.RESOURCE_TYPE.name(), Tables.UserAssociations.TYPE_RESOURCE_USER);
        cv.put(Tables.UserAssociations.CREATED_AT.name(), now);

        if (params.following) {
            cv.put(Tables.UserAssociations.ADDED_AT.name(), now);
            cv.putNull(Tables.UserAssociations.REMOVED_AT.name());
        } else {
            cv.put(Tables.UserAssociations.REMOVED_AT.name(), now);
            cv.putNull(Tables.UserAssociations.ADDED_AT.name());
        }

        return cv;
    }

    static final class UpdateFollowingParams {
        final Urn targetUrn;
        final boolean following;

        UpdateFollowingParams(Urn targetUrn, boolean following) {
            this.targetUrn = targetUrn;
            this.following = following;
        }
    }
}

