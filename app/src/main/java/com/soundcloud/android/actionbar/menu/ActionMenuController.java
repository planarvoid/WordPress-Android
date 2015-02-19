package com.soundcloud.android.actionbar.menu;

import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public interface ActionMenuController {

    static final int STATE_START_SYNC = 0;
    static final int STATE_SYNCING = 1;
    static final int STATE_REMOVE_SYNC = 2;

    void onCreateOptionsMenu(Menu menu, MenuInflater inflater);
    boolean onOptionsItemSelected(Fragment fragment, MenuItem item);
    void setState(int state);

}
