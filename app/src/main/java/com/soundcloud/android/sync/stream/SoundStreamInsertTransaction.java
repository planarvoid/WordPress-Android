package com.soundcloud.android.sync.stream;

import static com.soundcloud.android.commands.StorePlaylistsCommand.buildPlaylistContentValues;
import static com.soundcloud.android.commands.StoreTracksCommand.buildTrackContentValues;
import static com.soundcloud.android.commands.StoreUsersCommand.buildUserContentValues;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;

import android.content.ContentValues;

class SoundStreamInsertTransaction extends PropellerDatabase.Transaction {

    private final boolean clearTable;
    private final Iterable<ApiStreamItem> streamItems;

    SoundStreamInsertTransaction(boolean clearTable, Iterable<ApiStreamItem> streamItems) {
        this.clearTable = clearTable;
        this.streamItems = streamItems;
    }

    @Override
    public void steps(PropellerDatabase propeller) {
        if (clearTable) {
            step(propeller.delete(Table.SoundStream));
        }
        for (ApiStreamItem streamItem : streamItems) {
            step(propeller.upsert(Table.Sounds, getContentValuesForSoundTable(streamItem)));
            step(propeller.upsert(Table.Users, getContentValuesForSoundOwner(streamItem)));

            final Optional<ApiUser> reposter = streamItem.getReposter();
            if (reposter.isPresent()){
                step(propeller.upsert(Table.Users, buildUserContentValues(reposter.get())));
            }

            if (streamItem.isPromotedStreamItem()){
                final Optional<ApiUser> promoter = streamItem.getPromoter();
                if (promoter.isPresent()){
                    step(propeller.upsert(Table.Users, buildUserContentValues(promoter.get())));
                }

                InsertResult insertResult = step(propeller.insert(Table.PromotedTracks, buildPromotedContentValues(streamItem)));
                step(propeller.insert(Table.SoundStream, buildSoundStreamContentValues(streamItem, insertResult.getRowId()).get()));
            } else {
                step(propeller.insert(Table.SoundStream, buildSoundStreamContentValues(streamItem).get()));
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

    private ContentValuesBuilder buildSoundStreamContentValues(ApiStreamItem streamItem) {
        final ContentValuesBuilder builder = ContentValuesBuilder.values()
                .put(TableColumns.SoundStream.SOUND_ID, getSoundId(streamItem))
                .put(TableColumns.SoundStream.SOUND_TYPE, getSoundType(streamItem))
                .put(TableColumns.SoundStream.CREATED_AT, streamItem.getCreatedAtTime());

        final Optional<ApiUser> reposter = streamItem.getReposter();
        if (reposter.isPresent()){
            builder.put(TableColumns.SoundStream.REPOSTER_ID, reposter.get().getId());
        }
        return builder;
    }

    private ContentValuesBuilder buildSoundStreamContentValues(ApiStreamItem streamItem, long promotedId) {
        ContentValuesBuilder builder = buildSoundStreamContentValues(streamItem);
        builder.put(TableColumns.SoundStream.PROMOTED_ID, promotedId);
        return builder;
    }

    private ContentValues buildPromotedContentValues(ApiStreamItem streamItem) {
        final Joiner urlJoiner = Joiner.on(" ");

        final ContentValuesBuilder builder = ContentValuesBuilder.values()
                .put(TableColumns.PromotedTracks.URN, streamItem.getPromotedUrn().get().toString())
                .put(TableColumns.PromotedTracks.TRACKING_PROFILE_CLICKED_URLS, urlJoiner.join(streamItem.getTrackingProfileClickedUrls()))
                .put(TableColumns.PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS, urlJoiner.join(streamItem.getTrackingPromoterClickedUrls()))
                .put(TableColumns.PromotedTracks.TRACKING_TRACK_CLICKED_URLS, urlJoiner.join(streamItem.getTrackingTrackClickedUrls()))
                .put(TableColumns.PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS, urlJoiner.join(streamItem.getTrackingTrackImpressionUrls()))
                .put(TableColumns.PromotedTracks.TRACKING_TRACK_PLAYED_URLS, urlJoiner.join(streamItem.getTrackingTrackPlayedUrls()));

        final Optional<ApiUser> promoter = streamItem.getPromoter();
        if (promoter.isPresent()){
            builder.put(TableColumns.PromotedTracks.PROMOTER_ID, promoter.get().getId());
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
