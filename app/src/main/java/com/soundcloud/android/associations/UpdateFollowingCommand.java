package com.soundcloud.android.associations;

import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.from;

import com.soundcloud.android.Consts;
import com.soundcloud.android.commands.WriteStorageCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Where;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Date;

class UpdateFollowingCommand extends WriteStorageCommand<UpdateFollowingCommand.UpdateFollowingParams, WriteResult, Integer> {

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
                step(propeller.update(Table.Users,
                        buildContentValuesForFollowersCount(),
                        buildWhereClauseForFollowersCount(params)));
                step(propeller.upsert(Table.UserAssociations, buildContentValuesForFollowing(params)));
            }
        });
    }

    @Override
    protected Integer transform(WriteResult result) {
        return updatedFollowersCount;
    }

    private int obtainNewFollowersCount(PropellerDatabase propeller, UpdateFollowingParams params) {
        int count = propeller.query(from(Table.Users.name())
                .select(TableColumns.Users.FOLLOWERS_COUNT)
                .whereEq(TableColumns.Users._ID, params.targetUrn.getNumericId()))
                .first(Integer.class);

        if (isFollowing(propeller, params.targetUrn) == params.following || count == Consts.NOT_SET) {
            return count;
        } else {
            return params.following ? count + 1 : count - 1;
        }
    }

    private boolean isFollowing(PropellerDatabase propeller, Urn targetUrn) {
        final QueryResult queryResult = propeller.query(from(Table.UserAssociations.name())
                .select(TableColumns.UserAssociations.TARGET_ID)
                .whereEq(TableColumns.UserAssociations.TARGET_ID, targetUrn.getNumericId())
                .whereEq(TableColumns.UserAssociations.RESOURCE_TYPE, TableColumns.UserAssociations.TYPE_RESOURCE_USER)
                .whereEq(TableColumns.UserAssociations.ASSOCIATION_TYPE, TableColumns.UserAssociations.TYPE_FOLLOWING)
                .whereNull(TableColumns.UserAssociations.REMOVED_AT));

        final int followingCount = queryResult.getResultCount();
        queryResult.release();

        return followingCount == 1;
    }

    private ContentValues buildContentValuesForFollowersCount() {
        return ContentValuesBuilder
                .values()
                .put(TableColumns.Users.FOLLOWERS_COUNT, updatedFollowersCount)
                .get();
    }

    private Where buildWhereClauseForFollowersCount(UpdateFollowingParams params) {
        return filter().whereEq(TableColumns.Users._ID, params.targetUrn.getNumericId());
    }

    private ContentValues buildContentValuesForFollowing(UpdateFollowingParams params) {
        final long now = new Date().getTime();
        final ContentValues cv = new ContentValues();

        cv.put(TableColumns.UserAssociations.TARGET_ID, params.targetUrn.getNumericId());
        cv.put(TableColumns.UserAssociations.ASSOCIATION_TYPE, TableColumns.UserAssociations.TYPE_FOLLOWING);
        cv.put(TableColumns.UserAssociations.RESOURCE_TYPE, TableColumns.UserAssociations.TYPE_RESOURCE_USER);
        cv.put(TableColumns.UserAssociations.CREATED_AT, now);

        if (params.following) {
            cv.put(TableColumns.UserAssociations.ADDED_AT, now);
            cv.putNull(TableColumns.UserAssociations.REMOVED_AT);
        } else {
            cv.put(TableColumns.UserAssociations.REMOVED_AT, now);
            cv.putNull(TableColumns.UserAssociations.ADDED_AT);
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

