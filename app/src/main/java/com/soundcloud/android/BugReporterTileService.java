package com.soundcloud.android;

import com.soundcloud.android.utils.BugReporter;

import android.app.AlertDialog;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;

import javax.inject.Inject;

@RequiresApi(api = Build.VERSION_CODES.N)
public class BugReporterTileService extends TileService {

    @Inject BugReporter bugReporter;

    public BugReporterTileService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onClick() {
        AlertDialog feedbackDialog = bugReporter.getFeedbackDialog(getApplicationContext(), R.array.feedback_all);
        this.showDialog(feedbackDialog);
    }

    @Override
    public void onTileAdded() {
        setCurrentState(Tile.STATE_ACTIVE);
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        setCurrentState(Tile.STATE_INACTIVE);
        super.onTileRemoved();
    }

    private void setCurrentState(int state) {
        Tile tile = getQsTile();
        tile.setState(state);
        tile.updateTile();
    }

}