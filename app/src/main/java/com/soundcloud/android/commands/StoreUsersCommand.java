package com.soundcloud.android.commands;

import static com.soundcloud.java.collections.Iterables.addAll;

import com.soundcloud.android.storage.Tables.Users;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.schema.BulkInsertValues;
import com.soundcloud.propeller.schema.Column;

import android.content.ContentValues;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;

public class StoreUsersCommand extends DefaultWriteStorageCommand<Iterable<? extends UserRecord>, WriteResult> {

    @Inject
    public StoreUsersCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Iterable<? extends UserRecord> input) {
        return propeller.bulkInsert(Users.TABLE, getBulkInsertValues(input));
    }

    @NonNull
    private static BulkInsertValues getBulkInsertValues(Iterable<? extends UserRecord> input) {
        final HashSet<UserRecord> deduped = new HashSet<>();
        addAll(deduped, input);

        BulkInsertValues.Builder builder = new BulkInsertValues.Builder(Arrays.asList(
                Users._ID,
                Users.PERMALINK,
                Users.USERNAME,
                Users.COUNTRY,
                Users.CITY,
                Users.FOLLOWERS_COUNT,
                Users.DESCRIPTION,
                Users.AVATAR_URL,
                Users.VISUAL_URL,
                Users.WEBSITE_URL,
                Users.WEBSITE_NAME,
                Users.DISCOGS_NAME,
                Users.MYSPACE_NAME,
                Users.ARTIST_STATION
        ));

        for (UserRecord user : deduped) {
            builder.addRow(Arrays.asList(
                    user.getUrn().getNumericId(),
                    user.getPermalink(),
                    user.getUsername(),
                    user.getCountry(),
                    user.getCity(),
                    user.getFollowersCount(),
                    user.getDescription().orNull(),
                    user.getImageUrlTemplate().orNull(),
                    user.getVisualUrlTemplate().orNull(),
                    user.getWebsiteUrl().orNull(),
                    user.getWebsiteName().orNull(),
                    user.getDiscogsName().orNull(),
                    user.getMyspaceName().orNull(),
                    user.getArtistStationUrn().transform(Urns.TO_STRING).orNull()
            ));
        }
        return builder.build();
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

    private static <T> void putOptionalValue(ContentValuesBuilder baseBuilder, Optional<T> value, Column column) {
        baseBuilder.put(column.name(), value.isPresent() ? value.get().toString() : null);
    }
}
