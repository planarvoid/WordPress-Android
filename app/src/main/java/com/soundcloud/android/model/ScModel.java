package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;

import android.content.ContentValues;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class ScModel implements Parcelable {

    public static final int NOT_SET = -1;
    @JsonView(Views.Mini.class) public long id = NOT_SET;

    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        if (id != ScResource.NOT_SET) cv.put(BaseColumns._ID, id);
        return cv;
    }

    public void resolve(Context context) {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
