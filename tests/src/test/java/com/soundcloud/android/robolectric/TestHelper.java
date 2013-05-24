package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.Wrapper;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.behavior.Identifiable;
import com.soundcloud.android.model.behavior.Persisted;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowAccountManager;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import com.xtremelabs.robolectric.shadows.ShadowEnvironment;
import com.xtremelabs.robolectric.shadows.ShadowNetworkInfo;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestHelper {
    private TestHelper() {}

    public static ObjectMapper getObjectMapper() {
        return Wrapper.buildObjectMapper();
    }

    public static Activities getActivities(String path) throws IOException {
        return TestHelper.readJson(Activities.class, path);
    }

    public static <T> T readJson(Class<T> klazz, String path) throws IOException {
        InputStream is = TestHelper.class.getResourceAsStream(path);
        expect(is).not.toBeNull();
        return getObjectMapper().readValue(is, klazz);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ScResource> List<T> readResourceList(String path) throws IOException {
        return getObjectMapper().readValue(TestHelper.class.getResourceAsStream(path),
                ScResource.ScResourceHolder.class).collection;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ScResource> T readResource(String path) throws IOException {
        InputStream is = TestHelper.class.getResourceAsStream(path);
        return (T) getObjectMapper().readValue(is, ScResource.class);
    }

    public static <T> T readJson(Class<T> modelClass, Class<?> lookupClass, String file) throws IOException {
        InputStream is = lookupClass.getResourceAsStream(file);
        expect(is).not.toBeNull();
        return getObjectMapper().readValue(is, modelClass);
    }

    public static void addPendingHttpResponse(Class klazz, String... resources) throws IOException {
        for (String r : resources) {
            Robolectric.getFakeHttpLayer().addPendingHttpResponse(
                    new TestHttpResponse(200, resourceAsBytes(klazz, r)));
        }
    }

    public static void addCannedResponse(Class klazz, String url, String resource) throws IOException {
        Robolectric.getFakeHttpLayer().addHttpResponseRule(url,
                new TestHttpResponse(200, resourceAsBytes(klazz, resource)));
    }

    public static void addResponseRule(String uri, int status) {
        Robolectric.addHttpResponseRule(uri, new TestHttpResponse(status, ""));
    }

    public static void addPendingIOException(String path) {
        if (path != null && path.startsWith("/")) {
            path = path.substring(1, path.length());
        }

        FakeHttpLayer.RequestMatcherBuilder builder = new FakeHttpLayer.RequestMatcherBuilder();
        if (path != null) {
            builder.path(path);
        }
        Robolectric.getFakeHttpLayer().addHttpResponseRule(
                new FakeHttpLayer.RequestMatcherResponseRule(builder, new IOException("boom")));
    }

    public static String resourceAsString(Class klazz, String res) throws IOException {
        InputStream is = klazz.getResourceAsStream(res);
        expect(is).not.toBeNull();
        return IOUtils.readInputStream(is);
    }

    public static byte[] resourceAsBytes(Class klazz, String res) throws IOException {
        InputStream is = klazz.getResourceAsStream(res);
        expect(is).not.toBeNull();
        return IOUtils.readInputStreamAsBytes(is);
    }

    public static void assertFirstIdToBe(Content content, long id) {
        Cursor c = Robolectric.application.getContentResolver().query(content.uri, null, null, null, null);
        expect(c).not.toBeNull();
        c.moveToFirst();
        expect(c.getLong(c.getColumnIndex("_id"))).toEqual(id);
    }

    public static void assertResolverNotified(Uri... uris) {
        ShadowContentResolver res  =
                Robolectric.shadowOf_(Robolectric.application.getContentResolver());
        Set<Uri> _uris = new HashSet<Uri>();
        for (ShadowContentResolver.NotifiedUri u : res.getNotifiedUris()) {
            _uris.add(u.uri);
        }
        expect(_uris).toContain(uris);
    }

    public static void simulateOffline() {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        shadowOf(shadowOf(cm).getActiveNetworkInfo()).setConnectionStatus(false);
    }

    public static void simulateOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        shadowOf(shadowOf(cm).getActiveNetworkInfo()).setConnectionStatus(true);
    }


    public static void connectedViaWifi(boolean enabled) {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (enabled) {
            // pretend we're connected via wifi
            shadowOf(cm).setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null, ConnectivityManager.TYPE_WIFI,
                    0, true, true));
        } else {
            // pretend we're connected only via mobile, no wifi
            shadowOf(cm).setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null, ConnectivityManager.TYPE_MOBILE,
                    0, true, true));

            NetworkInfo info = ShadowNetworkInfo.newInstance(null, ConnectivityManager.TYPE_WIFI, 0, true, false);
            Robolectric.shadowOf(cm).setNetworkInfo(ConnectivityManager.TYPE_WIFI, info);
        }
    }


    public static void setBackgrounData(boolean enabled) {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);

        Robolectric.shadowOf(cm).setBackgroundDataSetting(enabled);
    }

    public static void enableFlightmode(boolean enabled) {
        Settings.System.putInt(Robolectric.application.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, enabled ? 1 : 0);
    }

    public static void addIdResponse(String url, int... ids) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"collection\": [");
        for (int i = 0; i < ids.length; i++) {
            sb.append(ids[i]);
            if (i < ids.length - 1) sb.append(", ");
        }
        sb.append("] }");
        Robolectric.addHttpResponseRule(url, new TestHttpResponse(200, sb.toString()));
    }

    public static void setSdkVersion(int version) {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", version);
    }

    public static void enableSDCard(){
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    }

    public static void disableSDCard() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_REMOVED);
    }

    public static Uri insert(Uri contentUri, Persisted insertable) {
        Uri uri = Robolectric.application.getContentResolver().insert(contentUri, insertable.buildContentValues());
        expect(uri).not.toBeNull();
        return uri;
    }

    public static <T extends Persisted & Identifiable> Uri insert(T insertable) {
        return insert(insertable.toUri(), insertable);
    }

    public static <T extends Persisted & Identifiable> Uri insertWithDependencies(Uri contentUri, T resource) {
        ContentResolver resolver = DefaultTestRunner.application.getContentResolver();
        final BulkInsertMap dependencies = new BulkInsertMap();
        resource.putDependencyValues(dependencies);
        dependencies.insert(resolver);

        return resolver.insert(contentUri, resource.buildContentValues());
    }

    public static <T extends Persisted & Identifiable> Uri insertWithDependencies(T resource) {
        return insertWithDependencies(resource.toUri(), resource);
    }

    public static SoundAssociation insertAsSoundAssociation(Playable playable, SoundAssociation.Type assocType) {
        SoundAssociation sa = new SoundAssociation(playable, new Date(), assocType);
        TestHelper.insertWithDependencies(Content.COLLECTION_ITEMS.uri, sa);
        return sa;
    }

    public static <T extends Persisted & Identifiable> int bulkInsert(Collection<T> items) {
        BulkInsertMap map = new BulkInsertMap();
        for (T m : items) {
            m.putFullContentValues(map);
        }
        return map.insert(DefaultTestRunner.application.getContentResolver());
    }

    public static int bulkInsert(ScResource... items) {
        return bulkInsert(Arrays.asList(items));
    }

    public static <T extends Persisted & Identifiable> int bulkInsert(Uri uri, Collection<T> resources) {
        List<ContentValues> items = new ArrayList<ContentValues>();
        BulkInsertMap map = new BulkInsertMap();

        for (T resource : resources) {
            resource.putDependencyValues(map);
            items.add(resource.buildContentValues());
        }
        ContentResolver resolver = DefaultTestRunner.application.getContentResolver();
        map.insert(resolver); // dependencies
        return resolver.bulkInsert(uri, items.toArray(new ContentValues[items.size()]));
    }


    public static int bulkInsertToCollectionItems(List<? extends ScResource> resources, Uri collectionUri) {
        SoundCloudApplication application = DefaultTestRunner.application;
        final long userId = SoundCloudApplication.getUserId();

        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            ScResource r = resources.get(i);
            if (r == null) continue;

            r.putFullContentValues(map);
            ContentValues contentValues = new ContentValues();

            contentValues.put(DBHelper.CollectionItems.POSITION, i);
            contentValues.put(DBHelper.CollectionItems.ITEM_ID, r.id);
            contentValues.put(DBHelper.CollectionItems.USER_ID, userId);
            map.add(collectionUri, contentValues);
        }
        ContentResolver resolver = application.getContentResolver();
        return map.insert(resolver);
    }

    public static <T extends Persisted> List<T> loadLocalContent(final Uri contentUri, Class<T> modelClass) throws Exception {
        Cursor itemsCursor = DefaultTestRunner.application.getContentResolver().query(contentUri, null, null, null, null);
        List<T> items = new ArrayList<T>();
        if (itemsCursor != null) {
            Constructor<T> constructor = modelClass.getConstructor(Cursor.class);
            while (itemsCursor.moveToNext()) {
                items.add(constructor.newInstance(itemsCursor));
            }
        }
        if (itemsCursor != null) itemsCursor.close();
        //noinspection unchecked
        return items;

    }

    @SuppressWarnings("unchecked")
    public static <T extends Persisted> T reload(final T model) {
        try {
            Class<T> clazz = (Class<T>) model.getClass();
            return loadLocalContent(model.toUri(), clazz).get(0);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static Recording createRecording(long userId) throws IOException {
        File tmp = createRecordingFile("wav");

        Recording r = new Recording(tmp);
        r.id = 1;
        r.latitude = 32.3;
        r.longitude = 23.1;
        r.what_text = "somewhat";
        r.where_text = "somehere";
        r.four_square_venue_id = "foursquare";
        r.description = "test recording";
        r.genre = "speed blues ";
        r.duration = 86 * 1000;
        r.user_id = userId;
        r.recipient_user_id = 300L;
        r.recipient_username = "foo";
        r.shared_emails = "foo@example.com";
        r.shared_ids = "1,2,3,4";
        r.upload_status = Recording.Status.NOT_YET_UPLOADED;
        r.artwork_path = r.getFile();
        r.resized_artwork_path = r.artwork_path;
        r.tip_key = "something";

        return r;
    }

    private static File createRecordingFile(String extension) throws IOException {
        File tmp = File.createTempFile("recording-test", extension);
        tmp.createNewFile();
        expect(tmp.exists()).toBeTrue();

        Calendar c = Calendar.getInstance();
        //noinspection MagicConstant
        c.set(2001, 1, 15, 14, 31, 1);  // 14:31:01, 15/02/2011
        tmp.setLastModified(c.getTimeInMillis());
        return tmp;
    }

    public static Playlist createNewUserPlaylist(User user, boolean isPrivate, List<Track> tracks) {
        final String title = "new playlist " + System.currentTimeMillis();
        bulkInsert(tracks);
        Playlist playlist = Playlist.newUserPlaylist(user, title, isPrivate, tracks);
        insertWithDependencies(playlist);
        return playlist;
    }

    public static void setUserId(long id){
        ShadowAccountManager shadowAccountManager = shadowOf(ShadowAccountManager.get(DefaultTestRunner.application));
        AccountOperations accountOperations = new AccountOperations(DefaultTestRunner.application);

        if(!accountOperations.soundCloudAccountExists()){
            shadowAccountManager.addAccount(new Account("name", "com.soundcloud.android.account"));
        }

        accountOperations.setAccountData(User.DataKeys.USER_ID, Long.toString(id));

    }
}
