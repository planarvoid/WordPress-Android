package com.soundcloud.android.tracks;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class TrackInfoActivity extends ScActivity {

    static final String LOG_TAG = "TrackInfo";
    public static final String EXTRA_URN = TrackInfoFragment.EXTRA_URN;

    public static void start(Context context, TrackUrn trackUrn) {
        context.startActivity(getIntent(trackUrn));
    }

    public static Intent getIntent(@NotNull TrackUrn trackUrn) {
        Intent intent = new Intent(Actions.TRACK);
        return intent.putExtra(EXTRA_URN, trackUrn);
    }

    public TrackInfoActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.activity_title_track);
        if (savedInstanceState == null) {
            createFragmentForPlaylist();
        }
    }

    private void createFragmentForPlaylist() {
        Bundle extras = getIntent().getExtras();
        Log.d(LOG_TAG, "(Re-)creating fragment for " + extras.getParcelable(EXTRA_URN));
        Fragment fragment = TrackInfoFragment.create(extras);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.PLAYER_INFO.get());
        }
    }
}
