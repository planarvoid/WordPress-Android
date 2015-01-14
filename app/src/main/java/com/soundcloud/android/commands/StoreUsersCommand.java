package com.soundcloud.android.commands;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class StoreUsersCommand extends StoreCommand<Iterable<ApiUser>> {

    @Inject
    public StoreUsersCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        final List<ContentValues> newItems = new ArrayList<>(Consts.LIST_PAGE_SIZE);
        for (ApiUser user : input) {
            newItems.add(buildUserContentValues(user));
        }

        return database.bulkInsert(Table.Users, newItems);
    }

    public static ContentValues buildUserContentValues(ApiUser user) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Users._ID, user.getId())
                .put(TableColumns.Users.USERNAME, user.getUsername())
                .put(TableColumns.Users.COUNTRY, user.getCountry())
                .put(TableColumns.Users.FOLLOWERS_COUNT, user.getFollowersCount())
                .get();
    }
}
