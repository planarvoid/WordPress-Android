package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

import java.util.Date;

public final class UserMapper extends RxResultMapper<PropertySet> {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(UserProperty.URN, Urn.forUser(cursorReader.getLong(Tables.UsersView.ID.name())));
        propertySet.put(UserProperty.USERNAME, cursorReader.getString(Tables.UsersView.USERNAME.name()));
        propertySet.put(UserProperty.FOLLOWERS_COUNT, cursorReader.getInt(Tables.UsersView.FOLLOWERS_COUNT.name()));
        propertySet.put(UserProperty.IS_FOLLOWED_BY_ME, cursorReader.getBoolean(Tables.UsersView.IS_FOLLOWING.name()));
        propertySet.put(UserProperty.IMAGE_URL_TEMPLATE, Optional.fromNullable(cursorReader.getString(Tables.UsersView.AVATAR_URL.name())));
        propertySet.put(UserProperty.VISUAL_URL, Optional.fromNullable(cursorReader.getString(Tables.UsersView.VISUAL_URL.name())));

        putOptionalFields(cursorReader, propertySet);

        return propertySet;
    }

    private void putOptionalFields(CursorReader cursorReader, PropertySet propertySet) {
        if (cursorReader.isNotNull(Tables.UsersView.COUNTRY.name())) {
            propertySet.put(UserProperty.COUNTRY, cursorReader.getString(Tables.UsersView.COUNTRY.name()));
        }

        if (cursorReader.isNotNull(Tables.UsersView.CITY.name())) {
            propertySet.put(UserProperty.CITY, cursorReader.getString(Tables.UsersView.CITY.name()));
        }

        if (cursorReader.isNotNull(Tables.UsersView.DESCRIPTION.name())) {
            propertySet.put(UserProperty.DESCRIPTION, cursorReader.getString(Tables.UsersView.DESCRIPTION.name()));
        }

        if (cursorReader.isNotNull(Tables.UsersView.WEBSITE_URL.name())) {
            propertySet.put(UserProperty.WEBSITE_URL, cursorReader.getString(Tables.UsersView.WEBSITE_URL.name()));
        }

        if (cursorReader.isNotNull(Tables.UsersView.WEBSITE_NAME.name())) {
            propertySet.put(UserProperty.WEBSITE_NAME, cursorReader.getString(Tables.UsersView.WEBSITE_NAME.name()));
        }

        if (cursorReader.isNotNull(Tables.UsersView.DISCOGS_NAME.name())) {
            propertySet.put(UserProperty.DISCOGS_NAME, cursorReader.getString(Tables.UsersView.DISCOGS_NAME.name()));
        }

        if (cursorReader.isNotNull(Tables.UsersView.MYSPACE_NAME.name())) {
            propertySet.put(UserProperty.MYSPACE_NAME, cursorReader.getString(Tables.UsersView.MYSPACE_NAME.name()));
        }

        propertySet.put(UserProperty.ARTIST_STATION,
                        cursorReader.isNotNull(Tables.UsersView.ARTIST_STATION.name()) ?
                        Optional.of(new Urn(cursorReader.getString(Tables.UsersView.ARTIST_STATION.name()))) :
                        Optional.absent());

        propertySet.put(UserProperty.FIRST_NAME,
                        cursorReader.isNotNull(Tables.UsersView.FIRST_NAME.name()) ?
                        Optional.of(cursorReader.getString(Tables.UsersView.FIRST_NAME.name())) :
                        Optional.absent());

        propertySet.put(UserProperty.LAST_NAME,
                        cursorReader.isNotNull(Tables.UsersView.LAST_NAME.name()) ?
                        Optional.of(cursorReader.getString(Tables.UsersView.LAST_NAME.name())) :
                        Optional.absent());

        propertySet.put(UserProperty.SIGNUP_DATE,
                        cursorReader.isNotNull(Tables.UsersView.SIGNUP_DATE.name()) ?
                        Optional.of(new Date(cursorReader.getLong(Tables.UsersView.SIGNUP_DATE.name()))) :
                        Optional.absent());
    }
}
