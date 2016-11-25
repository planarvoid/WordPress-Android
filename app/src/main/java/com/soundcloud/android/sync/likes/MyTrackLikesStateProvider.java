package com.soundcloud.android.sync.likes;

import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;

import com.soundcloud.android.utils.ErrorUtils;

import javax.inject.Inject;

public class MyTrackLikesStateProvider {

    private final LoadLikesPendingAdditionCommand loadLikesPendingAdditionCommand;
    private final LoadLikesPendingRemovalCommand loadLikesPendingRemovalCommand;

    @Inject
    public MyTrackLikesStateProvider(LoadLikesPendingAdditionCommand loadLikesPendingAdditionCommand,
                                     LoadLikesPendingRemovalCommand loadLikesPendingRemovalCommand) {
        this.loadLikesPendingAdditionCommand = loadLikesPendingAdditionCommand;
        this.loadLikesPendingRemovalCommand = loadLikesPendingRemovalCommand;
    }

    public boolean hasLocalChanges() {
        try {
            return !loadLikesPendingAdditionCommand.call(TYPE_TRACK).isEmpty()
                    || !loadLikesPendingRemovalCommand.call(TYPE_TRACK).isEmpty();

        } catch (Exception e) {
            ErrorUtils.handleSilentException(e);
        }
        return false;
    }
}
