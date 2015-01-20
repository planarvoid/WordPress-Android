package com.soundcloud.android.actionbar.menu;

import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public interface ActionMenuController {

    void onCreate(Fragment fragment);
    void onCreateOptionsMenu(Menu menu, MenuInflater inflater);
    boolean onOptionsItemSelected(Fragment fragment, MenuItem item);

}
