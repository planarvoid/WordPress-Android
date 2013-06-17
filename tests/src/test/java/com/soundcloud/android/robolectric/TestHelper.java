package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.accounts.AccountOperations.AccountInfoKeys.USER_ID;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.Wrapper;
import com.soundcloud.android.blueprints.CategoryBlueprint;
import com.soundcloud.android.blueprints.SuggestedUserBlueprint;
import com.soundcloud.android.blueprints.TrackBlueprint;
import com.soundcloud.android.blueprints.UserBlueprint;
import com.soundcloud.android.model.Category;
import com.soundcloud.android.model.CategoryGroup;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.Recording;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.model.behavior.Identifiable;
import com.soundcloud.android.model.behavior.Persisted;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.IOUtils;
import com.tobedevoured.modelcitizen.CreateModelException;
import com.tobedevoured.modelcitizen.ModelFactory;
import com.tobedevoured.modelcitizen.RegisterBlueprintException;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowAccountManager;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import com.xtremelabs.robolectric.shadows.ShadowEnvironment;
import com.xtremelabs.robolectric.shadows.ShadowNetworkInfo;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.client.methods.HttpGet;

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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestHelper {
    private TestHelper() {
    }

    public static ObjectMapper getObjectMapper() {
        return Wrapper.buildObjectMapper();
    }

    public static ModelFactory getModelFactory() {
        ModelFactory modelFactory = new ModelFactory();
        try {
            modelFactory.registerBlueprint(UserBlueprint.class);
            modelFactory.registerBlueprint(TrackBlueprint.class);
            modelFactory.registerBlueprint(CategoryBlueprint.class);
            modelFactory.registerBlueprint(SuggestedUserBlueprint.class);
        } catch (RegisterBlueprintException e) {
            throw new RuntimeException(e);
        }
        return modelFactory;
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
        Robolectric.getFakeHttpLayer().addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpGet.METHOD_NAME, url),
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
        ShadowContentResolver res =
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
        Robolectric.addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpGet.METHOD_NAME, url), new TestHttpResponse(200, sb.toString()));
    }

    public static FakeHttpLayer.UriRegexMatcher createRegexRequestMatcherForUriWithClientId(String method, String url) {
        return new FakeHttpLayer.UriRegexMatcher(method, url.replace("?","\\?")  + "(?:[&\\?]client_id=.+)?$");
    }

    public static void setSdkVersion(int version) {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", version);
    }

    public static void enableSDCard() {
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

    public static UserAssociation insertAsUserAssociation(User user, UserAssociation.Type assocType) {
        UserAssociation ua = new UserAssociation(assocType, user);
        TestHelper.insertWithDependencies(Content.USER_ASSOCIATIONS.uri, ua);
        return ua;
    }

    public static UserAssociation getUserAssociationByTargetId(Uri contentUri, long targetUserId){
        String where = DBHelper.UserAssociationView._ID + " = ? AND " +
                DBHelper.UserAssociationView.USER_ASSOCIATION_TYPE + " = ?";

        ContentResolver resolver = DefaultTestRunner.application.getContentResolver();
        Cursor cursor = resolver.query(contentUri, null, where,
                new String[]{String.valueOf(targetUserId), String.valueOf(Content.match(contentUri).collectionType)},
                null);

        return cursor.moveToFirst() ? new UserAssociation(cursor) : null;
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

    public static int bulkInsertToUserAssociations(List<? extends ScResource> resources, Uri collectionUri) {
        return bulkInsertToUserAssociations(resources, collectionUri, null, null, null);
    }

    public static int bulkInsertToUserAssociationsAsAdditions(List<? extends ScResource> resources, Uri collectionUri) {
        return bulkInsertToUserAssociations(resources, collectionUri, new Date(), null, null);
    }

    public static int bulkInsertToUserAssociationsAsAdditionsWithToken(List<? extends ScResource> resources, Uri collectionUri, String token) {
        return bulkInsertToUserAssociations(resources, collectionUri, new Date(), null, token);
    }

    public static int bulkInsertToUserAssociationsAsRemovals(List<? extends ScResource> resources, Uri collectionUri) {
        return bulkInsertToUserAssociations(resources, collectionUri, null, new Date(), null);
    }

    public static CategoryGroup buildCategoryGroup(String key, int categoryCount) throws CreateModelException {
        CategoryGroup categoryGroup = new CategoryGroup();
        categoryGroup.setKey(key);
        categoryGroup.setCategories(Collections.nCopies(categoryCount, getModelFactory().createModel(Category.class)));
        return categoryGroup;
    }

    private static int bulkInsertToUserAssociations(List<? extends ScResource> resources, Uri collectionUri,
                                                    Date addedAt, Date removedAt, String token) {
        SoundCloudApplication application = DefaultTestRunner.application;
        final long userId = SoundCloudApplication.getUserId();

        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            ScResource r = resources.get(i);
            if (r == null) continue;

            r.putFullContentValues(map);
            ContentValues contentValues = new ContentValues();

            contentValues.put(DBHelper.UserAssociations.POSITION, i);
            contentValues.put(DBHelper.UserAssociations.TARGET_ID, r.getId());
            contentValues.put(DBHelper.UserAssociations.OWNER_ID, userId);
            contentValues.put(DBHelper.UserAssociations.ADDED_AT, addedAt == null ? null : addedAt.getTime());
            contentValues.put(DBHelper.UserAssociations.REMOVED_AT, removedAt == null ? null : removedAt.getTime());
            contentValues.put(DBHelper.UserAssociations.TOKEN, token);
            map.add(collectionUri, contentValues);
        }
        ContentResolver resolver = application.getContentResolver();
        return map.insert(resolver);
    }

    public static int bulkInsertDummyIdsToUserAssociations(Uri collectionUri, int count, long userId) {
        ContentValues[] cv = new ContentValues[count];
        for (int i = 0; i < count; i++) {
            cv[i] = new ContentValues();
            cv[i].put(DBHelper.UserAssociations.POSITION, i);
            cv[i].put(DBHelper.UserAssociations.TARGET_ID, i);
            cv[i].put(DBHelper.UserAssociations.OWNER_ID, userId);
        }
        ContentResolver resolver = DefaultTestRunner.application.getContentResolver();
        return resolver.bulkInsert(collectionUri, cv);
    }

    public static <T extends Persisted> List<T> loadLocalContent(final Uri contentUri, Class<T> modelClass) throws Exception {
        return loadLocalContent(contentUri, modelClass, null);
    }

    public static <T extends Persisted> List<T> loadLocalContent(final Uri contentUri, Class<T> modelClass,
                                                                 String selection) throws Exception {
        Cursor itemsCursor = DefaultTestRunner.application.getContentResolver().query(contentUri, null, selection, null, null);
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

    public static <T extends Persisted> T loadLocalContentItem(final Uri contentUri, Class<T> modelClass, String where) throws Exception {
        return loadLocalContent(contentUri, modelClass, where).get(0);
    }

    public static UserAssociation loadUserAssociation(final Content content, long targetId) throws Exception {
        String where = DBHelper.UserAssociationView._ID + " = " + targetId + " AND " +
                DBHelper.UserAssociationView.USER_ASSOCIATION_TYPE + " = " + content.collectionType;
        return loadLocalContentItem(content.uri, UserAssociation.class, where);
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
        r.setId(1);
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

    public static void addResourceResponse(Class<?> klazz, String url, String resource) throws IOException {
        TestHelper.addCannedResponse(klazz, url, resource);
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

    public static void setUserId(long id) {
        ShadowAccountManager shadowAccountManager = shadowOf(ShadowAccountManager.get(DefaultTestRunner.application));
        AccountOperations accountOperations = new AccountOperations(DefaultTestRunner.application);

        if (!accountOperations.soundCloudAccountExists()) {
            shadowAccountManager.addAccount(new Account("name", "com.soundcloud.android.account"));
        }

        accountOperations.setAccountData(USER_ID.getKey(), Long.toString(id));
    }

    public static List<User> createUsers(int count) {
        if (count < 1) return Collections.EMPTY_LIST;

        List<User> items = new ArrayList<User>();
        for (long i = 100L; i <= count * 100; i += 100) {
            User u = new User();
            u.setId(i);
            u.permalink = "u" + String.valueOf(i);
            items.add(u);
        }
        return items;
    }
}
