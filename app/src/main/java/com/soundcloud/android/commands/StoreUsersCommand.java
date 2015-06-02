package com.soundcloud.android.commands;

import com.google.common.base.Optional;
import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.users.UserRecord;
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
        final List<ContentValues> newItems = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        for (UserRecord user : input) {
            newItems.add(buildUserContentValues(user));
        }
        return propeller.bulkInsert(Table.Users, newItems);
    }

    public static ContentValues buildUserContentValues(UserRecord user) {
        final ContentValuesBuilder baseBuilder = getBaseBuilder(user);

        putOptionalString(baseBuilder, user.getDescription(), TableColumns.Users.DESCRIPTION);
        putOptionalString(baseBuilder, user.getWebsiteUrl(), TableColumns.Users.WEBSITE_URL);
        putOptionalString(baseBuilder, user.getWebsiteName(), TableColumns.Users.WEBSITE_NAME);
        putOptionalString(baseBuilder, user.getDiscogsName(), TableColumns.Users.DISCOGS_NAME);
        putOptionalString(baseBuilder, user.getMyspaceName(), TableColumns.Users.MYSPACE_NAME);

        return baseBuilder.get();
    }

    private static ContentValuesBuilder getBaseBuilder(UserRecord user) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Users._ID, user.getUrn().getNumericId())
                .put(TableColumns.Users.USERNAME, user.getUsername())
                .put(TableColumns.Users.COUNTRY, user.getCountry())
                .put(TableColumns.Users.FOLLOWERS_COUNT, user.getFollowersCount());
    }

    private static void putOptionalString(ContentValuesBuilder baseBuilder, Optional<String> value, String column) {
        if (value.isPresent()) {
            baseBuilder.put(column, value.get());
        }
    }
}
