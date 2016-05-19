package com.soundcloud.android.screens.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

import java.util.List;

public abstract class BaseUserPlaylistsScreen extends BaseUserScreen {
    public BaseUserPlaylistsScreen(Han testDriver) {
        super(testDriver);
    }

    public PlaylistDetailsScreen clickFirstPlaylist() {
        final List<ViewElement> playableItems = playableRecyclerView().findOnScreenElements(With.id(R.id.playlist_list_item));
        playableItems.get(0).click();

        return new PlaylistDetailsScreen(testDriver);
    }
}
