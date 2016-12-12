package com.soundcloud.android.sync;

import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.model.Urn;

import android.content.Intent;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class SyncIntentHelper {

    private SyncIntentHelper() {
        //not called
    }

    static Syncable getSyncable(Intent intent) {
        checkArgument(intent.hasExtra(ApiSyncService.EXTRA_SYNCABLE), "Syncable must be present");
        final Syncable syncable = (Syncable) intent.getSerializableExtra(ApiSyncService.EXTRA_SYNCABLE);
        return checkNotNull(syncable, "Failed to deserialize syncable");
    }

    static Intent putSyncable(Intent intent, Syncable syncable) {
        intent.putExtra(ApiSyncService.EXTRA_SYNCABLE, syncable);
        return intent;
    }

    static List<Urn> getSyncEntities(Intent intent) {
        if (intent.hasExtra(ApiSyncService.EXTRA_SYNCABLE_ENTITIES)) {
            return intent.getParcelableArrayListExtra(ApiSyncService.EXTRA_SYNCABLE_ENTITIES);
        } else {
            return Collections.emptyList();
        }
    }

    static Intent putSyncEntities(Intent intent, Collection<Urn> entities) {
        intent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNCABLE_ENTITIES, new ArrayList<Parcelable>(entities));
        return intent;
    }

    static List<Syncable> getSyncables(Intent intent) {
        checkArgument(intent.hasExtra(ApiSyncService.EXTRA_SYNCABLES), "Syncables must be present");

        List<Syncable> syncables = new ArrayList<>();
        for (String syncableName : intent.getStringArrayListExtra(ApiSyncService.EXTRA_SYNCABLES)) {
            syncables.add(Syncable.valueOf(syncableName));
        }
        return syncables;
    }

    static Intent putSyncables(Intent intent, List<Syncable> syncables) {
        final ArrayList<String> names = new ArrayList<>(syncables.size());

        for (Syncable syncable : syncables) {
            names.add(syncable.name());
        }
        intent.putStringArrayListExtra(ApiSyncService.EXTRA_SYNCABLES, names);
        return intent;
    }
}
