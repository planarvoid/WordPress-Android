package com.soundcloud.android.sync.activities;

import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.comments.CommentRecord;
import com.soundcloud.android.comments.StoreCommentCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.Activities;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

class StoreActivitiesCommand extends DefaultWriteStorageCommand<Iterable<ApiActivityItem>, TxnResult> {

    private final StoreUsersCommand storeUsersCommand;
    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreCommentCommand storeCommentCommand;

    @Inject
    StoreActivitiesCommand(PropellerDatabase propeller,
                           StoreUsersCommand storeUsersCommand,
                           StoreTracksCommand storeTracksCommand,
                           StorePlaylistsCommand storePlaylistsCommand,
                           StoreCommentCommand storeCommentCommand) {
        super(propeller);
        this.storeUsersCommand = storeUsersCommand;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeCommentCommand = storeCommentCommand;
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, Iterable<ApiActivityItem> input) {
        return propeller.runTransaction(new StoreActivitiesTransaction(input));
    }

    protected class StoreActivitiesTransaction extends PropellerDatabase.Transaction {

        private final Iterable<ApiActivityItem> activities;

        protected StoreActivitiesTransaction(Iterable<ApiActivityItem> activities) {
            this.activities = activities;
        }

        @Override
        public void steps(PropellerDatabase propeller) {
            storeDependencies();

            for (ApiActivityItem activityItem : activities) {
                handleLikeActivity(propeller, activityItem);
                handleRepostActivity(propeller, activityItem);
                handleCommentActivity(propeller, activityItem);
                handleFollowActivity(propeller, activityItem);
            }
        }

        private void handleFollowActivity(PropellerDatabase propeller, ApiActivityItem activityItem) {
            final Optional<ApiUserFollowActivity> maybeFollowActivity = activityItem.getFollow();
            if (maybeFollowActivity.isPresent()) {
                insert(propeller, valuesFor(maybeFollowActivity.get(), ActivityKind.USER_FOLLOW));
            }
        }

        private void handleCommentActivity(PropellerDatabase propeller, ApiActivityItem activityItem) {
            final Optional<ApiTrackCommentActivity> maybeCommentActivity = activityItem.getTrackComment();
            if (maybeCommentActivity.isPresent()) {
                final ApiTrackCommentActivity commentActivity = maybeCommentActivity.get();
                // insert comment
                final CommentRecord comment = commentActivity.getComment();
                step(storeCommentCommand.call(comment));
                // insert activity
                final Urn playableUrn = comment.getTrackUrn();
                insert(propeller, valuesFor(
                        commentActivity, ActivityKind.TRACK_COMMENT, playableUrn)
                        .put(Activities.COMMENT_ID, storeCommentCommand.lastRowId()));
            }
        }

        private void handleLikeActivity(PropellerDatabase propeller, ApiActivityItem activityItem) {
            final Optional<ApiEngagementActivity> maybeLike = activityItem.getLike();
            if (maybeLike.isPresent()) {
                final ApiEngagementActivity like = maybeLike.get();
                final Urn playableUrn = like.getTargetUrn();
                insert(propeller, valuesFor(
                        like,
                        playableUrn.isTrack() ? ActivityKind.TRACK_LIKE : ActivityKind.PLAYLIST_LIKE,
                        playableUrn
                ));
            }
        }

        private void handleRepostActivity(PropellerDatabase propeller, ApiActivityItem activityItem) {
            final Optional<ApiEngagementActivity> maybeRepost = activityItem.getRepost();
            if (maybeRepost.isPresent()) {
                final ApiEngagementActivity repost = maybeRepost.get();
                final Urn playableUrn = repost.getTargetUrn();
                insert(propeller, valuesFor(repost,
                                            playableUrn.isTrack() ?
                                            ActivityKind.TRACK_REPOST :
                                            ActivityKind.PLAYLIST_REPOST,
                                            playableUrn
                ));
            }
        }

        private void storeDependencies() {
            final Set<UserRecord> users = new HashSet<>();
            final Set<TrackRecord> tracks = new HashSet<>();
            final Set<PlaylistRecord> playlists = new HashSet<>();

            for (ApiActivityItem activityItem : activities) {
                users.addAll(activityItem.getUser().asSet());
                tracks.addAll(activityItem.getTrack().asSet());
                playlists.addAll(activityItem.getPlaylist().asSet());
            }
            if (!users.isEmpty()) {
                step(storeUsersCommand.call(users));
            }
            if (!tracks.isEmpty()) {
                step(storeTracksCommand.call(tracks));
            }
            if (!playlists.isEmpty()) {
                step(storePlaylistsCommand.call(playlists));
            }
        }

        private ContentValuesBuilder valuesFor(ApiActivity activity, ActivityKind activityKind) {
            final ContentValuesBuilder builder = ContentValuesBuilder.values();
            builder.put(Activities.TYPE, activityKind.identifier());
            builder.put(Activities.USER_ID, activity.getUserUrn().getNumericId());
            builder.put(Activities.CREATED_AT, activity.getCreatedAt().getTime());
            return builder;
        }

        private ContentValuesBuilder valuesFor(ApiActivity activity, ActivityKind activityKind, Urn playableUrn) {
            return valuesFor(activity, activityKind)
                    .put(Activities.SOUND_ID, playableUrn.getNumericId())
                    .put(Activities.SOUND_TYPE, playableUrn.isTrack()
                                                ? Tables.Sounds.TYPE_TRACK
                                                : Tables.Sounds.TYPE_PLAYLIST);
        }

        private void insert(PropellerDatabase propeller, ContentValuesBuilder builder) {
            step(propeller.insert(Table.Activities, builder.get()));
        }
    }
}
