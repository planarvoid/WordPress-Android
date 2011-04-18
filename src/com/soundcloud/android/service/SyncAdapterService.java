
package com.soundcloud.android.service;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.objects.Track;
import com.soundcloud.api.CloudAPI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.type.TypeFactory;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SyncAdapterService extends Service {
    private static final String TAG = "ContactsSyncAdapterService";
    private static ScSyncAdapter sSyncAdapter = null;
    private static ContentResolver mContentResolver = null;

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

        List<Track> favorites = null;
        try {
            favorites = fetchAllTracks(app, CloudAPI.Enddpoints.MY_FAVORITES);
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


        Log.i(TAG, "favorites count: " + (favorites == null ? "null" : favorites.size()));


    }

    /**
     * Get the current url for this adapter
     *
     * @return the url
     */
    private static String buildRequest(String endpoint, int page) {
        Uri u = Uri.parse(endpoint);
        Uri.Builder builder = u.buildUpon();
        builder.appendQueryParameter("limit", Integer.toString(CloudAPI.MAX_COLLECTION_SIZE));
        builder.appendQueryParameter("offset", String.valueOf(CloudAPI.MAX_COLLECTION_SIZE * page));
        return builder.build().toString();
    }

    private static List<Track> fetchAllTracks(SoundCloudApplication app, String endpoint) throws JsonParseException, JsonMappingException, IllegalStateException, IOException {
        boolean keepGoing = true;
        int currentPage = 0;
        List<Track> tracks = new ArrayList<Track>();
        while (keepGoing) {
            List<Track> result;
                result = fetchTracks(app, buildRequest(endpoint, currentPage));
                if (result != null && result.size() > 0){
                    if (result.size() != CloudAPI.MAX_COLLECTION_SIZE) keepGoing = false;
                    tracks.addAll(result);
                } else{
                    keepGoing = false;
                }
            currentPage++;
        }
        return tracks;
    }

    private static List<Track> fetchTracks(SoundCloudApplication app, String path) throws JsonParseException, JsonMappingException, IllegalStateException, IOException {

        HttpResponse response = app.getContent(path);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            return app.getMapper().readValue(response.getEntity().getContent(),
                    TypeFactory.collectionType(ArrayList.class, Track.class));
        } else {
            Log.w(TAG, "invalid response code " + response.getStatusLine());
            return null;
        }
    }
}
