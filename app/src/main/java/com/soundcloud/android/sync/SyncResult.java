package com.soundcloud.android.sync;

import android.os.Parcel;
import android.os.Parcelable;

public final class SyncResult implements Parcelable {

    private final String action;
    private final boolean wasChanged;
    private final Exception exception;

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
    }

    private SyncResult(String action, boolean wasChanged, Exception exception){
        this.action = action;
        this.wasChanged = wasChanged;
        this.exception = exception;
    }

    public static SyncResult success(String action, boolean wasChanged){
        return new SyncResult(action, wasChanged, null);
    }

    public static SyncResult failure(String action, Exception exception){
        return new SyncResult(action, false, exception);
    }

    public String getAction() {
        return action;
    }

    @SuppressWarnings("UnusedDeclaration") // will be used
    public boolean wasChanged() {
        return wasChanged;
    }

    public boolean wasSuccess(){
        return exception == null;
    }

    public Exception getException() {
        return exception;
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
    }
}
