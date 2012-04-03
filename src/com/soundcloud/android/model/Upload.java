package com.soundcloud.android.model;

import com.soundcloud.android.task.UploadTask;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Params;
import com.soundcloud.api.Request;

import android.content.res.Resources;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Upload {
    public long   id;
    public long   local_recording_id;
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

    // files
    public File soundFile;
    public File encodedSoundFile;
    public File artworkFile;
    public File resizedArtworkFile;

    // state
    public int status;
    private boolean mSuccess;
    private IOException mUploadException;

    public static final String TAG_SOURCE_ANDROID_RECORD    = "soundcloud:source=android-record";
    public static final String TAG_RECORDING_TYPE_DEDICATED = "soundcloud:recording-type=dedicated";
    public static final String TAG_SOURCE_ANDROID_3RDPARTY_UPLOAD = "soundcloud:source=android-3rdparty-upload";

    public boolean isError() {
        return mUploadException != null;
    }

    public static interface Status {
        int NOT_YET_UPLOADED    = 0;
        int UPLOADING           = 1;
        int UPLOADED            = 2;
    }

    public Upload(Recording r, Resources res) {
        id = System.currentTimeMillis();

        // defaults
        downloadable = false;
        streamable = true;
        soundFile  = r.audio_path;

        if (r.artwork_path != null) artworkFile = r.artwork_path;

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

    /**
     * Gets called after successful upload. Clean any tmp files here.
     */
    public void onUploaded() {
    }

    public List<String> getTags() {
        List<String> tags = new ArrayList<String>();
        if (tag_list != null) {
            Collections.addAll(tags, tag_list.split("\\s+"));
        }
        return tags;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public Upload succeed() {
        mSuccess = true;
        return this;
    }

    public boolean isPrivate() {
        return Params.Track.PRIVATE.equals(sharing);
    }

    public Upload setUploadException(IOException e) {
        mUploadException = e;
        mSuccess = false;
        return this;
    }

    public IOException getUploadException() {
        return mUploadException;
    }

    public Request getRequest(File file, Request.TransferProgressListener listener) {
        final Request request = new Request(Endpoints.TRACKS);

        for (Map.Entry<String, ?> entry : toTrackMap().entrySet()) {
            if (entry.getValue() instanceof Iterable) {
                for (Object o : (Iterable)entry.getValue()) {
                    request.add(entry.getKey(), o.toString());
                }
            } else {
                request.add(entry.getKey(), entry.getValue().toString());
            }
        }

        final String fileName;
        if (is_native_recording) {
            final String newTitle = title == null ? "unknown" : title;
            fileName = String.format("%s.%s", URLEncoder.encode(newTitle.replace(" ", "_")), "ogg");
        } else {
            fileName = file.getName();
        }

        return request.withFile(com.soundcloud.api.Params.Track.ASSET_DATA, file, fileName)
                .withFile(com.soundcloud.api.Params.Track.ARTWORK_DATA, artworkFile)
                .setProgressListener(listener);
    }

    public boolean isCancelled() {
        return mUploadException instanceof UploadTask.CanceledUploadException;
    }
}
