package com.soundcloud.android.sync;

import static com.soundcloud.android.storage.Table.Collections;
import static com.soundcloud.android.storage.TableColumns.Collections.EXTRA;
import static com.soundcloud.android.storage.TableColumns.Collections.LAST_SYNC;
import static com.soundcloud.android.storage.TableColumns.Collections.LAST_SYNC_ATTEMPT;
import static com.soundcloud.android.storage.TableColumns.Collections.URI;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.UriUtils;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;

public class SyncStateStorage {

    private final PropellerRx propellerRx;
    private final SharedPreferences preferences;
    private final CurrentDateProvider dateProvider;
    private final PropellerDatabase propeller;

    @Inject
    public SyncStateStorage(PropellerDatabase propeller,
                            @Named(StorageModule.SYNCER) SharedPreferences preferences,
                            CurrentDateProvider dateProvider) {
        this.propeller = propeller;
        this.propellerRx = new PropellerRx(propeller);
        this.preferences = preferences;
        this.dateProvider = dateProvider;
    }

    void clear() {
        preferences.edit().clear().apply();
        propeller.delete(Collections);
    }

    public Observable<Boolean> hasSyncedMyPostsBefore() {
        return hasSyncedBefore(SyncContent.MySounds.contentUri());
    }

    public Observable<Boolean> hasSyncedBefore(Uri uri) {
        final Query query = Query.apply(exists(Query.from(Collections)
                .whereGt(LAST_SYNC, 0L)
                .whereEq(URI, normalizedUriString(uri))));
        return propellerRx.query(query).map(scalar(Boolean.class)).defaultIfEmpty(false);
    }

    public Observable<Long> lastSyncOrAttemptTime(Uri uri) {
        final Query query = Query.from(Collections)
                .select("max(" + LAST_SYNC_ATTEMPT + ", " + LAST_SYNC + ")")
                .whereEq(URI, normalizedUriString(uri));
        return propellerRx.query(query).map(scalar(Long.class)).defaultIfEmpty((long) Consts.NOT_SET);
    }

    public void synced(String entity) {
        preferences.edit().putLong(entity, dateProvider.getCurrentTime()).apply();
    }

    public boolean hasSyncedBefore(String entity) {
        return preferences.getLong(entity, Consts.NOT_SET) != Consts.NOT_SET;
    }

    private static String normalizedUriString(Uri uri) {
        return UriUtils.clearQueryParams(uri).toString();
    }

    ChangeResult legacyUpdateLastSyncAttempt(Uri uri, long timestamp) {
        final ContentValues contentValues = new ContentValues(2);
        contentValues.put(URI, normalizedUriString(uri));
        contentValues.put(LAST_SYNC_ATTEMPT, timestamp);
        return propeller.upsert(Collections, contentValues);
    }

    ChangeResult legacyUpdateLastSyncSuccess(Uri uri, long timestamp) {
        final ContentValues contentValues = new ContentValues(2);
        contentValues.put(URI, normalizedUriString(uri));
        contentValues.put(LAST_SYNC, timestamp);
        return propeller.upsert(Collections, contentValues);
    }

    long legacyLoadLastSyncSuccess(Uri uri) {
        return propeller.query(Query.from(Collections)
                .select(LAST_SYNC)
                .whereEq(URI, normalizedUriString(uri)))
                .firstOrDefault(Long.class, -1L);
    }

    ChangeResult legacyUpdateSyncMisses(Uri uri, int syncMisses) {
        ContentValues contentValues = new ContentValues(2);
        contentValues.put(URI, normalizedUriString(uri));
        contentValues.put(EXTRA, syncMisses);
        return propeller.upsert(Collections, contentValues);
    }

    int legacyLoadSyncMisses(Uri uri) {
        final String extra = propeller.query(
                Query.from(Collections)
                        .select(EXTRA)
                        .whereEq(URI, normalizedUriString(uri)))
                .firstOrDefault(String.class, null);
        if (Strings.isNullOrEmpty(extra)) {
            return 0;
        }
        return Integer.parseInt(extra);
    }
}
