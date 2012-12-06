package com.soundcloud.android.fragment;

import com.soundcloud.android.model.Recording;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.view.MyTracklistRow;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.view.ViewGroup;

class Foo extends ListFragment implements LoaderManager.LoaderCallbacks {

    private CursorAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        mAdapter = new CursorAdapter(getActivity(),null) {
//            @Override
//            public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
//                return new MyTracklistRow(getActivity())
//            }
//
//            @Override
//            public void bindView(View view, Context context, Cursor cursor) {
//            }
//        };
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {

        return new CursorLoader(getActivity(), Content.RECORDINGS.uri,
                null,
                DBHelper.Recordings.UPLOAD_STATUS + " < " + Recording.Status.UPLOADED + " OR " + DBHelper.Recordings.UPLOAD_STATUS + " = " + Recording.Status.ERROR,
                null,
                DBHelper.Recordings.TIMESTAMP + " DESC");
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        mAdapter.swapCursor((Cursor) data);
    }


    @Override
    public void onLoaderReset(Loader loader) {
        mAdapter.swapCursor(null);
    }
}