package com.soundcloud.android.model;

import static com.soundcloud.android.utils.CloudUtils.mkdirs;

import com.soundcloud.android.R;
import com.soundcloud.android.provider.DatabaseHelper;
import com.soundcloud.android.provider.DatabaseHelper.Recordings;
import com.soundcloud.android.task.UploadTask;
import com.soundcloud.android.utils.CloudUtils;
import com.soundcloud.android.utils.record.CloudRecorder.Profile;
import com.soundcloud.api.Params;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Upload extends BaseObj implements Parcelable {

    public long id;
    public long local_recording_id;
    public String sharing;
    public String sharing_note;
    public String post_to_empty;
    public String title;
    public String type;
    public String tag_list;
    public String ogg_filename;
    public String description;
    public String genre;
    public String service_ids;
    public String shared_emails;
    public boolean encode;
    public boolean downloadable;
    public boolean streamable;
    public boolean is_native_recording;

    public long upload_id;
    public int upload_status;
    public boolean upload_error;
    public boolean cancelled;

    public String trackPath;
    public String artworkPath;

    public File encodedFile;

    public static final String LOCAL_RECORDING_ID = "local_recording_id";
    public static final String SOURCE_PATH = "source_path";
    public static final String OGG_FILENAME = "ogg_filename";
    public static final String ARTWORK_PATH = "artwork_path";
    public static final String ENCODE = "encode";
    public static final String DELETE_AFTER = "delete_after";

    public static interface UploadStatus {
        int NOT_YET_UPLOADED    = 0;
        int UPLOADING           = 1;
        int UPLOADED            = 2;
    }

    public Upload(Map<String,?> uploadData) {

        id = System.currentTimeMillis();

        if (!uploadData.containsKey(SOURCE_PATH)) {
            throw new IllegalArgumentException("Need to specify " + SOURCE_PATH);
        }

        trackPath = String.valueOf(uploadData.get(SOURCE_PATH));
        artworkPath = String.valueOf(uploadData.get(ARTWORK_PATH));

        title = String.valueOf(uploadData.get(Params.Track.TITLE));
        type = String.valueOf(uploadData.get(Params.Track.TYPE));
        tag_list = String.valueOf(uploadData.get(Params.Track.TAG_LIST));
        sharing = String.valueOf(uploadData.get(Params.Track.SHARING));
        sharing_note = String.valueOf(uploadData.get(Params.Track.SHARING_NOTE));
        description = String.valueOf(uploadData.get(Params.Track.DESCRIPTION));
        genre = String.valueOf(uploadData.get(Params.Track.GENRE));

        downloadable = Boolean.getBoolean(String.valueOf(uploadData.get(Params.Track.DOWNLOADABLE)));
        streamable = Boolean.getBoolean(String.valueOf(uploadData.get(Params.Track.STREAMABLE)));

        if (uploadData.containsKey(Params.Track.POST_TO)) {
            service_ids = TextUtils.join(",",(List<Integer>) uploadData.get(Params.Track.POST_TO));
        }
        if (uploadData.containsKey(Params.Track.SHARED_EMAILS)) {
            shared_emails = TextUtils.join(",",(List<String>) uploadData.get(Params.Track.SHARED_EMAILS));
        }

        post_to_empty = uploadData.get(Params.Track.POST_TO_EMPTY) != null ? "" : null;

    }

    public Upload(Recording r){

        id = System.currentTimeMillis();

        // defaults
        downloadable = false;
        streamable = true;

        trackPath = r.audio_path.getAbsolutePath();
        if (r.artwork_path != null) artworkPath = r.artwork_path.getAbsolutePath();

        title = r.sharingNote();
        type = "recording";
        local_recording_id = r.id;
        sharing = r.is_private ? Params.Track.PRIVATE : Params.Track.PUBLIC;

        if (!TextUtils.isEmpty(r.description)) description = r.description;
        if (!TextUtils.isEmpty(r.genre)) genre = r.genre;


        if (!r.is_private) {

            List<Integer> serviceIds = new ArrayList<Integer>();
            if (!TextUtils.isEmpty(service_ids))
            for (String serviceId : service_ids.split(",")){
                if (!TextUtils.isEmpty(serviceId)) serviceIds.add(Integer.valueOf(serviceId));
            }

             if (!serviceIds.isEmpty()) {
                 sharing_note = r.sharingNote();
                 service_ids = TextUtils.join(",",serviceIds);
             } else {
                post_to_empty = "";
             }
        } else { // not private
             if (!TextUtils.isEmpty(r.shared_emails)) {
                 shared_emails = r.shared_emails;
             }
        }


        // add machine tags
        List<String> tags = new ArrayList<String>();
        if (r.tags != null) {
            for (String t : r.tags) {
                tags.add(t.contains(" ") ? "\""+t+"\"" : t);
            }
        }
        if (!TextUtils.isEmpty(r.four_square_venue_id)) tags.add("foursquare:venue=" + r.four_square_venue_id);
        if (r.latitude != 0) tags.add("geo:lat=" + r.latitude);
        if (r.longitude != 0) tags.add("geo:lon=" + r.longitude);
        if (r.external_upload) {
            tags.add("soundcloud:source=android-3rdparty-upload");
        } else {
            tags.add("soundcloud:source=android-record");
        }
        tag_list = TextUtils.join(",",tags);



        if (!r.external_upload) {
            if (r.audio_profile == Profile.RAW) {
                ogg_filename = r.generateUploadFilename(title).getAbsolutePath();
                encode = true;
            }
            is_native_recording = true;
        }
    }


    public Map<String, ?> toTrackMap() {

        Map<String, Object> data = new HashMap<String, Object>();
        data.put(Params.Track.TITLE, title);
        data.put(Params.Track.TYPE, type);
        data.put(Params.Track.SHARING, sharing);
        data.put(Params.Track.DOWNLOADABLE, downloadable);
        data.put(Params.Track.STREAMABLE, streamable);

        if (!TextUtils.isEmpty(sharing_note)) data.put(Params.Track.SHARING_NOTE, sharing_note);
        if (!TextUtils.isEmpty(tag_list)) data.put(Params.Track.TAG_LIST, tag_list);
        if (!TextUtils.isEmpty(description)) data.put(Params.Track.DESCRIPTION, description);
        if (!TextUtils.isEmpty(genre)) data.put(Params.Track.GENRE, genre);

        if (!TextUtils.isEmpty(service_ids)) data.put(Params.Track.POST_TO, service_ids.split(","));
        if (!TextUtils.isEmpty(shared_emails)) data.put(Params.Track.SHARED_EMAILS, shared_emails.split(","));

        if (post_to_empty != null) data.put(Params.Track.POST_TO_EMPTY, "");
        return data;
    }

    public Upload(Parcel in) {
        readFromParcel(in);
    }

    public static final Creator<Upload> CREATOR = new Creator<Upload>() {
        public Upload createFromParcel(Parcel in) {
            return new Upload(in);
        }

        public Upload[] newArray(int size) {
            return new Upload[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out,flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
