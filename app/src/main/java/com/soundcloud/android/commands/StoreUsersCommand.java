package com.soundcloud.android.commands;

import static com.soundcloud.android.storage.TableColumns.Users;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class StoreUsersCommand extends DefaultWriteStorageCommand<Iterable<? extends UserRecord>, WriteResult> {

    @Inject
    public StoreUsersCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Iterable<? extends UserRecord> input) {
        final List<ContentValues> newItems = new ArrayList<>(Iterables.size(input));
        for (UserRecord user : input) {
            newItems.add(buildUserContentValues(user));
        }
        return propeller.bulkInsert(Table.Users, newItems);
    }

    public static ContentValues buildUserContentValues(UserRecord user) {
        final ContentValuesBuilder baseBuilder = getBaseBuilder(user);

        putOptionalValue(baseBuilder, user.getDescription(), Users.DESCRIPTION);
        putOptionalValue(baseBuilder, user.getImageUrlTemplate(), Users.AVATAR_URL);
        putOptionalValue(baseBuilder, user.getWebsiteUrl(), Users.WEBSITE_URL);
        putOptionalValue(baseBuilder, user.getWebsiteName(), Users.WEBSITE_NAME);
        putOptionalValue(baseBuilder, user.getDiscogsName(), Users.DISCOGS_NAME);
        putOptionalValue(baseBuilder, user.getMyspaceName(), Users.MYSPACE_NAME);
        putOptionalValue(baseBuilder, user.getArtistStationUrn(), Users.ARTIST_STATION);

        return baseBuilder.get();
    }

    private static ContentValuesBuilder getBaseBuilder(UserRecord user) {
        return ContentValuesBuilder.values()
                                   .put(Users._ID, user.getUrn().getNumericId())
                                   .put(Users.PERMALINK, user.getPermalink())
                                   .put(Users.USERNAME, user.getUsername())
                                   .put(Users.COUNTRY, user.getCountry())
                                   .put(Users.CITY, user.getCity())
                                   .put(Users.FOLLOWERS_COUNT, user.getFollowersCount());
    }

    private static <T> void putOptionalValue(ContentValuesBuilder baseBuilder, Optional<T> value, String column) {
        baseBuilder.put(column, value.isPresent() ? value.get().toString() : null);
    }
}
