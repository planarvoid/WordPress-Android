package com.soundcloud.android.users;

import static com.soundcloud.android.storage.TableColumns.SoundView;
import static com.soundcloud.android.storage.TableColumns.UserAssociations;
import static com.soundcloud.android.storage.TableColumns.Users;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
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
                            Users._ID,
                            Users.USERNAME,
                            Users.COUNTRY,
                            Users.FOLLOWERS_COUNT,
                            Users.DESCRIPTION,
                            Users.AVATAR_URL,
                            Users.WEBSITE_URL,
                            Users.WEBSITE_NAME,
                            Users.MYSPACE_NAME,
                            Users.DISCOGS_NAME,
                            Users.ARTIST_STATION,
                            exists(followingQuery(userUrn)).as(IS_FOLLOWING)
                    )
                    .whereEq(SoundView._ID, userUrn.getNumericId());
    }

    private Query followingQuery(Urn userUrn) {
        return Query.from(Table.UserAssociations.name())
                    .whereEq(Table.UserAssociations.field(UserAssociations.TARGET_ID), userUrn.getNumericId())
                    .whereEq(Table.UserAssociations.field(UserAssociations.ASSOCIATION_TYPE),
                             UserAssociations.TYPE_FOLLOWING)
                    .whereNull(UserAssociations.REMOVED_AT);
    }

    private final class UserMapper extends RxResultMapper<PropertySet> {

        @Override
        public PropertySet map(CursorReader cursorReader) {
            final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(UserProperty.URN, Urn.forUser(cursorReader.getLong(Users._ID)));
            propertySet.put(UserProperty.USERNAME, cursorReader.getString(Users.USERNAME));
            propertySet.put(UserProperty.FOLLOWERS_COUNT, cursorReader.getInt(Users.FOLLOWERS_COUNT));
            propertySet.put(UserProperty.IS_FOLLOWED_BY_ME, cursorReader.getBoolean(IS_FOLLOWING));
            propertySet.put(UserProperty.IMAGE_URL_TEMPLATE,
                            Optional.fromNullable(cursorReader.getString(Users.AVATAR_URL)));

            putOptionalFields(cursorReader, propertySet);

            return propertySet;
        }

        private void putOptionalFields(CursorReader cursorReader, PropertySet propertySet) {
            if (cursorReader.isNotNull(Users.COUNTRY)) {
                propertySet.put(UserProperty.COUNTRY, cursorReader.getString(Users.COUNTRY));
            }

            if (cursorReader.isNotNull(Users.DESCRIPTION)) {
                propertySet.put(UserProperty.DESCRIPTION, cursorReader.getString(Users.DESCRIPTION));
            }

            if (cursorReader.isNotNull(Users.WEBSITE_URL)) {
                propertySet.put(UserProperty.WEBSITE_URL, cursorReader.getString(Users.WEBSITE_URL));
            }

            if (cursorReader.isNotNull(Users.WEBSITE_NAME)) {
                propertySet.put(UserProperty.WEBSITE_NAME, cursorReader.getString(Users.WEBSITE_NAME));
            }

            if (cursorReader.isNotNull(Users.DISCOGS_NAME)) {
                propertySet.put(UserProperty.DISCOGS_NAME, cursorReader.getString(Users.DISCOGS_NAME));
            }

            if (cursorReader.isNotNull(Users.MYSPACE_NAME)) {
                propertySet.put(UserProperty.MYSPACE_NAME, cursorReader.getString(Users.MYSPACE_NAME));
            }

            propertySet.put(UserProperty.ARTIST_STATION,
                            cursorReader.isNotNull(Users.ARTIST_STATION) ?
                            Optional.of(new Urn(cursorReader.getString(Users.ARTIST_STATION))) :
                            Optional.<Urn>absent());
        }
    }
}
