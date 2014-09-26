package com.soundcloud.android.users;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import rx.Observable;

import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

public class UserWriteStorage {

    private final DatabaseScheduler scheduler;

    public UserWriteStorage(DatabaseScheduler databaseScheduler) {
        this.scheduler = databaseScheduler;
    }

    public Observable<TxnResult> storeUsersAsync(final List<ApiUser> apiUsers) {
        final List<ContentValues> newItems = new ArrayList<ContentValues>(apiUsers.size());
        for (ApiUser user : apiUsers) {
            newItems.add(buildUserContentValues(user));
        }

        return scheduler.scheduleBulkInsert(Table.USERS.name, newItems);
    }

    private ContentValues buildUserContentValues(ApiUser user) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Users._ID, user.getId())
                .put(TableColumns.Users.USERNAME, user.getUsername())
                .put(TableColumns.Users.COUNTRY, user.getCountry())
                .put(TableColumns.Users.FOLLOWERS_COUNT, user.getFollowersCount())
                .get();
    }
}
