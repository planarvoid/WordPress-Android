package com.soundcloud.android.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import com.soundcloud.android.adapter.UserFavoritesAdapter;
import com.soundcloud.android.provider.ScContentProvider;

public class UserFavoritesFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

	private final static String LOG_TAG = UserFavoritesFragment.class.getName();
	private UserFavoritesAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = new UserFavoritesAdapter(getActivity(),null);
		setListAdapter(adapter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);

		getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id) {
                /*CursorWrapper cursorWrapper=(CursorWrapper)adapter.getItemAtPosition(position);
                    String tweet=cursorWrapper.getString(cursorWrapper.getColumnIndex(EliGDatabase.TWEET_COL));

                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");

                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Compartido con Eli-G ");

                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, tweet);
                    startActivity(Intent.createChooser(sharingIntent,getString(R.string.shared_with)));*/
                return true;
            }

        });

		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                /*CursorWrapper cursorWrapper=(CursorWrapper)adapter.getItemAtPosition(position);
                    String twitterUsername=cursorWrapper.getString(cursorWrapper.getColumnIndex(EliGDatabase.TWEET_USER_COL));
                    String twitterUserImage=cursorWrapper.getString(cursorWrapper.getColumnIndex(EliGDatabase.TWEET_IMAGE_URL_COL));

                    Intent tweetDetailIntent = new Intent(Clov3rConstant.TWITTER_USERNAME_DETAIL);
                    tweetDetailIntent.putExtra(Clov3rConstant.TWITTER_USERNAME_EXTRA, twitterUsername);
                    tweetDetailIntent.putExtra(Clov3rConstant.TWITTER_USERIMAGE_EXTRA, twitterUserImage);

                    startActivity(tweetDetailIntent);*/


            }

        });
	}

	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	public static UserFavoritesFragment newInstance() {
		return new UserFavoritesFragment();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), ScContentProvider.Content.TRACKS, null,null, null,null);
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		adapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
		adapter.swapCursor(null);
	}

	public void refresh() {
		getLoaderManager().restartLoader(0, null, this);
	}
}
