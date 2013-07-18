package com.soundcloud.android.service.sync;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import org.jetbrains.annotations.Nullable;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;

public class SyncInitiator {

    public static boolean pushFollowingsToApi(@Nullable Account account) {
        if (account != null) {
            final Bundle extras = new Bundle();
            extras.putBoolean(SyncAdapterService.EXTRA_SYNC_PUSH, true);
            extras.putString(SyncAdapterService.EXTRA_SYNC_PUSH_URI, Content.ME_FOLLOWINGS.uri.toString());
            ContentResolver.requestSync(account, ScContentProvider.AUTHORITY, extras);
            return true;
        } else {
            return false;
        }

    }
}
