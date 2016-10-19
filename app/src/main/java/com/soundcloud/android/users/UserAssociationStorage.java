package com.soundcloud.android.users;

import static com.soundcloud.android.storage.Table.UserAssociations;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.ADDED_AT;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.ASSOCIATION_TYPE;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.POSITION;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.REMOVED_AT;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.TARGET_ID;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.TYPE_FOLLOWER;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.TYPE_FOLLOWING;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.ASC;

import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.TableColumns.Users;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ScalarMapper;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.content.ContentValues;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserAssociationStorage {

    private static final String FOLLOWINGS_ALIAS = "Followings";
    private static final int BATCH_SIZE = 500;

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;
    private final StoreUsersCommand storeUsersCommand;

    @Inject
    public UserAssociationStorage(PropellerDatabase propeller, StoreUsersCommand storeUsersCommand) {
        this.propeller = propeller;
        this.storeUsersCommand = storeUsersCommand;
        this.propellerRx = new PropellerRx(propeller);
    }

    public void clear() {
        propeller.delete(UserAssociations);
    }

    public void deleteFollowingsById(List<Long> itemDeletions) {
        if (!itemDeletions.isEmpty()) {
            final List<List<Long>> batches = Lists.partition(itemDeletions, BATCH_SIZE);
            for (List<Long> idBatch : batches) {
                propeller.delete(UserAssociations, filter()
                        .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                        .whereIn(TARGET_ID, idBatch));
            }
        }
    }

    public TxnResult insertFollowedUsers(List<? extends UserRecord> followedUsers) {
        storeUsersCommand.call(followedUsers);
        final List<ContentValues> contentValuesList = new ArrayList<>(followedUsers.size());
        for (int position = 0; position < followedUsers.size(); position++) {
            final UserRecord user = followedUsers.get(position);
            if (user == null) {
                ErrorUtils.handleSilentException(new IllegalStateException("Null user in followings"));
                continue;
            }

            contentValuesList.add(followedUserContentValues(user.getUrn().getNumericId(), position));
        }
        return propeller.bulkInsert(UserAssociations, contentValuesList);
    }

    public void insertFollowedUserIds(List<Long> userIds, int startPosition) {
        int positionOffset = startPosition;
        List<List<Long>> batches = Lists.partition(userIds, BATCH_SIZE);
        for (List<Long> idBatch : batches) {
            List<ContentValues> contentValuesList = new ArrayList<>(idBatch.size());
            for (int i = 0; i < idBatch.size(); i++) {
                long userId = idBatch.get(i);
                contentValuesList.add(followedUserContentValues(userId, positionOffset + i));
            }
            positionOffset += idBatch.size();
            propeller.bulkInsert(UserAssociations, contentValuesList);
        }
    }

    private static ContentValues followedUserContentValues(long userId, int position) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(POSITION, position);
        contentValues.put(TARGET_ID, userId);
        contentValues.put(ASSOCIATION_TYPE, TYPE_FOLLOWING);
        return contentValues;
    }

    //TODO: this is business logic and belongs in the syncer. Let's move this once we migrate
    // affiliations to api-mobile and rewrite the syncer
    public void updateFollowingFromPendingState(Urn followedUser) {
        final PropertySet following = loadFollowedUser(followedUser);
        final Where filter = filter()
                .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                .whereEq(TARGET_ID, followedUser.getNumericId());
        if (following.contains(UserAssociationProperty.ADDED_AT)) {
            // following is pending addition; clear the timestamp
            final ContentValues contentValues = new ContentValues(1);
            contentValues.putNull(ADDED_AT);
            propeller.update(UserAssociations, contentValues, filter);
        } else if (following.contains(UserAssociationProperty.REMOVED_AT)) {
            // following is pending removal; delete it
            propeller.delete(UserAssociations, filter);
        }
    }

    public Set<Long> loadFollowedUserIds() {
        Query followingIds = Query.from(UserAssociations)
                                  .select(TARGET_ID)
                                  .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                                  .whereNull(ADDED_AT)
                                  .whereNull(REMOVED_AT);
        return new HashSet<>(propeller.query(followingIds).toList(ScalarMapper.create(Long.class)));
    }

    public Observable<List<PropertySet>> followers(int limit, long fromPosition) {
        final Query query = buildFollowersQuery(limit, fromPosition);
        return propellerRx.query(query).map(new FollowersMapper()).toList();
    }

    public Observable<List<PropertySet>> followedUsers(int limit, long fromPosition) {
        final Query query = buildFollowingsQuery(limit, fromPosition);
        return propellerRx.query(query).map(new FollowingsMapper()).toList();
    }

    public Observable<PropertySet> followedUser(Urn urn) {
        final Query query = buildFollowingsBaseQuery().whereEq(TARGET_ID, urn.getNumericId());
        return propellerRx.query(query).map(new FollowingsMapper());
    }

    private PropertySet loadFollowedUser(Urn urn) {
        final Query query = buildFollowingsBaseQuery().whereEq(TARGET_ID, urn.getNumericId());
        return propeller.query(query).firstOrDefault(new FollowingsMapper(), PropertySet.create());
    }

    public Observable<List<Urn>> followedUserUrns(int limit, long fromPosition) {
        Query query = Query.from(UserAssociations)
                           .select(field(TARGET_ID).as(BaseColumns._ID))
                           .whereEq(UserAssociations.field(ASSOCIATION_TYPE), TYPE_FOLLOWING)
                           .whereGt(UserAssociations.field(POSITION), fromPosition)
                           .order(UserAssociations.field(POSITION), ASC)
                           .limit(limit);

        return propellerRx.query(query).map(new UserUrnMapper()).toList();
    }

    public Observable<List<UserAssociation>> followedUserAssociations(int limit) {
        Query query = Query.from(UserAssociations)
                           .select(field(TARGET_ID).as(BaseColumns._ID),
                                   UserAssociations.field(ASSOCIATION_TYPE),
                                   UserAssociations.field(POSITION),
                                   UserAssociations.field(ADDED_AT),
                                   UserAssociations.field(REMOVED_AT)
                           )
                           .whereEq(UserAssociations.field(ASSOCIATION_TYPE), TYPE_FOLLOWING)
                           .whereNull(UserAssociations.field(REMOVED_AT))
                           .limit(limit);

        return propellerRx.query(query).map(new UserAssociationEntityMapper()).toList();
    }

    public boolean hasStaleFollowings() {
        return propeller.query(Query.count(UserAssociations)
                                    .where(staleFollowingsFilter()))
                        .firstOrDefault(Integer.class, 0) > 0;
    }

    public List<PropertySet> loadStaleFollowings() {
        return propeller.query(buildFollowingsBaseQuery()
                                       .where(staleFollowingsFilter()))
                        .toList(new FollowingsMapper());
    }

    private Where staleFollowingsFilter() {
        return filter()
                .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                .whereNotNull(ADDED_AT)
                .orWhereNotNull(REMOVED_AT);
    }

    protected Query buildFollowersQuery(int limit, long fromPosition) {
        return Query.from(Table.Users)
                    .select(
                            field(Users._ID),
                            field(Users.USERNAME),
                            field(Users.COUNTRY),
                            field(Users.FOLLOWERS_COUNT),
                            field(Users.AVATAR_URL),
                            UserAssociations.field(POSITION),
                            UserAssociations.field(ADDED_AT),
                            UserAssociations.field(REMOVED_AT),
                            FOLLOWINGS_ALIAS + "." + ASSOCIATION_TYPE)
                    .innerJoin(UserAssociations.name(),
                               filter()
                                       .whereEq(UserAssociations.field(TARGET_ID),
                                                Table.Users.field(TableColumns.Users._ID))
                                       .whereEq(UserAssociations.field(ASSOCIATION_TYPE), TYPE_FOLLOWER))
                    .leftJoin(UserAssociations.name() + " as " + FOLLOWINGS_ALIAS,
                              filter()
                                      .whereEq(FOLLOWINGS_ALIAS + "." + TARGET_ID,
                                               Table.Users.field(TableColumns.Users._ID))
                                      .whereEq(FOLLOWINGS_ALIAS + "." + ASSOCIATION_TYPE, TYPE_FOLLOWING))
                    .whereGt(UserAssociations.field(POSITION), fromPosition)
                    .order(UserAssociations.field(POSITION), ASC)
                    .limit(limit);
    }

    protected Query buildFollowingsQuery(int limit, long fromPosition) {
        return buildFollowingsBaseQuery()
                .whereGt(UserAssociations.field(POSITION), fromPosition)
                .order(UserAssociations.field(POSITION), ASC)
                .limit(limit);
    }

    @NonNull
    private Query buildFollowingsBaseQuery() {
        return Query.from(Table.Users)
                    .select(
                            field(Users._ID),
                            field(Users.USERNAME),
                            field(Users.COUNTRY),
                            field(Users.FOLLOWERS_COUNT),
                            field(Users.AVATAR_URL),
                            field(POSITION),
                            field(ADDED_AT),
                            field(REMOVED_AT))
                    .innerJoin(UserAssociations.name(),
                               filter()
                                       .whereEq(UserAssociations.field(TARGET_ID), Table.Users.field(Users._ID))
                                       .whereEq(UserAssociations.field(ASSOCIATION_TYPE), TYPE_FOLLOWING));
    }

    private class UserUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader reader) {
            return Urn.forUser(reader.getLong(BaseColumns._ID));
        }
    }

    private class FollowersMapper extends UserAssociationMapper {
        @Override
        public PropertySet map(CursorReader reader) {
            final PropertySet map = super.map(reader);
            map.put(UserProperty.IS_FOLLOWED_BY_ME, reader.isNotNull(FOLLOWINGS_ALIAS + "." + ASSOCIATION_TYPE));
            return map;
        }
    }

    private class FollowingsMapper extends UserAssociationMapper {
        @Override
        public PropertySet map(CursorReader reader) {
            final PropertySet map = super.map(reader);
            map.put(UserProperty.IS_FOLLOWED_BY_ME, true);
            return map;
        }
    }

    private class UserAssociationMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader reader) {
            final PropertySet propertySet = PropertySet.create(reader.getColumnCount() + 1);
            propertySet.put(UserProperty.URN, Urn.forUser(reader.getLong(BaseColumns._ID)));
            propertySet.put(UserProperty.USERNAME, reader.getString(Users.USERNAME));
            propertySet.put(UserProperty.IMAGE_URL_TEMPLATE, Optional.fromNullable(reader.getString(Users.AVATAR_URL)));
            if (reader.isNotNull(Users.COUNTRY)) {
                propertySet.put(UserProperty.COUNTRY, reader.getString(Users.COUNTRY));
            }
            propertySet.put(UserProperty.FOLLOWERS_COUNT, reader.getInt(Users.FOLLOWERS_COUNT));
            propertySet.put(UserAssociationProperty.POSITION, reader.getLong(POSITION));
            if (reader.isNotNull(ADDED_AT)) {
                propertySet.put(UserAssociationProperty.ADDED_AT, reader.getDateFromTimestamp(ADDED_AT));
            }
            if (reader.isNotNull(REMOVED_AT)) {
                propertySet.put(UserAssociationProperty.REMOVED_AT, reader.getDateFromTimestamp(REMOVED_AT));
            }
            return propertySet;
        }
    }

    private static class UserAssociationEntityMapper extends RxResultMapper<UserAssociation> {

        @Override
        public UserAssociation map(CursorReader reader) {
            return UserAssociation.create(reader);
        }
    }
}
