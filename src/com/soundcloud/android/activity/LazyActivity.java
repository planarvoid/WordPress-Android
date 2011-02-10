
package com.soundcloud.android.activity;

import java.util.ArrayList;
import java.util.List;

import org.urbanstew.soundcloudapi.SoundCloudAPI;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.DBAdapter;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.objects.Comment;
import com.soundcloud.android.objects.Event;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.android.objects.BaseObj.WriteState;
import com.soundcloud.android.task.LoadTask;

public abstract class LazyActivity extends ScActivity implements OnItemClickListener {
    private static final String TAG = "LazyActivity";

    protected LinearLayout mHolder;

    protected Parcelable mDetailsData;

    protected long mCurrentTrackId = -1;

    protected SoundCloudAPI.State mLastCloudState;

    protected String mFilter = "";

    private int mPageSize;

    private String mTrackOrder;

    private String mUserOrder;

    protected DBAdapter db;

    protected SharedPreferences mPreferences;

    protected Comment addComment;

    private MenuItem menuCurrentPlayingItem;

    private MenuItem menuCurrentUploadingItem;

    protected Parcelable menuParcelable;

    protected Parcelable dialogParcelable;

    protected String dialogUsername;

    protected LoadTask mLoadTask;

    private ProgressDialog mProgressDialog;

    protected LinearLayout mMainHolder;

    protected int mSearchListIndex;

    /**
     * @param savedInstanceState
     * @param layoutResId
     */
    protected void onCreate(Bundle savedInstanceState, int layoutResId) {
        super.onCreate(savedInstanceState);

        setContentView(layoutResId);

        build();
        restoreState();
        initLoadTasks();

    }

    /**
     * A parcelable object has just been retrieved by an async task somewhere,
     * so perform any mapping necessary on that object, for example:
     * {@link com.soundcloud.android.activity.Dashboard}
     * 
     * @param p
     */
    public void mapDetails(Parcelable p) {

    }

    /**
     * Get a progress dialog that has been created. Used primarily to update
     * dialog as necessary
     * 
     * @return the current progress dialog, or null if one doesnt exist
     */
    public ProgressDialog getProgressDialog() {
        return mProgressDialog;
    }

    /**
     * Set the current progress dialog
     * 
     * @param progressDialog : the progress dialog that shoudld be set as
     *            current
     */
    public void setProgressDialog(ProgressDialog progressDialog) {
        mProgressDialog = progressDialog;
    }

    /**
     * @return
     */
    public String getTrackOrder() {
        return mTrackOrder;
    }

    /**
     * @return
     */
    public String getUserOrder() {
        return mUserOrder;
    }

    public void configureListMenu(ListView list, CloudUtils.LoadType type) {

        list.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
                menu.setHeaderTitle("With this track");
                menu.setHeaderIcon(R.drawable.ic_tab_you);
                menu.add(0, 0, 0, "Play").setIcon(R.drawable.play);
                menu.add(0, 1, 1, "View User Profile").setIcon(R.drawable.ic_tab_you);
                menu.add(0, 2, 2, "Favorite").setIcon(R.drawable.favorite);
                SubMenu sharingMenu = menu.addSubMenu("Share to").setIcon(R.drawable.share)
                        .setHeaderIcon(R.drawable.share);
                sharingMenu.add(0, 3, 1, "Twitter");
                sharingMenu.add(0, 4, 1, "Facebook");

            }
        });
    }

    /**
     * Initialize any new loading, this will take place after any running
     * loading tasks have been restored
     */
    protected void initLoadTasks() {

    }

    /**
     * Get the id of the track that is currently playing
     * 
     * @return the track id that is being played
     */
    public long getCurrentTrackId() {
        return mCurrentTrackId;
    }

    /**
     * A parcelable has just been loaded, so perform any data operations
     * necessary
     * 
     * @param p : the parcelable that has just been loaded
     */
    public void resolveParcelable(Parcelable p) {
        if (p instanceof Track) {
            CloudUtils.resolveTrack(getSoundCloudApplication(), (Track) p, WriteState.none,
                    CloudUtils.getCurrentUserId(this));
        } else if (p instanceof Event) {
            if (((Event) p).getTrack() != null)
                CloudUtils.resolveTrack(getSoundCloudApplication(), ((Event) p).getTrack(),
                        WriteState.none, CloudUtils.getCurrentUserId(this));
        } else if (p instanceof User) {
            CloudUtils.resolveUser(getSoundCloudApplication(), (User) p, WriteState.none,
                    CloudUtils.getCurrentUserId(this));
        }
    }

    /**
     * Track has been clicked in a list, enqueu the list of tracks if necessary
     * and send the user to the player
     * 
     * @param list
     * @param playPos
     */
    public void playTrack(final List<Parcelable> list, final int playPos) {

        Track t = null;

        // is this a track of a list
        if (list.get(playPos) instanceof Track)
            t = ((Track) list.get(playPos));
        else if (list.get(playPos) instanceof Event)
            t = ((Event) list.get(playPos)).getTrack();

        // find out if this track is already playing. If it is, just go to the
        // player
        try {
            if (t != null && mService != null && mService.getTrackId() != -1
                    && mService.getTrackId() == (t.id)) {
                // skip the enquing, its already playing
                Intent intent = new Intent(this, ScPlayer.class);
                startActivity(intent);
                return;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // pass the tracklist to the application. This is the quckest way to get
        // it to the service
        // another option would be to pass the parcelables through the intent,
        // but that has the
        // unnecessary overhead of unmarshalling/marshallingi them in to
        // bundles. This way
        // we are just passing pointers
        this.getSoundCloudApplication().cachePlaylist((ArrayList<Parcelable>) list);

        try {
            Log.i(TAG, "Play from app cache call");
            mService.playFromAppCache(playPos);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        startActivity(new Intent(this, ScPlayer.class));
    }

    /**
     * A list item has been clicked
     */
    public void onItemClick(AdapterView<?> list, View row, int position, long id) {

        if (((LazyBaseAdapter) list.getAdapter()).getData().size() <= 0
                || position >= ((LazyBaseAdapter) list.getAdapter()).getData().size())
            return; // bad list item clicked (possibly loading item)

        if (((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof Track
                || ((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof Event) {
            // track clicked
            this.playTrack(((LazyBaseAdapter) list.getAdapter()).getData(), position);

        } else if (((LazyBaseAdapter) list.getAdapter()).getData().get(position) instanceof User) {

            // user clicked
            Intent i = new Intent(this, ScProfile.class);
            i.putExtra("user", ((LazyBaseAdapter) list.getAdapter()).getData().get(position));
            startActivity(i);

        }
    }

    /**
     * Handle common options menu building
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menuCurrentPlayingItem = menu.add(menu.size(), CloudUtils.OptionsMenu.VIEW_CURRENT_TRACK,
                menu.size(), R.string.menu_view_current_track).setIcon(
                R.drawable.ic_menu_info_details);
        menuCurrentUploadingItem = menu.add(menu.size(),
                CloudUtils.OptionsMenu.CANCEL_CURRENT_UPLOAD, menu.size(),
                R.string.menu_cancel_current_upload).setIcon(R.drawable.ic_menu_delete);

        menu.add(menu.size(), CloudUtils.OptionsMenu.SETTINGS, menu.size(), R.string.menu_settings)
                .setIcon(R.drawable.ic_menu_preferences);
        return super.onCreateOptionsMenu(menu);
    }

    public long getCurrentUserId() {
        return CloudUtils.getCurrentUserId(this);
    }

    /**
     * Prepare the options menu based on the current class and current play
     * state
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!this.getClass().getName().contentEquals("com.soundcloud.android.ScPlayer")) {
            menuCurrentPlayingItem.setVisible(true);
        } else {
            menuCurrentPlayingItem.setVisible(false);
        }

        try {
            if (mCreateService.isUploading()) {
                menuCurrentUploadingItem.setVisible(true);
            } else {
                menuCurrentUploadingItem.setVisible(false);
            }
        } catch (Exception e) {
            menuCurrentUploadingItem.setVisible(false);
        }

        return true;
    }

    public void leftSwipe() {
        Toast.makeText(this, "Left Swipe", Toast.LENGTH_SHORT).show();
    }

    public void rightSwipe() {
        Toast.makeText(this, "Right Swipe", Toast.LENGTH_SHORT).show();
    }

    public void setPageSize(int mPageSize) {
        this.mPageSize = mPageSize;
    }

    public int getPageSize() {
        return mPageSize;
    }

    /***** State saving/restoring *****/

    /**
     * Restore the state of this activity from a previous state, if one exists
     */
    protected void restoreState() {

    }

    /***** Build and Lifecycle *****/

    /**
     * Build any components we need to for this activity
     */
    protected void build() {

    }

    /**
     * Bind our services
     */
    @Override
    protected void onStart() {
        super.onStart();

    }

    /**
     * Unbind our services
     */
    @Override
    protected void onStop() {
        super.onStop();

    }

    /**
	 * 
	 */
    @Override
    protected void onResume() {
        super.onResume();

        // set our default preferences here, in case they were just changed
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setPageSize(Integer.parseInt(mPreferences.getString("defaultPageSize", "20")));
        mTrackOrder = mPreferences.getString("defaultTrackSorting", "");
        mUserOrder = mPreferences.getString("defaultUserSorting", "");
    }

    @Override
    protected void onAuthenticated() {
        super.onAuthenticated();
        if (mHolder != null)
            mHolder.setVisibility(View.VISIBLE);
        initLoadTasks();
        mLastCloudState = getSoundCloudApplication().getState();
    }
}
