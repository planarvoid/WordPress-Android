package com.soundcloud.android.sync.activities;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.TableColumns.Activities;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

class StoreActivitiesCommand extends DefaultWriteStorageCommand<Iterable<ApiActivityItem>, TxnResult> {

    @Inject
    StoreActivitiesCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, Iterable<ApiActivityItem> input) {
        return propeller.runTransaction(new StoreActivitiesTransaction(input));
    }

    private static class StoreActivitiesTransaction extends PropellerDatabase.Transaction {

        private final Iterable<ApiActivityItem> activities;

        private StoreActivitiesTransaction(Iterable<ApiActivityItem> activities) {
            this.activities = activities;
        }

        @Override
        public void steps(PropellerDatabase propeller) {
            storeDependencies(propeller);

            for (ApiActivityItem activityItem : activities) {
                handleLikeActivity(propeller, activityItem);
                handleRepostActivity(propeller, activityItem);
            }

        }

        private void handleLikeActivity(PropellerDatabase propeller, ApiActivityItem activityItem) {
            final Optional<ApiEngagementActivity> maybeLike = activityItem.getLike();
            if (maybeLike.isPresent()) {
                final ApiEngagementActivity like = maybeLike.get();
                step(propeller.upsert(Table.Activities, buildContentValues(
                        like.getTargetUrn(), like.getUserUrn(), like.getCreatedAt(),
                        like.getTargetUrn().isTrack() ? ActivityKind.TRACK_LIKE : ActivityKind.PLAYLIST_LIKE
                )));
            }
        }

        private void handleRepostActivity(PropellerDatabase propeller, ApiActivityItem activityItem) {
            final Optional<ApiEngagementActivity> maybeRepost = activityItem.getRepost();
            if (maybeRepost.isPresent()) {
                final ApiEngagementActivity repost = maybeRepost.get();
                step(propeller.upsert(Table.Activities, buildContentValues(
                        repost.getTargetUrn(), repost.getUserUrn(), repost.getCreatedAt(),
                        repost.getTargetUrn().isTrack() ? ActivityKind.TRACK_REPOST : ActivityKind.PLAYLIST_REPOST
                )));
            }
        }

        private void storeDependencies(PropellerDatabase propeller) {
            final Set<UserRecord> users = new HashSet<>();
            final Set<TrackRecord> tracks = new HashSet<>();
            final Set<PlaylistRecord> playlists = new HashSet<>();

            for (ApiActivityItem activityItem : activities) {
                users.addAll(activityItem.getUser().asSet());
                tracks.addAll(activityItem.getTrack().asSet());
                playlists.addAll(activityItem.getPlaylist().asSet());
            }
            if (!users.isEmpty()) {
                step(new StoreUsersCommand(propeller).call(users));
            }
            if (!tracks.isEmpty()) {
                step(new StoreTracksCommand(propeller).call(tracks));
            }
            if (!playlists.isEmpty()) {
                step(new StorePlaylistsCommand(propeller).call(playlists));
            }
        }

        private static ContentValues buildContentValues(Urn targetUrn, Urn userUrn, Date createdAt,
                                                        ActivityKind activityKind) {
            final ContentValues cv = new ContentValues();
            cv.put(Activities.TYPE, activityKind.tableConstant());
            cv.put(Activities.SOUND_ID, targetUrn.getNumericId());
            cv.put(Activities.SOUND_TYPE, targetUrn.isTrack()
                    ? TableColumns.Sounds.TYPE_TRACK
                    : TableColumns.Sounds.TYPE_PLAYLIST);
            cv.put(Activities.USER_ID, userUrn.getNumericId());
            cv.put(Activities.CREATED_AT, createdAt.getTime());
            return cv;
        }

    }
}
