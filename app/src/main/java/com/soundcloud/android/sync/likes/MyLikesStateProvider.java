package com.soundcloud.android.sync.likes;

import com.soundcloud.android.storage.TableColumns;
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
            return !loadLikesPendingAdditionCommand.with(TableColumns.Sounds.TYPE_TRACK).call().isEmpty()
                    || !loadLikesPendingAdditionCommand.with(TableColumns.Sounds.TYPE_PLAYLIST).call().isEmpty()
                    || !loadLikesPendingRemovalCommand.with(TableColumns.Sounds.TYPE_TRACK).call().isEmpty()
                    || !loadLikesPendingRemovalCommand.with(TableColumns.Sounds.TYPE_PLAYLIST).call().isEmpty();

        } catch (Exception e) {
            ErrorUtils.handleSilentException(e);
        }
        return false;
    }
}
