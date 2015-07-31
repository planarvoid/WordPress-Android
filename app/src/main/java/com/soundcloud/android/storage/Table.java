package com.soundcloud.android.storage;

import android.provider.BaseColumns;

public enum Table implements com.soundcloud.propeller.Table {
    SoundStream(false, DatabaseSchema.DATABASE_CREATE_SOUNDSTREAM),
    PromotedTracks(false, DatabaseSchema.DATABASE_CREATE_PROMOTED_TRACKS),
    Sounds(PrimaryKey.of(TableColumns.Sounds._ID, TableColumns.Sounds._TYPE), false, DatabaseSchema.DATABASE_CREATE_SOUNDS, TableColumns.Sounds.ALL_FIELDS),
    TrackMetadata(false, DatabaseSchema.DATABASE_CREATE_TRACK_METADATA, TableColumns.TrackMetadata.ALL_FIELDS),
    TrackPolicies(PrimaryKey.of(TableColumns.TrackPolicies.TRACK_ID), false, DatabaseSchema.DATABASE_CREATE_TRACK_POLICIES, TableColumns.TrackPolicies.ALL_FIELDS),
    Users(false, DatabaseSchema.DATABASE_CREATE_USERS, TableColumns.Users.ALL_FIELDS),
    Comments(false, DatabaseSchema.DATABASE_CREATE_COMMENTS),
    Activities(false, DatabaseSchema.DATABASE_CREATE_ACTIVITIES),
    Recordings(false, DatabaseSchema.DATABASE_CREATE_RECORDINGS, TableColumns.Recordings.ALL_FIELDS),
    RecommendationSeeds(false, DatabaseSchema.DATABASE_CREATE_RECOMMENDATION_SEEDS),
    Recommendations(false, DatabaseSchema.DATABASE_CREATE_RECOMMENDATIONS),
    Searches(false, DatabaseSchema.DATABASE_CREATE_SEARCHES),
    PlaylistTracks(PrimaryKey.of(
            TableColumns.PlaylistTracks._ID,
            TableColumns.PlaylistTracks.POSITION,
            TableColumns.PlaylistTracks.PLAYLIST_ID),
            false, DatabaseSchema.DATABASE_CREATE_PLAYLIST_TRACKS),
    UserAssociations(false, DatabaseSchema.DATABASE_CREATE_USER_ASSOCIATIONS),

    PlayQueue(false, DatabaseSchema.DATABASE_CREATE_PLAY_QUEUE),

    CollectionItems(PrimaryKey.of(
            TableColumns.CollectionItems.USER_ID,
            TableColumns.CollectionItems.ITEM_ID,
            TableColumns.CollectionItems.COLLECTION_TYPE,
            TableColumns.CollectionItems.RESOURCE_TYPE),
            false, DatabaseSchema.DATABASE_CREATE_COLLECTION_ITEMS),
    Collections(false, DatabaseSchema.DATABASE_CREATE_COLLECTIONS),
    CollectionPages(PrimaryKey.of(
            TableColumns.CollectionPages.COLLECTION_ID,
            TableColumns.CollectionPages.PAGE_INDEX),
            false, DatabaseSchema.DATABASE_CREATE_COLLECTION_PAGES),

    Suggestions(false, DatabaseSchema.DATABASE_CREATE_SUGGESTIONS, TableColumns.Suggestions.ALL_FIELDS),

    TrackDownloads(false, DatabaseSchema.DATABASE_CREATE_TRACK_DOWNLOADS), // download state
    OfflineContent(false, DatabaseSchema.DATABASE_CREATE_OFFLINE_CONTENT), // marked for offline sync (user intent)
    Waveforms(PrimaryKey.of(TableColumns.Waveforms.TRACK_ID), false, DatabaseSchema.DATABASE_CREATE_WAVEFORMS),

    Likes(PrimaryKey.of(TableColumns.Likes._ID, TableColumns.Likes._TYPE), false, DatabaseSchema.DATABASE_CREATE_LIKES),
    Posts(PrimaryKey.of(TableColumns.Posts.TYPE, TableColumns.Posts.TARGET_TYPE, TableColumns.Posts.TARGET_ID), false, DatabaseSchema.DATABASE_CREATE_POSTS),

    // views
    SoundView(true, DatabaseSchema.DATABASE_CREATE_SOUND_VIEW),
    SoundStreamView(true, DatabaseSchema.DATABASE_CREATE_SOUNDSTREAM_VIEW),
    ActivityView(true, DatabaseSchema.DATABASE_CREATE_ACTIVITY_VIEW),
    SoundAssociationView(true, DatabaseSchema.DATABASE_CREATE_SOUND_ASSOCIATION_VIEW),
    UserAssociationView(PrimaryKey.of(
            TableColumns.UserAssociations.OWNER_ID,
            TableColumns.UserAssociations.TARGET_ID,
            TableColumns.UserAssociations.ASSOCIATION_TYPE,
            TableColumns.UserAssociations.RESOURCE_TYPE),
            true, DatabaseSchema.DATABASE_CREATE_USER_ASSOCIATION_VIEW),
    PlaylistTracksView(true, DatabaseSchema.DATABASE_CREATE_PLAYLIST_TRACKS_VIEW);


    public final PrimaryKey primaryKey;
    public final String createString;
    public final String id;
    public final String type;
    public final String[] fields;
    public final boolean view;
    public static final String TAG = DatabaseManager.TAG;

    Table(boolean view, String create, String... fields) {
        this(PrimaryKey.of(BaseColumns._ID), view, create, fields);
    }

    Table(PrimaryKey primaryKey, boolean view, String create, String... fields) {
        this.primaryKey = primaryKey;
        this.view = view;
        if (create != null) {
            createString = buildCreateString(name(), create, view);
        } else {
            createString = null;
        }
        id = this.name() + "." + BaseColumns._ID;
        type = this.name() + "." + TableColumns.ResourceTable._TYPE;
        this.fields = fields;
    }

    public static String buildCreateString(String tableName, String columnString, boolean isView) {
        return "CREATE " + (isView ? "VIEW" : "TABLE") + " IF NOT EXISTS " + tableName + " " + columnString;
    }

    @Override
    public PrimaryKey primaryKey() {
        return primaryKey;
    }

    public String field(String field) {
        return this.name() + "." + field;
    }

    @Override
    public String toString() {
        return name();
    }
}
