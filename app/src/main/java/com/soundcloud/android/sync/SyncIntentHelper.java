package com.soundcloud.android.sync;

import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class SyncIntentHelper {

    public static Syncable getSyncable(Intent intent) {
        checkArgument(intent.hasExtra(ApiSyncService.EXTRA_SYNCABLE), "Syncable must be present");
        final Syncable syncable = (Syncable) intent.getSerializableExtra(ApiSyncService.EXTRA_SYNCABLE);
        return checkNotNull(syncable, "Failed to deserialize syncable");
    }

    public static Intent putSyncable(Intent intent, Syncable syncable) {
        intent.putExtra(ApiSyncService.EXTRA_SYNCABLE, syncable.name());
        return intent;
    }

    public static List<Syncable> getSyncables(Intent intent) {
        checkArgument(intent.hasExtra(ApiSyncService.EXTRA_SYNCABLES), "Syncables must be present");

        List<Syncable> syncables = new ArrayList<>();
        for (String syncableName : intent.getStringArrayListExtra(ApiSyncService.EXTRA_SYNCABLES)) {
            syncables.add(Syncable.valueOf(syncableName));
        }
        return syncables;
    }

    public static Intent putSyncables(Intent intent, List<Syncable> syncables) {
        final ArrayList<String> names = new ArrayList<>(syncables.size());

        for (Syncable syncable : syncables) {
            names.add(syncable.name());
        }
        intent.putStringArrayListExtra(ApiSyncService.EXTRA_SYNCABLES, names);
        return intent;
    }
}
