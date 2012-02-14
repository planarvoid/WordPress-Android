package com.soundcloud.android.tracking;


/**
 * See
 * <a href="https://docs.google.com/a/soundcloud.com/spreadsheet/ccc?key=0AkJmFQ2aH2kTdHM2MXQzMjZ4blNHT00wNl9vMFMxNmc#gid=1">
 * master document</a> for more information.
 */
public enum Level2 {
    Entry(1),
    Stream(2),
    Activity(3),
    Record(4),
    Search(5),
    Sounds(6),
    Users(7),
    You(8),
    Settings(9);

    public final int id;

    Level2(int id) {
        this.id = id;
    }
}





