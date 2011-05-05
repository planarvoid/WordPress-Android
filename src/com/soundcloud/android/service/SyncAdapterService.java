
package com.soundcloud.android.service;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.SoundCloudDB;
import com.soundcloud.android.SoundCloudDB.Events;
import com.soundcloud.android.activity.Dashboard;
import com.soundcloud.android.objects.Activities;
import com.soundcloud.android.objects.Track;
import com.soundcloud.android.objects.User;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.type.TypeFactory;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SyncAdapterService extends Service {
    private static final String TAG = "ScSyncAdapterService";
    private static ScSyncAdapter sSyncAdapter = null;
    private static ContentResolver mContentResolver = null;

    private static final int MAX_COLLECTION_SIZE = 200;
    private static final int MAX_EVENTS_STORED = 200;

    public SyncAdapterService() {
        super();
    }

    private static class ScSyncAdapter extends AbstractThreadedSyncAdapter {
        private Context mContext;
        private SoundCloudApplication mApp;

        public ScSyncAdapter(SoundCloudApplication app, Context context) {
            super(context, true);
            mContext = context;
            mApp = app;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                ContentProviderClient provider, SyncResult syncResult) {
            try {
                SyncAdapterService.performSync(mApp, mContext, account, extras, authority,
                        provider, syncResult);
            } catch (OperationCanceledException e) {
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        ret = getSyncAdapter().getSyncAdapterBinder();
        return ret;
    }

    private ScSyncAdapter getSyncAdapter() {
        if (sSyncAdapter == null)
            sSyncAdapter = new ScSyncAdapter((SoundCloudApplication)this.getApplication(), this);
        return sSyncAdapter;
    }

    private static void performSync(SoundCloudApplication app, Context context, Account account, Bundle extras,
            String authority, ContentProviderClient provider, SyncResult syncResult)
            throws OperationCanceledException {
        mContentResolver = context.getContentResolver();

        Log.i(TAG, "performSync: " + account.toString());

        app.useAccount(account);

        final long user_id = app.getAccountDataLong( User.DataKeys.USER_ID);


        // get the timestamp of the newest record in the database
        Cursor firstCursor = mContentResolver.query(Events.CONTENT_URI, new String[] {
            Events.ID, Events.ORIGIN_ID,
        }, Events.USER_ID + " = " + user_id, null, Events.CREATED_AT + " DESC LIMIT "
                + MAX_EVENTS_STORED);

        if (firstCursor.getCount() > 0) firstCursor.moveToFirst();

        final long firstOriginId = firstCursor.getCount() == 0 ? 0 : firstCursor.getLong(0);
        final long firstTrackId = firstCursor.getCount() == 0 ? 0 : firstCursor.getLong(1);
        firstCursor.close();


        int added = 0;
        Activities activities = null;
        try {
            HttpResponse response = app.get(buildRequest(Endpoints.MY_ACTIVITIES, 0));
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                activities = app.getMapper().readValue(
                        response.getEntity().getContent(), Activities.class);
                activities.setCursorToLastEvent();
                added = SoundCloudDB.getInstance().insertActivities(app.getContentResolver(), activities,
                        user_id, firstTrackId);

                Log.i(TAG,"Inserted " + added + " of " + activities.size() + " activities");
            }

        } catch (JsonParseException e) {
            syncResult.stats.numParseExceptions++;
            e.printStackTrace();
        } catch (JsonMappingException e) {
            syncResult.stats.numParseExceptions++;
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
        }

        final boolean caughtUp = (activities != null && added != activities.size());

        Intent intent = new Intent();
        intent.setAction(Dashboard.SYNC_CHECK_ACTION);
        app.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                int result = getResultCode();

                if (result == Activity.RESULT_CANCELED) { // Activity caught it
                    Log.d(TAG, "No Dashboard Activity, go ahead delete events as necessary");
                    // if there are older entries, delete them as necessary
                    if (firstTrackId > 0) {
                        if (caughtUp) {
                            SoundCloudDB.getInstance().cleanStaleActivities(mContentResolver, user_id, MAX_EVENTS_STORED);
                        } else {
                            // we never reached the older entries, so delete them
                            SoundCloudDB.getInstance().deleteActivitiesBefore(mContentResolver, user_id, firstOriginId);
                        }
                    }
                    return;
                }
            }
        }, null, Activity.RESULT_CANCELED, null, null);




    }

    /**
     * Get the current url for this adapter
     *
     * @return the url
     */
    private static Request buildRequest(String endpoint, int page) {
        Request request = Request.to(endpoint);
        request.add("limit",  Integer.toString(MAX_COLLECTION_SIZE));
        request.add("offset", String.valueOf(MAX_COLLECTION_SIZE * page));
        return request;
    }

    //{"collection":[{"type":"track","created_at":"2011/05/02 16:33:18 +0000","origin":{"id":14559625,"created_at":"2011/05/02 16:33:18 +0000","user_id":2490,"duration":4416,"commentable":true,"state":"finished","sharing":"public","tag_list":"","permalink":"may_02_2011-001-edit","description":"","streamable":true,"downloadable":true,"genre":"","release":"","purchase_url":null,"label_id":null,"label_name":"","isrc":"","video_url":null,"track_type":"","key_signature":"","bpm":null,"title":"Quick Monday is quick","release_year":null,"release_month":null,"release_day":null,"original_format":"wav","license":"cc-by-nc-sa","uri":"https://api.soundcloud.com/tracks/14559625","permalink_url":"http://soundcloud.com/david/may_02_2011-001-edit","artwork_url":null,"waveform_url":"http://w1.sndcdn.com/sWJO1byxkgl5_m.png","user":{"id":2490,"permalink":"david","username":"David No\u00ebl","uri":"https://api.soundcloud.com/users/2490","permalink_url":"http://soundcloud.com/david","avatar_url":"http://i1.sndcdn.com/avatars-000003312251-vi5p6e-large.jpg?af2741b"},"stream_url":"https://api.soundcloud.com/tracks/14559625/stream","download_url":"https://api.soundcloud.com/tracks/14559625/download","user_playback_count":1,"user_favorite":false,"playback_count":32,"download_count":0,"favoritings_count":2,"comment_count":1,"created_with":{"id":3884,"name":"iRig Recorder","uri":"https://api.soundcloud.com/apps/3884","permalink_url":"http://soundcloud.com/apps/irig-recorder"},"attachments_uri":"https://api.soundcloud.com/tracks/14559625/attachments","sharing_note":{"text":"Mondays with @thisisparker are always a little bit faster","created_at":"2011/05/02 16:38:00 +0000"}},"tags":"affiliated"},{"type":"track","created_at":"2011/05/02 15:02:22 +0000","origin":{"id":14555387,"created_at":"2011/05/02 15:02:22 +0000","user_id":4606,"duration":366113,"commentable":true,"state":"finished","sharing":"public","tag_list":"nina simone feeling good hrdvsion remix sun closed eyes sleep forever","permalink":"nina-simone-feeling-good","description":"","streamable":true,"downloadable":true,"genre":"","release":"","purchase_url":null,"label_id":null,"label_name":"","isrc":"","video_url":null,"track_type":"remix","key_signature":"","bpm":null,"title":"Nina Simone - Feeling Good (Hrdvsion Remix)","release_year":null,"release_month":null,"release_day":null,"original_format":"mp3","license":"all-rights-reserved","uri":"https://api.soundcloud.com/tracks/14555387","permalink_url":"http://soundcloud.com/hrdvsion/nina-simone-feeling-good","artwork_url":"http://i1.sndcdn.com/artworks-000006889448-5kcm2w-large.jpg?af2741b","waveform_url":"http://w1.sndcdn.com/SzxkcLi84GTl_m.png","user":{"id":4606,"permalink":"hrdvsion","username":"Hrdvsion","uri":"https://api.soundcloud.com/users/4606","permalink_url":"http://soundcloud.com/hrdvsion","avatar_url":"http://i1.sndcdn.com/avatars-000000981338-vjhn9o-large.jpg?af2741b"},"stream_url":"https://api.soundcloud.com/tracks/14555387/stream","download_url":"https://api.soundcloud.com/tracks/14555387/download","user_playback_count":1,"user_favorite":false,"playback_count":186,"download_count":64,"favoritings_count":16,"comment_count":15,"attachments_uri":"https://api.soundcloud.com/tracks/14555387/attachments","sharing_note":{"text":"nina simone... sun, eyes closed in the brightness, sleep forever.","created_at":"2011/05/02 15:02:22 +0000"}},"tags":"affiliated"}],"next_href":"https://api.soundcloud.com/me/activities/tracks?cursor=c5751842-74a0-11e0-96d2-d41dad77532f\u0026limit=2"}

    private static List<Track> fetchAllTracks(SoundCloudApplication app, String endpoint) throws JsonParseException, JsonMappingException, IllegalStateException, IOException {
        boolean keepGoing = true;
        int currentPage = 0;
        List<Track> tracks = new ArrayList<Track>();
        while (keepGoing) {
            List<Track> result;
                result = fetchTracks(app, buildRequest(endpoint, currentPage));
                if (result != null && result.size() > 0){
                    if (result.size() != MAX_COLLECTION_SIZE) keepGoing = false;
                    tracks.addAll(result);
                } else{
                    keepGoing = false;
                }
            currentPage++;
        }
        return tracks;
    }

    private static List<Track> fetchTracks(SoundCloudApplication app, Request path) throws JsonParseException, JsonMappingException, IllegalStateException, IOException {

        HttpResponse response = app.get(path);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return app.getMapper().readValue(response.getEntity().getContent(),
                    TypeFactory.collectionType(ArrayList.class, Track.class));
        } else {
            Log.w(TAG, "invalid response code " + response.getStatusLine());
            return null;
        }
    }

}
