package com.soundcloud.android.provider;

import android.provider.BaseColumns;

import java.util.EnumSet;

public enum Table {
    TRACKS("Tracks", DBHelper.DATABASE_CREATE_TRACKS),
    TRACK_PLAYS("TrackPlays", DBHelper.DATABASE_CREATE_TRACK_PLAYS),
    USERS("Users", DBHelper.DATABASE_CREATE_USERS),
    COMMENTS("Comments", DBHelper.DATABASE_CREATE_COMMENTS),
    ACTIVITIES("Activities", DBHelper.DATABASE_CREATE_ACTIVITIES),
    RECORDINGS("Recordings", DBHelper.DATABASE_CREATE_RECORDINGS),
    SEARCHES("Searches", DBHelper.DATABASE_CREATE_SEARCHES),
    TRACK_VIEW("TrackView", DBHelper.DATABASE_CREATE_TRACK_VIEW),

    COLLECTION_ITEMS("CollectionItems", DBHelper.DATABASE_CREATE_COLLECTION_ITEMS),
    COLLECTIONS("Collections", DBHelper.DATABASE_CREATE_COLLECTIONS),
    COLLECTION_PAGES("CollectionPages", DBHelper.DATABASE_CREATE_COLLECTION_PAGES),

    PLAYLIST("Playlist", DBHelper.DATABASE_CREATE_PLAYLIST),
    PLAYLIST_ITEMS("PlaylistItems", DBHelper.DATABASE_CREATE_PLAYLIST_ITEMS);

    public final String name;
    public final String createString;
    public final String id;

    Table(String name, String create) {
        if (name == null || create == null) throw new NullPointerException();
        this.name = name;
        createString = create;
        id = this.name +"."+BaseColumns._ID;
    }

    public static Table get(String name) {
        EnumSet<Table> tables = EnumSet.allOf(Table.class);
        for (Table table : tables) {
            if (table.name.equals(name)) return table;
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
