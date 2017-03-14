package com.soundcloud.android.users;

import static com.soundcloud.android.storage.Tables.UserAssociations.ADDED_AT;
import static com.soundcloud.android.storage.Tables.UserAssociations.ASSOCIATION_TYPE;
import static com.soundcloud.android.storage.Tables.UserAssociations.POSITION;
import static com.soundcloud.android.storage.Tables.UserAssociations.REMOVED_AT;
import static com.soundcloud.android.storage.Tables.UserAssociations.TARGET_ID;
import static com.soundcloud.android.storage.Tables.UserAssociations.TYPE_FOLLOWING;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.ASC;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.Following;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.UserAssociations;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ScalarMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import com.soundcloud.propeller.schema.BulkInsertValues;
import rx.Observable;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserAssociationStorage {

    private static final int BATCH_SIZE = 500;

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;

    @Inject
    public UserAssociationStorage(PropellerDatabase propeller) {
        this.propeller = propeller;
        this.propellerRx = new PropellerRx(propeller);
    }

    public void clear() {
        propeller.delete(UserAssociations.TABLE);
    }

    public void deleteFollowingsById(List<Long> itemDeletions) {
        if (!itemDeletions.isEmpty()) {
            final List<List<Long>> batches = Lists.partition(itemDeletions, BATCH_SIZE);
            for (List<Long> idBatch : batches) {
                propeller.delete(UserAssociations.TABLE, filter()
                        .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                        .whereIn(TARGET_ID, idBatch));
            }
        }
    }

    public void insertFollowedUserIds(List<Long> userIds) {
        int positionOffset = 0;
        List<List<Long>> batches = Lists.partition(userIds, BATCH_SIZE);
        for (List<Long> idBatch : batches) {
            BulkInsertValues.Builder builder = new BulkInsertValues.Builder(
                    Arrays.asList(POSITION, TARGET_ID, ASSOCIATION_TYPE)
            );

            for (int i = 0; i < idBatch.size(); i++) {
                long userId = idBatch.get(i);
                builder.addRow(Arrays.asList(positionOffset + i, userId, TYPE_FOLLOWING));
            }
            propeller.bulkInsert(UserAssociations.TABLE, builder.build());
            positionOffset += idBatch.size();
        }
    }

    //TODO: this is business logic and belongs in the syncer. Let's move this once we migrate
    // affiliations to api-mobile and rewrite the syncer
    public void updateFollowingFromPendingState(Urn followedUser) {
        final Optional<UserAssociation> following = loadFollowedUser(followedUser);
        final Where filter = filter()
                .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                .whereEq(TARGET_ID, followedUser.getNumericId());
        if (following.isPresent() && following.get().addedAt().isPresent()) {
            // following is pending addition; clear the timestamp
            final ContentValues contentValues = new ContentValues(1);
            contentValues.putNull(ADDED_AT.name());
            propeller.update(UserAssociations.TABLE, contentValues, filter);
        } else if (following.isPresent() && following.get().removedAt().isPresent()) {
            // following is pending removal; delete it
            propeller.delete(UserAssociations.TABLE, filter);
        }
    }

    public Set<Long> loadFollowedUserIds() {
        Query followingIds = Query.from(UserAssociations.TABLE)
                                  .select(TARGET_ID)
                                  .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                                  .whereNull(ADDED_AT)
                                  .whereNull(REMOVED_AT);
        return new HashSet<>(propeller.query(followingIds).toList(ScalarMapper.create(Long.class)));
    }

    public Observable<List<Following>> followedUsers(int limit, long fromPosition) {
        final Query query = buildFollowingsQuery(limit, fromPosition);
        return propellerRx.query(query).map(new UserAssociationMapper()).toList();
    }

    public Observable<User> followedUser(Urn urn) {
        final Query query = buildFollowingsBaseQuery().whereEq(TARGET_ID, urn.getNumericId());
        return propellerRx.query(query).map(new UserAssociationMapper()).map(Following::user);
    }

    private Optional<UserAssociation> loadFollowedUser(Urn urn) {
        final Query query = buildFollowingsBaseQuery().whereEq(TARGET_ID, urn.getNumericId());
        return Optional.fromNullable(propeller.query(query).firstOrDefault(new UserAssociationMapper(), null)).transform(Following::userAssociation);
    }

    public Observable<List<Urn>> followedUserUrns(int limit, long fromPosition) {
        Query query = Query.from(UserAssociations.TABLE)
                           .select(TARGET_ID)
                           .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                           .whereGt(POSITION, fromPosition)
                           .order(POSITION, ASC)
                           .limit(limit);

        return propellerRx.query(query).map(new UserUrnMapper()).toList();
    }

    public Observable<List<UserAssociation>> followedUserAssociations() {
        Query query = Query.from(UserAssociations.TABLE)
                           .select(TARGET_ID,
                                   ASSOCIATION_TYPE,
                                   POSITION,
                                   ADDED_AT,
                                   REMOVED_AT
                           )
                           .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                           .whereNull(REMOVED_AT);
        return propellerRx.query(query).map(new UserAssociationEntityMapper()).toList();
    }

    public boolean hasStaleFollowings() {
        return propeller.query(Query.count(UserAssociations.TABLE)
                                    .where(staleFollowingsFilter()))
                        .firstOrDefault(Integer.class, 0) > 0;
    }

    public List<Following> loadStaleFollowings() {
        return propeller.query(buildFollowingsBaseQuery()
                                       .where(staleFollowingsFilter()))
                        .toList(new UserAssociationMapper());
    }

    private Where staleFollowingsFilter() {
        return filter()
                .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                .whereNotNull(ADDED_AT)
                .orWhereNotNull(REMOVED_AT);
    }

    private Query buildFollowingsQuery(int limit, long fromPosition) {
        return buildFollowingsBaseQuery()
                .whereGt(POSITION, fromPosition)
                .order(POSITION, ASC)
                .limit(limit);
    }

    @NonNull
    private Query buildFollowingsBaseQuery() {
        return Query.from(Tables.UsersView.TABLE)
                    .select("UsersView.*",
                            TARGET_ID,
                            POSITION,
                            ADDED_AT,
                            REMOVED_AT,
                            ASSOCIATION_TYPE)
                    .innerJoin(UserAssociations.TABLE,
                               filter()
                                       .whereEq(TARGET_ID, Tables.UsersView.ID)
                                       .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING));
    }

    private class UserUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader reader) {
            return Urn.forUser(reader.getLong(TARGET_ID));
        }
    }

    private class UserAssociationMapper extends RxResultMapper<Following> {
        @Override
        public Following map(CursorReader reader) {
            User user = User.fromCursorReader(reader);
            return Following.from(user, UserAssociation.create(reader));
        }
    }

    private static class UserAssociationEntityMapper extends RxResultMapper<UserAssociation> {

        @Override
        public UserAssociation map(CursorReader reader) {
            return UserAssociation.create(reader);
        }
    }
}
