package com.soundcloud.android.sync;

import static java.util.Collections.singletonList;

import com.soundcloud.android.events.UrnEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import rx.functions.Func1;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SyncJobResult implements Parcelable, UrnEvent {

    public static final Func1<SyncJobResult, Urn> TO_URN = syncJobResult -> syncJobResult.getFirstUrn();

    public static final Func1<SyncJobResult, Boolean> IS_SINGLE_PLAYLIST_SYNCED_FILTER = syncJobResult -> Syncable.PLAYLIST.name().equals(syncJobResult.getAction())
            && syncJobResult.wasChanged()
            && syncJobResult.hasChangedEntities();

    private final String action;
    private final boolean wasChanged;
    private final Exception exception;
    private final List<Urn> entitiesSynced;

    public static final Creator<SyncJobResult> CREATOR = new Creator<SyncJobResult>() {
        public SyncJobResult createFromParcel(Parcel source) {
            return new SyncJobResult(source);
        }

        public SyncJobResult[] newArray(int size) {
            return new SyncJobResult[size];
        }
    };

    private SyncJobResult(Parcel in) {
        this.action = in.readString();
        this.wasChanged = in.readByte() != 0;
        this.exception = (Exception) in.readSerializable();

        this.entitiesSynced = new ArrayList<>();
        in.readTypedList(this.entitiesSynced, Urn.CREATOR);
    }

    private SyncJobResult(String action, boolean wasChanged, Exception exception, List<Urn> entities) {
        this.action = action;
        this.wasChanged = wasChanged;
        this.exception = exception;
        this.entitiesSynced = entities;
    }

    public static SyncJobResult success(String action, boolean wasChanged) {
        return new SyncJobResult(action, wasChanged, null, Collections.<Urn>emptyList());
    }

    public static SyncJobResult success(String action, boolean wasChanged, Urn entity) {
        return success(action, wasChanged, singletonList(entity));
    }

    public static SyncJobResult success(String action, boolean wasChanged, List<Urn> entities) {
        return new SyncJobResult(action, wasChanged, null, entities);
    }

    public static SyncJobResult failure(String action, Exception exception) {
        return new SyncJobResult(action, false, exception, Collections.<Urn>emptyList());
    }

    public String getAction() {
        return action;
    }

    public boolean wasChanged() {
        return wasChanged;
    }

    public boolean wasSuccess() {
        return exception == null;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public Urn getFirstUrn() {
        return entitiesSynced.iterator().next();
    }

    public List<Urn> getUrns() {
        return entitiesSynced;
    }

    public Boolean hasChangedEntities() {
        return !entitiesSynced.isEmpty();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.action);
        dest.writeByte(wasChanged ? (byte) 1 : (byte) 0);
        dest.writeSerializable(this.exception);
        dest.writeTypedList(this.entitiesSynced);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncJobResult)) {
            return false;
        }
        SyncJobResult that = (SyncJobResult) o;
        return MoreObjects.equal(wasChanged, that.wasChanged)
                && MoreObjects.equal(action, that.action)
                && MoreObjects.equal(exception, that.exception)
                && MoreObjects.equal(entitiesSynced, that.entitiesSynced);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(wasChanged, action, exception, entitiesSynced);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("action", action)
                          .add("wasChanged", wasChanged)
                          .add("exception", exception)
                          .add("entitiesSynced", entitiesSynced)
                          .toString();
    }
}

