package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class StoreProfileCommand extends Command<ApiUserProfile, Boolean> {
    private final WriteMixedRecordsCommand writeMixedRecordsCommand;

    @Inject
    protected StoreProfileCommand(WriteMixedRecordsCommand writeMixedRecordsCommand) {
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
    }

    @Override
    public Boolean call(ApiUserProfile profile) {
        final ModelCollection<ApiPlayableSource> defaultPlayableSourceModelCollection = new ModelCollection<>();
        final List<RecordHolder> emptyList = Collections.emptyList();

        final Optional<ModelCollection<ApiTrackPost>> tracks = profile.getTracks();
        final Optional<ModelCollection<ApiPlaylistPost>> releases = profile.getReleases();
        final Optional<ModelCollection<ApiPlaylistPost>> playlists = profile.getPlaylists();

        Iterable<RecordHolder> entities = Iterables.concat(
                Collections.singletonList(profile.getUser()),
                TO_RECORD_HOLDERS(profile.getSpotlight().or(defaultPlayableSourceModelCollection)),
                tracks.isPresent() ? tracks.get().getCollection() : emptyList,
                releases.isPresent() ? releases.get().getCollection() : emptyList,
                playlists.isPresent() ? playlists.get().getCollection() : emptyList,
                TO_RECORD_HOLDERS(profile.getReposts().or(defaultPlayableSourceModelCollection)),
                TO_RECORD_HOLDERS(profile.getLikes().or(defaultPlayableSourceModelCollection))
                );

        return writeMixedRecordsCommand.call(entities);
    }

    private static List<RecordHolder> TO_RECORD_HOLDERS(ModelCollection<? extends ApiEntityHolderSource> entityHolderSources) {
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
