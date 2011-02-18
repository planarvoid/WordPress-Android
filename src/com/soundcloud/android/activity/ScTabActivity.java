package com.soundcloud.android.activity;

import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TabHost;
import com.soundcloud.android.CloudUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.service.CloudPlaybackService;
import com.soundcloud.android.view.ScTabView;

import static com.soundcloud.android.SoundCloudApplication.TAG;

public class ScTabActivity extends TabActivity {

    protected Boolean mIgnorePlaybackStatus = false;

    private MenuItem menuCurrentPlayingItem;

    private MenuItem menuCurrentUploadingItem;


    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.cloudtabs);

        TabHost host = getTabHost();
        TabHost.TabSpec spec;
        spec = host.newTabSpec("profile").setIndicator(
                getString(R.string.tab_you),
                getResources().getDrawable(R.drawable.ic_tab_you));
        spec.setContent(new Intent(this, UserBrowser.class));

        host.addTab(spec);

        spec = host.newTabSpec("record").setIndicator(
                getString(R.string.tab_record),
                getResources().getDrawable(R.drawable.ic_tab_record));

        spec.setContent(new Intent(this, ScCreate.class));

        host.addTab(spec);


        spec = host.newTabSpec("search").setIndicator(
                getString(R.string.tab_search),
                getResources().getDrawable(R.drawable.ic_tab_search));

        spec.setContent(new Intent(this, ScSearch.class));

        host.addTab(spec);


        host.setCurrentTab(0);
    }



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

        menu.add(menu.size(), CloudUtils.OptionsMenu.REFRESH, 0, R.string.menu_refresh).setIcon(
                R.drawable.context_refresh);
        return super.onCreateOptionsMenu(menu);
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
            /* if (mCreateService.isUploading()) { XXX */
            if (false) {
                menuCurrentUploadingItem.setVisible(true);
            } else {
                menuCurrentUploadingItem.setVisible(false);
            }
        } catch (Exception e) {
            menuCurrentUploadingItem.setVisible(false);
        }

        return true;
    }

@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CloudUtils.OptionsMenu.REFRESH:

                ((ScActivity)getCurrentActivity()).onRefresh(false);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

     @Override
    protected void onDestroy() {
        super.onDestroy();

    }

}
