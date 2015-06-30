package com.soundcloud.android.sync;

import com.google.common.base.Objects;
import com.soundcloud.android.events.UrnEvent;
import com.soundcloud.android.model.Urn;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SyncResult implements Parcelable, UrnEvent {

    private final String action;
    private final boolean wasChanged;
    private final Exception exception;
    private final List<Urn> entitiesSynced;

    public static final Creator<SyncResult> CREATOR = new Creator<SyncResult>() {
        public SyncResult createFromParcel(Parcel source) {
            return new SyncResult(source);
        }

        public SyncResult[] newArray(int size) {
            return new SyncResult[size];
        }
    };

    private SyncResult(Parcel in) {
        this.action = in.readString();
        this.wasChanged = in.readByte() != 0;
        this.exception = (Exception) in.readSerializable();

        this.entitiesSynced = new ArrayList<>();
        in.readTypedList(this.entitiesSynced, Urn.CREATOR);
    }

    private SyncResult(String action, boolean wasChanged, Exception exception, List<Urn> entities) {
        this.action = action;
        this.wasChanged = wasChanged;
        this.exception = exception;
        this.entitiesSynced = entities;
    }

    public static SyncResult success(String action, boolean wasChanged) {
        return new SyncResult(action, wasChanged, null, Collections.<Urn>emptyList());
    }

    public static SyncResult success(String action, boolean wasChanged, Urn entity) {
        return new SyncResult(action, wasChanged, null, Arrays.asList(entity));
    }

    public static SyncResult failure(String action, Exception exception) {
        return new SyncResult(action, false, exception, Collections.<Urn>emptyList());
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
        if (!(o instanceof SyncResult)) {
            return false;
        }
        SyncResult that = (SyncResult) o;
        return Objects.equal(wasChanged, that.wasChanged)
                && Objects.equal(action, that.action)
                && Objects.equal(exception, that.exception)
                && Objects.equal(entitiesSynced, that.entitiesSynced);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(wasChanged, action, exception, entitiesSynced);
    }
}
