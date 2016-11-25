package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;

import com.soundcloud.android.utils.ErrorUtils;

import javax.inject.Inject;

public class MyLikesStateProvider {

    private final LoadLikesPendingAdditionCommand loadLikesPendingAdditionCommand;
    private final LoadLikesPendingRemovalCommand loadLikesPendingRemovalCommand;

    @Inject
    public MyLikesStateProvider(LoadLikesPendingAdditionCommand loadLikesPendingAdditionCommand,
                                LoadLikesPendingRemovalCommand loadLikesPendingRemovalCommand) {
        this.loadLikesPendingAdditionCommand = loadLikesPendingAdditionCommand;
        this.loadLikesPendingRemovalCommand = loadLikesPendingRemovalCommand;
    }

    public boolean hasLocalChanges() {
        try {
            return !loadLikesPendingAdditionCommand.call(TYPE_TRACK).isEmpty()
                    || !loadLikesPendingAdditionCommand.call(TYPE_PLAYLIST).isEmpty()
                    || !loadLikesPendingRemovalCommand.call(TYPE_TRACK).isEmpty()
                    || !loadLikesPendingRemovalCommand.call(TYPE_PLAYLIST).isEmpty();

        } catch (Exception e) {
            ErrorUtils.handleSilentException(e);
        }
        return false;
    }
}
