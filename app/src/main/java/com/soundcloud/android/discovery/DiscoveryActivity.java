package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.search.PlaylistTagsFragment;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class DiscoveryActivity extends ScActivity implements PlaylistTagsFragment.PlaylistTagsFragmentListener {

    @Inject @LightCycle PlayerController playerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.activity_title_discovery);

        if (savedInstanceState == null) {
            createFragmentForDiscovery();
        }
    }

    private void createFragmentForDiscovery() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new DiscoveryFragment()).commit();
    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayout();
    }

    @Override
    public void onTagsScrolled() {
        //TODO: This needs to be refactored
    }

    @Override
    public void onTagSelected(String tag) {
        //TODO: This needs to be refactored
    }
}
