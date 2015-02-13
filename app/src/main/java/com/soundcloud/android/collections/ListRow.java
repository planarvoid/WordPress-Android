package com.soundcloud.android.collections;

import android.database.Cursor;
import android.os.Parcelable;

/**
 * Implemented by layouts that appear as list rows and need to update their content.
 */
@Deprecated
public interface ListRow {

    void display(Cursor cursor);

    void display(int position, Parcelable p);

}
