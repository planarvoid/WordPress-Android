
package com.soundcloud.android.objects;

import com.soundcloud.android.provider.ScContentProvider;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import java.io.File;
import java.lang.reflect.Field;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Track extends BaseObj implements Parcelable {

    private static final String TAG = "Track";

    public Long id;

    public String artwork_url;

    public String attachments_uri;

    public String avatar_url;

    public Float bpm;

    public Boolean commentable;

    public Integer comment_count;

    public String created_at;

    public CreatedWith created_with;

    public String description;

    public Boolean downloadable;

    public Integer download_count;

    public String download_url;

    public Integer downloads_remaining;

    public Integer duration;

    public String duration_formatted;

    public Integer favoritings_count;

    public String genre;

    public String isrc;

    public String key_signature;

    public User label;

    public String label_id;

    public String label_name;

    public String license;

    public String original_format;

    public String permalink;

    public String permalink_url;

    public String playback_count;

    public String purchase_url;

    public String release;

    public String release_day;

    public String release_month;

    public String release_year;

    public String secret_token;

    public String secret_uri;

    public Integer shared_to_count;

    public String sharing;

    public String state;

    public Boolean streamable;

    public String stream_url;

    public String tag_list;

    public String track_type;

    public String title;

    public String uri;

    public Boolean user_played;

    public String user_playback_count;

    public Boolean user_favorite;

    public Integer user_favorite_id;

    public User user;

    public Long user_id;

    public String video_url;

    public String waveform_url;

    public static class CreatedWith {
        public Integer id;

        public String name;

        public String uri;

        public String permalink_url;
    }
    
    public boolean mIsPlaylist = false;

    public File mCacheFile;

    public Long filelength;

    public String mSignedUri;
    
    public static final class Tracks implements BaseColumns {
        
        private Tracks() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/tracks");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.tracks";

        public static final String ID = "_id";

        public static final String PERMALINK = "permalink";

        public static final String DURATION = "duration";
        
        public static final String TAG_LIST = "tag_list";
        
        public static final String TRACK_TYPE = "track_type";
        
        public static final String TITLE = "title";
        
        public static final String PERMALINK_URL = "permalink_url";
        
        public static final String ARTWORK_URL = "artwork_url";
        
        public static final String WAVEFORM_URL = "waveform_url";
        
        public static final String DOWNLOADABLE = "downloadable";
        
        public static final String DOWNLOAD_URL = "download_url";
        
        public static final String STREAM_URL = "stream_url";
        
        public static final String STREAMABLE = "streamable";
        
        public static final String USER_ID = "user_id";
        
        public static final String USER_FAVORITE = "user_favorite";
        
        public static final String USER_PLAYED = "user_played";
        
        public static final String FILELENGTH = "filelength";
        
    }

    public Track() {
    }

    public Track(Parcel in) {
        readFromParcel(in);
    }

    public Track(Cursor cursor) {
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();

            String[] keys = cursor.getColumnNames();
            for (String key : keys) {
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.class) {
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Boolean.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)) == 0 ? false
                                    : true);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public static final Parcelable.Creator<Track> CREATOR = new Parcelable.Creator<Track>() {
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        public Track[] newArray(int size) {
            return new Track[size];
        }
    };
    
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out,flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
