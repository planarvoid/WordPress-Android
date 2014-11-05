package com.soundcloud.android.stream;

import static com.soundcloud.android.playlists.PlaylistWriteStorage.buildPlaylistContentValues;
import static com.soundcloud.android.tracks.TrackWriteStorage.buildTrackContentValues;
import static com.soundcloud.android.users.UserWriteStorage.buildUserContentValues;

import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class SoundStreamWriteStorage {

    private final PropellerDatabase database;

    @Inject
    public SoundStreamWriteStorage(PropellerDatabase database) {
        this.database = database;
    }

    public TxnResult replaceStreamItems(List<ApiStreamItem> streamItems) {
        return database.runTransaction(replaceStreamItemsTransaction(streamItems));
    }

    private PropellerDatabase.Transaction replaceStreamItemsTransaction(final Collection<ApiStreamItem> streamItems) {
        return new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {

                step(propeller.delete(Table.SOUNDSTREAM.name));

                for (ApiStreamItem streamItem : streamItems) {
                    step(propeller.insert(Table.SOUNDSTREAM.name, buildSoundStreamContentValues(streamItem)));
                    step(propeller.upsert(Table.SOUNDS.name, TableColumns.Sounds._ID, getSoundTableContentValues(streamItem)));
                    final Optional<ApiUser> reposter = streamItem.getReposter();
                    if (reposter.isPresent()){
                        step(propeller.upsert(Table.USERS.name, TableColumns.Users._ID, buildUserContentValues(reposter.get())));
                    }

                }
            }
        };
    }

    private ContentValues getSoundTableContentValues(ApiStreamItem streamItem) {
        final Optional<ApiTrack> track = streamItem.getTrack();
        if (track.isPresent()){
            return buildTrackContentValues(track.get());
        } else {
            return buildPlaylistContentValues(streamItem.getPlaylist().get());
        }
    }

    private ContentValues buildSoundStreamContentValues(ApiStreamItem streamItem) {

        final ContentValuesBuilder builder = ContentValuesBuilder.values()
                .put(TableColumns.SoundStream.SOUND_ID, getSoundId(streamItem))
                .put(TableColumns.SoundStream.SOUND_TYPE, getSoundType(streamItem))
                .put(TableColumns.SoundStream.CREATED_AT, getCreatedAt(streamItem));

        final Optional<ApiUser> reposter = streamItem.getReposter();
        if (reposter.isPresent()){
            builder.put(TableColumns.SoundStream.REPOSTER_ID, reposter.get().getId());
        }
        return builder.get();
    }

    private int getSoundType(ApiStreamItem streamItem) {
        return streamItem.getTrack().isPresent() ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST;
    }

    private long getSoundId(ApiStreamItem streamItem) {
        final Optional<ApiTrack> track = streamItem.getTrack();
        return track.isPresent() ? track.get().getId() : streamItem.getPlaylist().get().getId();
    }

    private long getCreatedAt(ApiStreamItem streamItem) {
        return streamItem.getCreatedAt().getTime();
    }
}
