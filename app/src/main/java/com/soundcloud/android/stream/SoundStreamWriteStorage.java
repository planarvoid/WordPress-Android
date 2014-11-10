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

public class SoundStreamWriteStorage {

    private final PropellerDatabase database;

    @Inject
    public SoundStreamWriteStorage(PropellerDatabase database) {
        this.database = database;
    }

    public TxnResult replaceStreamItems(Iterable<ApiStreamItem> streamItems) {
        return database.runTransaction(new ReplaceTransaction(streamItems));
    }

    public TxnResult insertStreamItems(Iterable<ApiStreamItem> streamItems) {
        return database.runTransaction(new InsertTransaction(streamItems));
    }

    private static class ReplaceTransaction extends InsertTransaction {

        ReplaceTransaction(Iterable<ApiStreamItem> streamItems) {
            super(streamItems);
        }

        @Override
        public void steps(PropellerDatabase propeller) {
            // clear DB first
            step(propeller.delete(Table.SoundStream));
            super.steps(propeller);
        }
    }

    private static class InsertTransaction extends PropellerDatabase.Transaction {

        private final Iterable<ApiStreamItem> streamItems;

        InsertTransaction(Iterable<ApiStreamItem> streamItems) {
            this.streamItems = streamItems;
        }

        @Override
        public void steps(PropellerDatabase propeller) {
            for (ApiStreamItem streamItem : streamItems) {
                step(propeller.insert(Table.SoundStream, buildSoundStreamContentValues(streamItem)));
                step(propeller.upsert(Table.Sounds, getContentValuesForSoundTable(streamItem)));
                step(propeller.upsert(Table.Users, getContentValuesForSoundOwner(streamItem)));

                final Optional<ApiUser> reposter = streamItem.getReposter();
                if (reposter.isPresent()){
                    step(propeller.upsert(Table.Users, buildUserContentValues(reposter.get())));
                }
            }
        }

        private ContentValues getContentValuesForSoundOwner(ApiStreamItem streamItem) {
            final Optional<ApiTrack> track = streamItem.getTrack();
            if (track.isPresent()){
                return buildUserContentValues(track.get().getUser());
            } else {
                return buildUserContentValues(streamItem.getPlaylist().get().getUser());
            }

        }

        private ContentValues getContentValuesForSoundTable(ApiStreamItem streamItem) {
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
                    .put(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime());

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
    }
}
