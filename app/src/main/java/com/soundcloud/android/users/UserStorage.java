package com.soundcloud.android.users;

import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;

public class UserStorage {

    private static final String IS_FOLLOWING = "is_following";

    private final PropellerRx propeller;

    @Inject
    UserStorage(PropellerRx propeller) {
        this.propeller = propeller;
    }

    Observable<PropertySet> loadUser(Urn urn) {
        return propeller.query(buildUserQuery(urn))
                .map(new UserMapper())
                .firstOrDefault(PropertySet.create());
    }

    private Query buildUserQuery(Urn userUrn) {
        return Query.from(Table.Users.name())
                .select(
                        TableColumns.Users._ID,
                        TableColumns.Users.USERNAME,
                        TableColumns.Users.COUNTRY,
                        TableColumns.Users.FOLLOWERS_COUNT,
                        TableColumns.Users.DESCRIPTION,
                        TableColumns.Users.AVATAR_URL,
                        TableColumns.Users.WEBSITE_URL,
                        TableColumns.Users.WEBSITE_NAME,
                        TableColumns.Users.MYSPACE_NAME,
                        TableColumns.Users.DISCOGS_NAME,
                        exists(followingQuery(userUrn)).as(IS_FOLLOWING)
                )
                .whereEq(TableColumns.SoundView._ID, userUrn.getNumericId());
    }

    private Query followingQuery(Urn userUrn) {
        return Query.from(Table.UserAssociations.name())
                .whereEq(Table.UserAssociations.field(TableColumns.UserAssociations.TARGET_ID), userUrn.getNumericId())
                .whereEq(Table.UserAssociations.field(TableColumns.UserAssociations.ASSOCIATION_TYPE), TableColumns.UserAssociations.TYPE_FOLLOWING)
                        .whereNull(TableColumns.UserAssociations.REMOVED_AT);
    }

    final class UserMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(UserProperty.URN, Urn.forUser(cursorReader.getLong(TableColumns.Users._ID)));
            propertySet.put(UserProperty.USERNAME, cursorReader.getString(TableColumns.Users.USERNAME));
            propertySet.put(UserProperty.FOLLOWERS_COUNT, cursorReader.getInt(TableColumns.Users.FOLLOWERS_COUNT));
            propertySet.put(UserProperty.IS_FOLLOWED_BY_ME, cursorReader.getBoolean(IS_FOLLOWING));
            propertySet.put(UserProperty.IMAGE_URL_TEMPLATE, Optional.fromNullable(
                    cursorReader.getString(TableColumns.Users.AVATAR_URL)));
            putOptionalFields(cursorReader, propertySet);

            return propertySet;
        }

        private void putOptionalFields(CursorReader cursorReader, PropertySet propertySet) {
            if (cursorReader.isNotNull(TableColumns.Users.COUNTRY)){
                propertySet.put(UserProperty.COUNTRY, cursorReader.getString(TableColumns.Users.COUNTRY));
            }

            if (cursorReader.isNotNull(TableColumns.Users.DESCRIPTION)){
                propertySet.put(UserProperty.DESCRIPTION, cursorReader.getString(TableColumns.Users.DESCRIPTION));
            }

            if (cursorReader.isNotNull(TableColumns.Users.WEBSITE_URL)){
                propertySet.put(UserProperty.WEBSITE_URL, cursorReader.getString(TableColumns.Users.WEBSITE_URL));
            }

            if (cursorReader.isNotNull(TableColumns.Users.WEBSITE_NAME)){
                propertySet.put(UserProperty.WEBSITE_NAME, cursorReader.getString(TableColumns.Users.WEBSITE_NAME));
            }

            if (cursorReader.isNotNull(TableColumns.Users.DISCOGS_NAME)){
                propertySet.put(UserProperty.DISCOGS_NAME, cursorReader.getString(TableColumns.Users.DISCOGS_NAME));
            }

            if (cursorReader.isNotNull(TableColumns.Users.MYSPACE_NAME)){
                propertySet.put(UserProperty.MYSPACE_NAME, cursorReader.getString(TableColumns.Users.MYSPACE_NAME));
            }
        }
    }
}
