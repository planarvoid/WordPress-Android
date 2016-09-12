package com.soundcloud.android.collection;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.collection.playhistory.PlayHistoryActivity;
import com.soundcloud.android.collection.playhistory.PlayHistoryFragment;
import com.soundcloud.android.collection.playlists.PlaylistsActivity;
import com.soundcloud.android.collection.playlists.PlaylistsFragment;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedActivity;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedFragment;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                CollectionFragment.class,
                CollectionPreviewView.class,
                ConfirmRemoveOfflineDialogFragment.class,
                PlayHistoryActivity.class,
                PlayHistoryFragment.class,
                PlaylistsActivity.class,
                PlaylistsFragment.class,
                RecentlyPlayedActivity.class,
                RecentlyPlayedFragment.class
        }
)
public class CollectionModule {
}
