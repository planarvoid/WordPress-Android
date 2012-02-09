package com.soundcloud.android.model;

import android.content.res.Resources;
import android.os.Parcel;
import android.text.TextUtils;
import com.soundcloud.android.utils.record.CloudRecorder.Profile;
import com.soundcloud.api.Params;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.io.File;
import java.util.*;

import static com.soundcloud.android.utils.IOUtils.mkdirs;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Upload extends ScModel {
    public long local_recording_id;
    public String sharing;
    public String sharing_note;
    public String post_to_empty;
    public String title;
    public String type;
    public String tag_list;
    public String description;
    public String genre;
    public String service_ids;
    public String shared_emails;
    public String shared_ids;
    public boolean encode;
    public boolean downloadable;
    public boolean streamable;
    public boolean is_native_recording;

    public int upload_status;
    public boolean upload_error;
    public boolean cancelled;

    // XXX should be File
    public String trackPath;
    public String artworkPath;

    public File encodedFile;

    public static final String LOCAL_RECORDING_ID = "local_recording_id";
    public static final String SOURCE_PATH = "source_path";
    public static final String OGG_FILENAME = "ogg_filename";
    public static final String ARTWORK_PATH = "artwork_path";
    public static final String DELETE_AFTER = "delete_after";

    public static final String TAG_SOURCE_ANDROID_RECORD    = "soundcloud:source=android-record";
    public static final String TAG_RECORDING_TYPE_DEDICATED = "soundcloud:recording-type=dedicated";
    public static final String TAG_SOURCE_ANDROID_3RDPARTY_UPLOAD = "soundcloud:source=android-3rdparty-upload";

    public static interface UploadStatus {
        int NOT_YET_UPLOADED    = 0;
        int UPLOADING           = 1;
        int UPLOADED            = 2;
    }

    public Upload(Map<String, ?> uploadData) {

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
        if (uploadData.containsKey(Params.Track.SHARED_IDS)) {
            shared_ids = TextUtils.join(",",(List<String>) uploadData.get(Params.Track.SHARED_IDS));
        }

        post_to_empty = uploadData.get(Params.Track.POST_TO_EMPTY) != null ? "" : null;

    }

    public Upload(Recording r, Resources res) {
        id = System.currentTimeMillis();

        // defaults
        downloadable = false;
        streamable = true;

        trackPath = r.audio_path.getAbsolutePath();

        if (r.artwork_path != null) artworkPath = r.artwork_path.getAbsolutePath();

        title = r.sharingNote(res);
        type = "recording";
        local_recording_id = r.id;
        sharing = r.is_private ? Params.Track.PRIVATE : Params.Track.PUBLIC;

        if (!TextUtils.isEmpty(r.description)) description = r.description;
        if (!TextUtils.isEmpty(r.genre)) genre = r.genre;

        if (!r.is_private) {
            List<Integer> serviceIds = new ArrayList<Integer>();
            if (!TextUtils.isEmpty(r.service_ids)) {
                for (String serviceId : r.service_ids.split(",")) {
                    if (!TextUtils.isEmpty(serviceId)) serviceIds.add(Integer.valueOf(serviceId));
                }
            }

            if (!serviceIds.isEmpty()) {
                sharing_note = r.sharingNote(res);
                service_ids = TextUtils.join(",", serviceIds);
            } else {
                post_to_empty = "";
            }
        } else { // not private
            if (!TextUtils.isEmpty(r.shared_emails)) {
                shared_emails = r.shared_emails;
            }
        }

        if (r.private_user_id >0){
            shared_ids = String.valueOf(r.private_user_id);
        } else if (!TextUtils.isEmpty(r.shared_ids)) {
            shared_ids = r.shared_ids;
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
            tags.add(TAG_SOURCE_ANDROID_3RDPARTY_UPLOAD);
        } else {
            tags.add(TAG_SOURCE_ANDROID_RECORD);
            if (r.private_user_id > 0) {
                tags.add(TAG_RECORDING_TYPE_DEDICATED);
            }
        }

        tag_list = TextUtils.join(" ", tags);

        if (!r.external_upload) {
            if (r.audio_profile == Profile.RAW) {
                encodedFile = new File(encodeDir(r.audio_path),
                        String.format("%s.%s",
                            r.audio_path.getName().contains(".") ? r.audio_path.getName().substring(0,
                            r.audio_path.getName().lastIndexOf(".")) : r.audio_path.getName() ,"ogg"));
                encode = true;
            }
            is_native_recording = true;
        }
    }

    private File encodeDir(File trackFile) {
        File encodeDir = new File(trackFile.getParentFile(), ".encode");
        if (!encodeDir.exists()) {
            mkdirs(encodeDir);
        }
        return encodeDir;
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

        if (!TextUtils.isEmpty(service_ids)) {
            List<String> ids = new ArrayList<String>();
            Collections.addAll(ids, service_ids.split(","));
            data.put(Params.Track.POST_TO, ids);
        }

        if (!TextUtils.isEmpty(shared_emails)) {
            List<String> ids = new ArrayList<String>();
            Collections.addAll(ids, shared_emails.split(","));
            data.put(Params.Track.SHARED_EMAILS, ids);
        }

        if (!TextUtils.isEmpty(shared_ids)) {
            List<String> ids = new ArrayList<String>();
            Collections.addAll(ids, shared_ids.split(","));
            data.put(Params.Track.SHARED_IDS, ids);
        }

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

    public List<String> getTags() {
        List<String> tags = new ArrayList<String>();
        if (tag_list != null) {
            Collections.addAll(tags, tag_list.split("\\s+"));
        }
        return tags;
    }
}
