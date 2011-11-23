package com.soundcloud.android.fragment;

import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.soundcloud.android.adapter.ScCursorAdapter;
import com.soundcloud.android.adapter.UserFavoritesAdapter;
import com.soundcloud.android.provider.ScContentProvider;

public class MyFavoritesFragment extends CursorListFragment {

	private final static String LOG_TAG = MyFavoritesFragment.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

    @Override
    protected ScCursorAdapter newAdapter() {
        return new UserFavoritesAdapter(getActivity(),null);
    }

    @Override
    protected Uri getCursorUri() {
        return ScContentProvider.Content.TRACKS;
    }

    @Override
    protected void onListItemClick(CursorWrapper cursorWrapper) {
        Log.i(LOG_TAG,"List item clicked");
    }

}
