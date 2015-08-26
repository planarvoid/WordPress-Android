package com.soundcloud.android.users;

import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Query.Order.ASC;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.TableColumns.UserAssociations;
import com.soundcloud.android.storage.TableColumns.Users;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.List;

public class UserAssociationStorage {

    private static final String FOLLOWINGS_ALIAS = "Followings";

    private final PropellerRx propellerRx;

    @Inject
    public UserAssociationStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<List<PropertySet>> loadFollowers(int limit, long fromPosition) {
        final Query query = buildFollowersQuery(limit, fromPosition);
        return propellerRx.query(query).map(new FollowersMapper()).toList();
    }

    public Observable<List<PropertySet>> loadFollowings(int limit, long fromPosition) {
        final Query query = buildFollowingsQuery(limit, fromPosition);
        return propellerRx.query(query).map(new FollowingsMapper()).toList();
    }

    public Observable<PropertySet> loadFollowing(Urn urn) {
        final Query query = buildFollowingsBaseQuery().whereEq(UserAssociations.TARGET_ID, urn.getNumericId());
        return propellerRx.query(query).map(new FollowingsMapper());
    }


    public Observable<List<Urn>> loadFollowingsUrns(int limit, long fromPosition) {
        Query query = Query.from(Table.UserAssociations)
                .select(field(UserAssociations.TARGET_ID).as(BaseColumns._ID))
                .whereEq(Table.UserAssociations.field(UserAssociations.ASSOCIATION_TYPE), UserAssociations.TYPE_FOLLOWING)
                .whereGt(Table.UserAssociations.field(UserAssociations.POSITION), fromPosition)
                .order(Table.UserAssociations.field(UserAssociations.POSITION), ASC)
                .limit(limit);

        return propellerRx.query(query).map(new UserUrnMapper()).toList();
    }

    protected Query buildFollowersQuery(int limit, long fromPosition) {
        return Query.from(Table.Users)
                .select(
                        field(Users._ID),
                        field(Users.USERNAME),
                        field(Users.COUNTRY),
                        field(Users.FOLLOWERS_COUNT),
                        Table.UserAssociations.field(UserAssociations.POSITION),
                        FOLLOWINGS_ALIAS + "." + UserAssociations.ASSOCIATION_TYPE)
                .innerJoin(Table.UserAssociations.name(),
                        Filter.filter()
                                .whereEq(Table.UserAssociations.field(UserAssociations.TARGET_ID), Table.Users.field(TableColumns.Users._ID))
                                .whereEq(Table.UserAssociations.field(UserAssociations.ASSOCIATION_TYPE), UserAssociations.TYPE_FOLLOWER))
                .leftJoin(Table.UserAssociations.name() + " as " + FOLLOWINGS_ALIAS,
                        Filter.filter()
                                .whereEq(FOLLOWINGS_ALIAS + "." + UserAssociations.TARGET_ID, Table.Users.field(TableColumns.Users._ID))
                                .whereEq(FOLLOWINGS_ALIAS + "." + UserAssociations.ASSOCIATION_TYPE, UserAssociations.TYPE_FOLLOWING))
                .whereGt(Table.UserAssociations.field(UserAssociations.POSITION), fromPosition)
                .order(Table.UserAssociations.field(UserAssociations.POSITION), ASC)
                .limit(limit);
    }

    protected Query buildFollowingsQuery(int limit, long fromPosition) {
        return buildFollowingsBaseQuery()
                .whereGt(Table.UserAssociations.field(UserAssociations.POSITION), fromPosition)
                .order(Table.UserAssociations.field(UserAssociations.POSITION), ASC)
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
                        field(UserAssociations.POSITION))
                .innerJoin(Table.UserAssociations.name(),
                        Filter.filter()
                                .whereEq(Table.UserAssociations.field(UserAssociations.TARGET_ID), Table.Users.field(Users._ID))
                                .whereEq(Table.UserAssociations.field(UserAssociations.ASSOCIATION_TYPE), UserAssociations.TYPE_FOLLOWING));
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
            map.put(UserProperty.IS_FOLLOWED_BY_ME, reader.isNotNull(FOLLOWINGS_ALIAS + "." + UserAssociations.ASSOCIATION_TYPE));
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
            if (reader.isNotNull(Users.COUNTRY)) {
                propertySet.put(UserProperty.COUNTRY, reader.getString(Users.COUNTRY));
            }
            propertySet.put(UserProperty.FOLLOWERS_COUNT, reader.getInt(Users.FOLLOWERS_COUNT));
            propertySet.put(UserAssociationProperty.POSITION, reader.getLong(UserAssociations.POSITION));
            return propertySet;
        }
    }
}
