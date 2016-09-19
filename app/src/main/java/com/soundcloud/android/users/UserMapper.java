package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public final class UserMapper extends RxResultMapper<PropertySet> {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(UserProperty.URN, Urn.forUser(cursorReader.getLong(Tables.UsersView.ID)));
        propertySet.put(UserProperty.USERNAME, cursorReader.getString(Tables.UsersView.USERNAME));
        propertySet.put(UserProperty.FOLLOWERS_COUNT, cursorReader.getInt(Tables.UsersView.FOLLOWERS_COUNT));
        propertySet.put(UserProperty.IS_FOLLOWED_BY_ME, cursorReader.getBoolean(Tables.UsersView.IS_FOLLOWING));
        propertySet.put(UserProperty.IMAGE_URL_TEMPLATE,
                        Optional.fromNullable(cursorReader.getString(Tables.UsersView.AVATAR_URL)));

        putOptionalFields(cursorReader, propertySet);

        return propertySet;
    }

    private void putOptionalFields(CursorReader cursorReader, PropertySet propertySet) {
        if (cursorReader.isNotNull(Tables.UsersView.COUNTRY)) {
            propertySet.put(UserProperty.COUNTRY, cursorReader.getString(Tables.UsersView.COUNTRY));
        }

        if (cursorReader.isNotNull(Tables.UsersView.CITY)) {
            propertySet.put(UserProperty.CITY, cursorReader.getString(Tables.UsersView.CITY));
        }

        if (cursorReader.isNotNull(Tables.UsersView.DESCRIPTION)) {
            propertySet.put(UserProperty.DESCRIPTION, cursorReader.getString(Tables.UsersView.DESCRIPTION));
        }

        if (cursorReader.isNotNull(Tables.UsersView.WEBSITE_URL)) {
            propertySet.put(UserProperty.WEBSITE_URL, cursorReader.getString(Tables.UsersView.WEBSITE_URL));
        }

        if (cursorReader.isNotNull(Tables.UsersView.WEBSITE_NAME)) {
            propertySet.put(UserProperty.WEBSITE_NAME, cursorReader.getString(Tables.UsersView.WEBSITE_NAME));
        }

        if (cursorReader.isNotNull(Tables.UsersView.DISCOGS_NAME)) {
            propertySet.put(UserProperty.DISCOGS_NAME, cursorReader.getString(Tables.UsersView.DISCOGS_NAME));
        }

        if (cursorReader.isNotNull(Tables.UsersView.MYSPACE_NAME)) {
            propertySet.put(UserProperty.MYSPACE_NAME, cursorReader.getString(Tables.UsersView.MYSPACE_NAME));
        }

        propertySet.put(UserProperty.ARTIST_STATION,
                        cursorReader.isNotNull(Tables.UsersView.ARTIST_STATION) ?
                        Optional.of(new Urn(cursorReader.getString(Tables.UsersView.ARTIST_STATION))) :
                        Optional.<Urn>absent());
    }
}
