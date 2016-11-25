package com.soundcloud.android.sync.stream;

import static com.soundcloud.propeller.query.Filter.filter;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Joiner;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ScalarMapper;
import com.soundcloud.propeller.query.Query;

import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

@AutoFactory(allowSubclasses = true)
class SoundStreamInsertTransaction extends PropellerDatabase.Transaction {

    private final Iterable<ApiStreamItem> streamItems;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;
    private final StoreTracksCommand storeTracksCommand;

    SoundStreamInsertTransaction(Iterable<ApiStreamItem> streamItems,
                                 @Provided StoreUsersCommand storeUsersCommand,
                                 @Provided StoreTracksCommand storeTracksCommand,
                                 @Provided StorePlaylistsCommand storePlaylistsCommand) {
        this.streamItems = streamItems;
        this.storeUsersCommand = storeUsersCommand;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
    }

    @Override
    public void steps(PropellerDatabase propeller) {
        beforeInserts(propeller);
        bulkInsertDependencies();

        for (ApiStreamItem streamItem : streamItems) {
            if (streamItem.isPromotedStreamItem()) {
                InsertResult insertResult = step(propeller.insert(Table.PromotedTracks,
                                                                  buildPromotedContentValues(streamItem)));
                step(propeller.insert(Table.SoundStream,
                                      buildSoundStreamContentValues(streamItem, insertResult.getRowId()).get()));
            } else {
                step(propeller.insert(Table.SoundStream, buildSoundStreamContentValues(streamItem).get()));
            }
        }
    }

    private void bulkInsertDependencies() {
        ArrayList<UserRecord> users = new ArrayList<>();
        ArrayList<TrackRecord> tracks = new ArrayList<>();
        ArrayList<PlaylistRecord> playlists = new ArrayList<>();

        for (ApiStreamItem streamItem : streamItems) {

            final Optional<ApiTrack> trackOptional = streamItem.getTrack();
            if (trackOptional.isPresent()) {
                ApiTrack track = trackOptional.get();
                tracks.add(track);
                users.add(track.getUser());
            } else {
                ApiPlaylist playlist = streamItem.getPlaylist().get();
                playlists.add(playlist);
                users.add(playlist.getUser());
            }

            final Optional<ApiUser> reposter = streamItem.getReposter();
            if (reposter.isPresent()) {
                users.add(reposter.get());
            }

            if (streamItem.isPromotedStreamItem()) {
                final Optional<ApiUser> promoter = streamItem.getPromoter();
                if (promoter.isPresent()) {
                    users.add(promoter.get());
                }
            }
        }
        step(storeUsersCommand.call(users));
        step(storeTracksCommand.call(tracks));
        step(storePlaylistsCommand.call(playlists));
    }

    protected void beforeInserts(PropellerDatabase propeller) {
        List<Long> promotedStreamIds = propeller.query(Query.from(Table.SoundStream.name())
                                                            .select(TableColumns.SoundStream.PROMOTED_ID)
                                                            .whereNotNull(TableColumns.SoundStream.PROMOTED_ID))
                                                .toList(ScalarMapper.create(Long.class));
        step(propeller.delete(Table.SoundStream, filter().whereNotNull(TableColumns.SoundStream.PROMOTED_ID)));
        step(propeller.delete(Table.PromotedTracks,
                              filter().whereIn(TableColumns.PromotedTracks._ID, promotedStreamIds)));
    }

    private ContentValuesBuilder buildSoundStreamContentValues(ApiStreamItem streamItem) {
        final ContentValuesBuilder builder = ContentValuesBuilder.values()
                                                                 .put(TableColumns.SoundStream.SOUND_ID,
                                                                      getSoundId(streamItem))
                                                                 .put(TableColumns.SoundStream.SOUND_TYPE,
                                                                      getSoundType(streamItem))
                                                                 .put(TableColumns.SoundStream.CREATED_AT,
                                                                      streamItem.getCreatedAtTime());

        final Optional<ApiUser> reposter = streamItem.getReposter();
        if (reposter.isPresent()) {
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
        final Joiner urlJoiner = Strings.joinOn(" ");

        final ContentValuesBuilder builder = ContentValuesBuilder.values()
                                                                 .put(TableColumns.PromotedTracks.AD_URN,
                                                                      streamItem.getAdUrn().get())
                                                                 .put(TableColumns.PromotedTracks.CREATED_AT,
                                                                      System.currentTimeMillis())
                                                                 .put(TableColumns.PromotedTracks.TRACKING_PROFILE_CLICKED_URLS,
                                                                      urlJoiner.join(streamItem.getTrackingProfileClickedUrls()))
                                                                 .put(TableColumns.PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS,
                                                                      urlJoiner.join(streamItem.getTrackingPromoterClickedUrls()))
                                                                 .put(TableColumns.PromotedTracks.TRACKING_TRACK_CLICKED_URLS,
                                                                      urlJoiner.join(streamItem.getTrackingItemClickedUrls()))
                                                                 .put(TableColumns.PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS,
                                                                      urlJoiner.join(streamItem.getTrackingItemImpressionUrls()))
                                                                 .put(TableColumns.PromotedTracks.TRACKING_TRACK_PLAYED_URLS,
                                                                      urlJoiner.join(streamItem.getTrackingTrackPlayedUrls()));

        final Optional<ApiUser> promoter = streamItem.getPromoter();
        if (promoter.isPresent()) {
            builder.put(TableColumns.PromotedTracks.PROMOTER_ID, promoter.get().getId());
            builder.put(TableColumns.PromotedTracks.PROMOTER_NAME, promoter.get().getUsername());
        }
        return builder.get();
    }

    private int getSoundType(ApiStreamItem streamItem) {
        return streamItem.getTrack().isPresent() ? Tables.Sounds.TYPE_TRACK : Tables.Sounds.TYPE_PLAYLIST;
    }

    private long getSoundId(ApiStreamItem streamItem) {
        final Optional<ApiTrack> track = streamItem.getTrack();
        return track.isPresent() ? track.get().getId() : streamItem.getPlaylist().get().getId();
    }
}
