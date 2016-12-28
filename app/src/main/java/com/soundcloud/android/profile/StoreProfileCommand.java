package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StoreProfileCommand extends Command<UserProfileRecord, Boolean> {
    private final WriteMixedRecordsCommand writeMixedRecordsCommand;

    @Inject
    protected StoreProfileCommand(WriteMixedRecordsCommand writeMixedRecordsCommand) {
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
    }

    @Override
    public Boolean call(UserProfileRecord profile) {
        final ModelCollection<? extends ApiEntityHolderSource> spotlight = profile.getSpotlight();
        final ModelCollection<? extends ApiEntityHolder> tracks = profile.getTracks();
        final ModelCollection<? extends ApiEntityHolder> albums = profile.getAlbums();
        final ModelCollection<? extends ApiEntityHolder> playlists = profile.getPlaylists();
        final ModelCollection<? extends ApiEntityHolderSource> reposts = profile.getReposts();
        final ModelCollection<? extends ApiEntityHolderSource> likes = profile.getLikes();

        Iterable<RecordHolder> entities = Iterables.concat(
                Collections.singletonList((RecordHolder) profile.getUser()),
                TO_RECORD_HOLDERS(spotlight),
                tracks.getCollection(),
                albums.getCollection(),
                playlists.getCollection(),
                TO_RECORD_HOLDERS(reposts),
                TO_RECORD_HOLDERS(likes)
        );

        return writeMixedRecordsCommand.call(entities);
    }

    public static List<RecordHolder> TO_RECORD_HOLDERS(ModelCollection<? extends ApiEntityHolderSource> entityHolderSources) {
        List<RecordHolder> recordHolders = new ArrayList<>();

        for (ApiEntityHolderSource entityHolderSource : entityHolderSources) {
            final Optional<ApiEntityHolder> entityHolder = entityHolderSource.getEntityHolder();

            if (entityHolder.isPresent()) {
                recordHolders.add(entityHolder.get());
            }
        }

        return recordHolders;
    }
}
