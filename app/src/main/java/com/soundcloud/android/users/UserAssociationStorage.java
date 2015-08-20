package com.soundcloud.android.users;

import static com.soundcloud.android.storage.TableColumns.SoundView.CREATED_AT;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Query.Order.DESC;

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

import javax.inject.Inject;
import java.util.List;

public class UserAssociationStorage {

    private static final String FOLLOWINGS_ALIAS = "Followings";

    private final PropellerRx propellerRx;

    @Inject
    public UserAssociationStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<List<PropertySet>> loadFollowers(int limit, long fromTimestamp){
        final Query query = buildFollowersQuery(limit, fromTimestamp);
        return propellerRx.query(query).map(new FollowersMapper()).toList();
    }

    public Observable<List<PropertySet>> loadFollowings(int limit, long fromTimestamp){
        final Query query = buildFollowingsQuery(limit, fromTimestamp);
        return propellerRx.query(query).map(new FollowingsMapper()).toList();
    }

    protected Query buildFollowersQuery(int limit, long fromTimestamp) {
        return Query.from(Table.Users)
                .select(
                        field(Users._ID),
                        field(Users.USERNAME),
                        field(Users.COUNTRY),
                        field(Users.FOLLOWERS_COUNT),
                        FOLLOWINGS_ALIAS + "." + UserAssociations.ASSOCIATION_TYPE)
                .innerJoin(Table.UserAssociations.name(),
                        Filter.filter()
                                .whereEq(Table.UserAssociations.field(UserAssociations.TARGET_ID), Table.Users.field(TableColumns.Users._ID))
                                .whereEq(Table.UserAssociations.field(UserAssociations.ASSOCIATION_TYPE), UserAssociations.TYPE_FOLLOWER))
                .leftJoin(Table.UserAssociations.name() + " as " + FOLLOWINGS_ALIAS,
                        Filter.filter()
                                .whereEq(FOLLOWINGS_ALIAS + "." + UserAssociations.TARGET_ID, Table.Users.field(TableColumns.Users._ID))
                                .whereEq(FOLLOWINGS_ALIAS + "." + UserAssociations.ASSOCIATION_TYPE, UserAssociations.TYPE_FOLLOWING))
                .whereLt(Table.UserAssociations.field(UserAssociations.CREATED_AT), fromTimestamp)
                .order(Table.UserAssociations.field(UserAssociations.CREATED_AT), DESC)
                .limit(limit);
    }

    protected Query buildFollowingsQuery(int limit, long fromTimestamp) {
        return Query.from(Table.Users)
                .select(
                        field(Users._ID),
                        field(Users.USERNAME),
                        field(Users.COUNTRY),
                        field(Users.FOLLOWERS_COUNT))
                .innerJoin(Table.UserAssociations.name(),
                        Filter.filter()
                                .whereEq(Table.UserAssociations.field(UserAssociations.TARGET_ID), Table.Users.field(TableColumns.Users._ID))
                                .whereEq(Table.UserAssociations.field(UserAssociations.ASSOCIATION_TYPE), UserAssociations.TYPE_FOLLOWING))
                .whereLt(Table.UserAssociations.field(UserAssociations.CREATED_AT), fromTimestamp)
                .order(Table.UserAssociations.field(CREATED_AT), DESC)
                .limit(limit);
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
            propertySet.put(UserProperty.COUNTRY, reader.getString(Users.COUNTRY));
            propertySet.put(UserProperty.FOLLOWERS_COUNT, reader.getInt(Users.FOLLOWERS_COUNT));
            return propertySet;
        }
    }
}
