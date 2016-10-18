package com.soundcloud.android.commands;

import static com.soundcloud.android.storage.TableColumns.Users;
import static com.soundcloud.java.collections.Iterables.addAll;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class StoreUsersCommand extends DefaultWriteStorageCommand<Iterable<? extends UserRecord>, WriteResult> {

    @Inject
    public StoreUsersCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Iterable<? extends UserRecord> input) {
        final HashSet<UserRecord> deduped = new HashSet<>();
        addAll(deduped, input);

        final List<ContentValues> newItems = new ArrayList<>(Iterables.size(deduped));
        for (UserRecord user : deduped) {
            newItems.add(buildUserContentValues(user));
        }
        return propeller.bulkInsert_experimental(Table.Users, getColumnTypes(),newItems);
    }

    private Map<String, Class> getColumnTypes() {
        final HashMap<String, Class> columns = new HashMap<>();
        columns.put(Users._ID, Long.class);
        columns.put(Users.PERMALINK, String.class);
        columns.put(Users.USERNAME, String.class);
        columns.put(Users.COUNTRY, String.class);
        columns.put(Users.CITY, String.class);
        columns.put(Users.FOLLOWERS_COUNT, Integer.class);
        columns.put(Users.DESCRIPTION, String.class);
        columns.put(Users.AVATAR_URL, String.class);
        columns.put(Users.VISUAL_URL, String.class);
        columns.put(Users.WEBSITE_URL, String.class);
        columns.put(Users.WEBSITE_NAME, String.class);
        columns.put(Users.DISCOGS_NAME, String.class);
        columns.put(Users.MYSPACE_NAME, String.class);
        columns.put(Users.ARTIST_STATION, String.class);
        return columns;

    }

    public static ContentValues buildUserContentValues(UserRecord user) {
        final ContentValuesBuilder baseBuilder = getBaseBuilder(user);

        putOptionalValue(baseBuilder, user.getDescription(), Users.DESCRIPTION);
        putOptionalValue(baseBuilder, user.getImageUrlTemplate(), Users.AVATAR_URL);
        putOptionalValue(baseBuilder, user.getVisualUrlTemplate(), Users.VISUAL_URL);
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
