package com.soundcloud.android.activity.landing;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.fragment.FriendFinderFragment;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Toast;

public class FriendFinder extends ScActivity implements ScLandingPage {

    public static final String FRAG_TAG = "ff_fragment";
    FriendFinderFragment mFragment;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getSupportActionBar().setTitle(getString(R.string.side_menu_friend_finder));

        if (state == null) {
            mFragment = FriendFinderFragment.newInstance(getApp());
            getSupportFragmentManager().beginTransaction()
                    .add(mRootView.getContentHolderId(), mFragment, FRAG_TAG).commit();
        } else {
            mFragment = (FriendFinderFragment) getSupportFragmentManager().findFragmentByTag(FRAG_TAG);
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_friend_finder;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        switch (requestCode) {
            case Consts.RequestCodes.MAKE_CONNECTION:
                if (resultCode == RESULT_OK) {
                    boolean success = result.getBooleanExtra("success", false);
                    String msg = getString(
                            success ? R.string.connect_success : R.string.connect_failure,
                            result.getStringExtra("service"));
                    Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();

                    if (success) {
                        // this should reload the services and the list should auto refresh
                        // from the content observer
                        startService(new Intent(this, ApiSyncService.class)
                                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                                .setData(Content.ME_CONNECTIONS.uri));

                        if (mFragment != null) {
                            mFragment.setState(FriendFinderFragment.States.LOADING, false);
                        }
                    }
                } else {
                    if (mFragment != null) {
                        mFragment.executeRefreshTask();
                    }
                }
                break;
        }
    }
}
