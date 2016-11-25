package com.soundcloud.android.storage;

import android.provider.BaseColumns;

@Deprecated // use the new `Tables` structure
public enum Table implements com.soundcloud.propeller.schema.Table {
    SoundStream(false, DatabaseSchema.DATABASE_CREATE_SOUNDSTREAM),
    PromotedTracks(false, DatabaseSchema.DATABASE_CREATE_PROMOTED_TRACKS),
    Activities(false, DatabaseSchema.DATABASE_CREATE_ACTIVITIES),
    PlaylistTracks(PrimaryKey.of(
            TableColumns.PlaylistTracks._ID,
            TableColumns.PlaylistTracks.POSITION,
            TableColumns.PlaylistTracks.PLAYLIST_ID),
                   false, DatabaseSchema.DATABASE_CREATE_PLAYLIST_TRACKS),

    Collections(PrimaryKey.of(TableColumns.Collections.URI), false, DatabaseSchema.DATABASE_CREATE_COLLECTIONS),

    Waveforms(PrimaryKey.of(TableColumns.Waveforms.TRACK_ID), false, DatabaseSchema.DATABASE_CREATE_WAVEFORMS),

    // views
    SoundView(true, DatabaseSchema.DATABASE_CREATE_SOUND_VIEW),
    SoundStreamView(true, DatabaseSchema.DATABASE_CREATE_SOUNDSTREAM_VIEW),
    ActivityView(true, DatabaseSchema.DATABASE_CREATE_ACTIVITY_VIEW),
    PlaylistTracksView(true, DatabaseSchema.DATABASE_CREATE_PLAYLIST_TRACKS_VIEW);


    public final PrimaryKey primaryKey;
    public final String createString;
    public final String id;
    public final String type;
    public final boolean view;
    public static final String TAG = DatabaseManager.TAG;

    Table(boolean view, String create) {
        this(PrimaryKey.of(BaseColumns._ID), view, create);
    }

    Table(PrimaryKey primaryKey, boolean view, String create) {
        this.primaryKey = primaryKey;
        this.view = view;
        if (create != null) {
            createString = buildCreateString(name(), create, view);
        } else {
            createString = null;
        }
        id = this.name() + "." + BaseColumns._ID;
        type = this.name() + "." + TableColumns.ResourceTable._TYPE;
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
