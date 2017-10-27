package com.soundcloud.android.commands;

import static com.soundcloud.java.collections.Iterables.addAll;

import com.soundcloud.android.storage.Tables.Users;
import com.soundcloud.android.users.UserRecord;
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
                Users.FIRST_NAME,
                Users.LAST_NAME,
                Users.COUNTRY,
                Users.CITY,
                Users.FOLLOWERS_COUNT,
                Users.FOLLOWINGS_COUNT,
                Users.DESCRIPTION,
                Users.AVATAR_URL,
                Users.VISUAL_URL,
                Users.ARTIST_STATION,
                Users.SIGNUP_DATE,
                Users.IS_PRO
        ));

        for (UserRecord user : deduped) {
            builder.addRow(Arrays.asList(
                    user.getUrn().getNumericId(),
                    user.getPermalink(),
                    user.getUsername(),
                    user.getFirstName().orNull(),
                    user.getLastName().orNull(),
                    user.getCountry(),
                    user.getCity(),
                    user.getFollowersCount(),
                    user.getFollowingsCount(),
                    user.getDescription().orNull(),
                    user.getImageUrlTemplate().orNull(),
                    user.getVisualUrlTemplate().orNull(),
                    user.getArtistStationUrn().transform(urn -> urn.getContent()).orNull(),
                    user.getCreatedAt().isPresent() ? user.getCreatedAt().get().getTime() : null,
                    user.isPro()
            ));
        }
        return builder.build();
    }

    public static ContentValues buildUserContentValues(UserRecord user) {
        final ContentValuesBuilder baseBuilder = getBaseBuilder(user);

        putOptionalValue(baseBuilder, user.getFirstName(), Users.FIRST_NAME);
        putOptionalValue(baseBuilder, user.getLastName(), Users.LAST_NAME);
        putOptionalValue(baseBuilder, user.getDescription(), Users.DESCRIPTION);
        putOptionalValue(baseBuilder, user.getImageUrlTemplate(), Users.AVATAR_URL);
        putOptionalValue(baseBuilder, user.getVisualUrlTemplate(), Users.VISUAL_URL);
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
                                   .put(Users.FOLLOWERS_COUNT, user.getFollowersCount())
                                   .put(Users.FOLLOWINGS_COUNT, user.getFollowingsCount())
                                   .put(Users.SIGNUP_DATE, user.getCreatedAt().isPresent() ? user.getCreatedAt().get().getTime() : null)
                                   .put(Users.IS_PRO, user.isPro());
    }

    private static <T> void putOptionalValue(ContentValuesBuilder baseBuilder, Optional<T> value, Column column) {
        baseBuilder.put(column.name(), value.isPresent() ? value.get().toString() : null);
    }
}
