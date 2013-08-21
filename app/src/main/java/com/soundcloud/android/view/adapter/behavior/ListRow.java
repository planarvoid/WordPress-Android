package com.soundcloud.android.view.adapter.behavior;

import android.database.Cursor;
import android.os.Parcelable;

/**
 * Implemented by layouts that appear as list rows and need to update their content.
 */
public interface ListRow {

    void display(Cursor cursor);

    void display(int position, Parcelable p);

}
