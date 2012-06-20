package com.soundcloud.android.activity;

import com.soundcloud.android.R;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by IntelliJ IDEA.
 * User: jschmidt
 * Date: 1/15/12
 * Time: 3:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class ActionActivity extends ScActivity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in action bar clicked; go home
            Intent intent = new Intent(this, Dashboard.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        case R.id.menu_record:
            // app icon in action bar clicked; go home
            intent = new Intent(this, ScCreate.class);
            startActivity(intent);
            return true;
        case R.id.menu_search:
            // app icon in action bar clicked; go home
            intent = new Intent(this, ScSearch.class);
            startActivity(intent);
            return true;
        case R.id.menu_you:
            // app icon in action bar clicked; go home
            intent = new Intent(this, UserBrowser.class);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
    }
}
}
