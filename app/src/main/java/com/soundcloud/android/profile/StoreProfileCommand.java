package com.soundcloud.android.profile;

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
        Iterable<RecordHolder> entities = Iterables.concat(
                Collections.singletonList(profile.getUser()),
                TO_RECORD_HOLDERS(profile.getSpotlight()),
                profile.getTracks().getCollection(),
                profile.getReleases().getCollection(),
                profile.getPlaylists().getCollection(),
                TO_RECORD_HOLDERS(profile.getReposts()),
                TO_RECORD_HOLDERS(profile.getLikes())
                );

        return writeMixedRecordsCommand.call(entities);
    }

    private static List<RecordHolder> TO_RECORD_HOLDERS(ModelCollection<? extends ApiEntityHolderSource> entityHolderSources) {
        List<RecordHolder> entities = new ArrayList<>();

        for (ApiEntityHolderSource entityHolderSource : entityHolderSources) {
            final Optional<ApiEntityHolder> entityHolder = entityHolderSource.getEntityHolder();

            if (entityHolder.isPresent()) {
                entities.add(entityHolder.get());
            }
        }

        return entities;
    }
}
