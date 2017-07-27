package com.soundcloud.android.sync;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Urns;

import android.content.Intent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class SyncIntentHelper {

    private SyncIntentHelper() {
        //not called
    }

    static Syncable getSyncable(Intent intent) {
        if (!intent.hasExtra(ApiSyncService.EXTRA_SYNCABLE)) {
            throw new IllegalArgumentException("Syncable must be present");
        }
        final Syncable syncable = (Syncable) intent.getSerializableExtra(ApiSyncService.EXTRA_SYNCABLE);
        if (syncable == null) {
            throw new IllegalArgumentException("Failed to deserialize syncable");
        }
        return syncable;
    }

    static Intent putSyncable(Intent intent, Syncable syncable) {
        intent.putExtra(ApiSyncService.EXTRA_SYNCABLE, syncable);
        return intent;
    }

    static List<Urn> getSyncEntities(Intent intent) {
        if (intent.hasExtra(ApiSyncService.EXTRA_SYNCABLE_ENTITIES)) {
            return Urns.urnsFromIntent(intent, ApiSyncService.EXTRA_SYNCABLE_ENTITIES);
        } else {
            return Collections.emptyList();
        }
    }

    static Intent putSyncEntities(Intent intent, Collection<Urn> entities) {
        return Urns.writeToIntent(intent, ApiSyncService.EXTRA_SYNCABLE_ENTITIES, new ArrayList<>(entities));
    }

    static List<Syncable> getSyncables(Intent intent) {
        if (!intent.hasExtra(ApiSyncService.EXTRA_SYNCABLES)) {
            throw new IllegalArgumentException("Syncables must be present");
        }

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
