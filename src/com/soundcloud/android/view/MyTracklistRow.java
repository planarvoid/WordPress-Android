
package com.soundcloud.android.view;

import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.adapter.LazyBaseAdapter;

import android.database.Cursor;
import android.util.Log;

public class MyTracklistRow extends TracklistRow {

    public MyTracklistRow(ScActivity _activity, LazyBaseAdapter _adapter) {
        super(_activity, _adapter);
    }

    /** update the views with the data corresponding to selection index */
    @Override
    public void display(int position) {
        Log.i("tracklistrow","DISPLAY ITEM " + mAdapter.getItem(position));
        if (mAdapter.getItem(position) instanceof Cursor)
            Log.i("tracklistrow","DISPLAY A CURSOR");
        else
            super.display(position);

    }
}
