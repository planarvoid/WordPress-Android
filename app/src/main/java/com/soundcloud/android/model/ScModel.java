package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class ScModel implements Parcelable, ModelLike {

    public static final String EXTRA_ID = "id";
    public static final int NOT_SET = -1;
    @JsonView(Views.Mini.class) public long id = NOT_SET;

    public ScModel() { }

    public ScModel(long id) {
        this.id = id;
    }

    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        if (id != ScResource.NOT_SET) cv.put(BaseColumns._ID, id);
        return cv;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    protected static int getIntOrNotSet(Cursor c, String column) {
        final int index = c.getColumnIndex(column);
        return c.isNull(index) ? NOT_SET : c.getInt(index);
    }

    @JsonIgnore
    public long getListItemId() {
        return id;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public Uri toUri() {
        return null;
    }
}
