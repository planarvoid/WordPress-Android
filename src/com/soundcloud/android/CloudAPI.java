package com.soundcloud.android;

import com.soundcloud.android.objects.User;
import com.soundcloud.utils.SoundCloudAuthorizationClient;
import com.soundcloud.utils.http.Http;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.content.ContentBody;
import org.codehaus.jackson.map.ObjectMapper;
import org.urbanstew.soundcloudapi.SoundCloudAPI;

import java.io.IOException;
import java.util.List;

public interface CloudAPI {


    public interface Enddpoints {
        String TRACKS              = "tracks";
        String CONNECTIONS         = "me/connections.json";
        String USERS               = "users";
        String PATH_TRACKS         = "tracks";

        String MY_DETAILS          = "me";
        String MY_ACTIVITIES       = "me/activities/tracks";
        String MY_EXCLUSIVE_TRACKS = "me/activities/tracks/exclusive";
        String MY_TRACKS           = "me/tracks";
        String MY_PLAYLISTS        = "me/playlists";
        String MY_FAVORITES        = "me/favorites";
        String MY_FOLLOWERS        = "me/followers";
        String MY_FOLLOWINGS       = "me/followings";
        String USER_DETAILS        = "users/{user_id}";
        String USER_FOLLOWINGS     = "users/{user_id}/followings";
        String USER_FOLLOWERS      = "users/{user_id}/followers";
        String TRACK_DETAILS       = "tracks/{track_id}";
        String USER_TRACKS         = "users/{user_id}/tracks";
        String USER_FAVORITES      = "users/{user_id}/favorites";
        String USER_PLAYLISTS      = "users/{user_id}/playlists";
        String TRACK_COMMENTS      = "tracks/{track_id}/comments";
    }

    public interface Params {
        String TITLE          = "track[title]"; // required
        String TYPE           = "track[track_type]";

        String ASSET_DATA     = "track[asset_data]";
        String ARTWORK_DATA   = "track[artwork_data]";

        String POST_TO        = "track[post_to][][id]";
        String POST_TO_EMPTY  = "track[post_to][]";
        String TAG_LIST       = "track[tag_list]";
        String SHARING        = "track[sharing]";

        String STREAMABLE     = "track[streamable]";
        String DOWNLOADABLE   = "track[downloadable]";

        String SHARED_EMAILS  = "track[shared_to][emails][][address]";
        String SHARING_NOTE   = "track[sharing_note]";

        String PUBLIC         = "public";
        String PRIVATE        = "private";
    }

    // XXX simplify+consolidate methods

    ObjectMapper getMapper();

    HttpUriRequest getRequest(String path, List<NameValuePair> params);
    HttpResponse execute(HttpUriRequest request) throws IOException;


    String getSignedUrl(String path);
    String signStreamUrlNaked(String path);

    HttpResponse putContent(String path, List<NameValuePair> params) throws IOException;
    HttpResponse postContent(String path, List<NameValuePair> params) throws IOException;
    HttpResponse deleteContent(String path) throws IOException;
    HttpResponse getContent(String path) throws IOException;

    int resolve(String uri) throws IOException;

    HttpResponse upload(ContentBody trackBody,
                        ContentBody artworkBody,
                        List<NameValuePair> params,
                        Http.ProgressListener listener)
                    throws IOException;

    void unauthorize();
    void authorizeWithoutCallback(Client client);

    SoundCloudAPI.State getState();

    static interface Client extends SoundCloudAuthorizationClient {
        void storeUser(User user, String token, String secret);
    }
}
