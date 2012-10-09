package com.soundcloud.android.provider;

import android.database.sqlite.SQLiteDiskIOException;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLiteErrors {
    private static Pattern CODE = Pattern.compile(".* \\(code (\\d+)\\)\\Z");

    // taken from sqlite3.h
    private static final int SQLITE_IOERR                  = 10;
    private static final int SQLITE_IOERR_READ             = (SQLITE_IOERR | (1<<8)) ;
    private static final int SQLITE_IOERR_SHORT_READ       = (SQLITE_IOERR | (2<<8)) ;
    private static final int SQLITE_IOERR_WRITE            = (SQLITE_IOERR | (3<<8)) ;
    private static final int SQLITE_IOERR_FSYNC            = (SQLITE_IOERR | (4<<8)) ;
    private static final int SQLITE_IOERR_DIR_FSYNC        = (SQLITE_IOERR | (5<<8)) ;
    private static final int SQLITE_IOERR_TRUNCATE         = (SQLITE_IOERR | (6<<8)) ;
    private static final int SQLITE_IOERR_FSTAT            = (SQLITE_IOERR | (7<<8)) ;
    private static final int SQLITE_IOERR_UNLOCK           = (SQLITE_IOERR | (8<<8)) ;
    private static final int SQLITE_IOERR_RDLOCK           = (SQLITE_IOERR | (9<<8)) ;
    private static final int SQLITE_IOERR_DELETE           = (SQLITE_IOERR | (10<<8));
    private static final int SQLITE_IOERR_BLOCKED          = (SQLITE_IOERR | (11<<8));
    private static final int SQLITE_IOERR_NOMEM            = (SQLITE_IOERR | (12<<8));
    private static final int SQLITE_IOERR_ACCESS           = (SQLITE_IOERR | (13<<8));
    private static final int SQLITE_IOERR_CHECKRESERVEDLOCK= (SQLITE_IOERR | (14<<8));
    private static final int SQLITE_IOERR_LOCK             = (SQLITE_IOERR | (15<<8));
    private static final int SQLITE_IOERR_CLOSE            = (SQLITE_IOERR | (16<<8));
    private static final int SQLITE_IOERR_DIR_CLOSE        = (SQLITE_IOERR | (17<<8));
    private static final int SQLITE_IOERR_SHMOPEN          = (SQLITE_IOERR | (18<<8));
    private static final int SQLITE_IOERR_SHMSIZE          = (SQLITE_IOERR | (19<<8));
    private static final int SQLITE_IOERR_SHMLOCK          = (SQLITE_IOERR | (20<<8));
    private static final int SQLITE_IOERR_SHMMAP           = (SQLITE_IOERR | (21<<8));
    private static final int SQLITE_IOERR_SEEK             = (SQLITE_IOERR | (22<<8));


    public static String convertToErrorMessage(SQLiteDiskIOException e) {
        return convertToErrorMessage(e.getMessage());
    }

    public static String convertToErrorMessage(String s) {
        if (!TextUtils.isEmpty(s)) {
            Matcher m = CODE.matcher(s);

            if (m.matches()) {
                int code = Integer.parseInt(m.group(1));
                switch (code) {
                    case SQLITE_IOERR_READ             : return "IOERR_READ";
                    case SQLITE_IOERR_SHORT_READ       : return "IOERR_SHORT_READ";
                    case SQLITE_IOERR_WRITE            : return "IOERR_WRITE";
                    case SQLITE_IOERR_FSYNC            : return "IOERR_FSYNC";
                    case SQLITE_IOERR_DIR_FSYNC        : return "IOERR_DIR_FSYNC";
                    case SQLITE_IOERR_TRUNCATE         : return "IOERR_TRUNCATE";
                    case SQLITE_IOERR_FSTAT            : return "IOERR_FSTAT";
                    case SQLITE_IOERR_UNLOCK           : return "IOERR_UNLOCK";
                    case SQLITE_IOERR_RDLOCK           : return "IOERR_RDLOCK";
                    case SQLITE_IOERR_DELETE           : return "IOERR_DELETE";
                    case SQLITE_IOERR_BLOCKED          : return "IOERR_BLOCKED";
                    case SQLITE_IOERR_NOMEM            : return "IOERR_NOMEM";
                    case SQLITE_IOERR_ACCESS           : return "IOERR_ACCESS";
                    case SQLITE_IOERR_CHECKRESERVEDLOCK: return "IOERR_CHECKRESERVEDLOCK";
                    case SQLITE_IOERR_LOCK             : return "IOERR_LOCK";
                    case SQLITE_IOERR_CLOSE            : return "IOERR_CLOSE";
                    case SQLITE_IOERR_DIR_CLOSE        : return "IOERR_DIR_CLOSE";
                    case SQLITE_IOERR_SHMOPEN          : return "IOERR_SHMOPEN";
                    case SQLITE_IOERR_SHMSIZE          : return "IOERR_SHMSIZE";
                    case SQLITE_IOERR_SHMLOCK          : return "IOERR_SHMLOCK";
                    case SQLITE_IOERR_SHMMAP           : return "IOERR_SHMMAP";
                    case SQLITE_IOERR_SEEK             : return "IOERR_SEEK";
                    default: return null;
                }
            }
        }
        return null;
    }
}
