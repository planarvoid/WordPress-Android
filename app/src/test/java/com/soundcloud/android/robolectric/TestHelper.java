package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.accounts.AccountOperations.AccountInfoKeys.USER_ID;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.api.legacy.model.behavior.Identifiable;
import com.soundcloud.android.api.legacy.model.behavior.Persisted;
import com.soundcloud.android.blueprints.AffiliationBlueprint;
import com.soundcloud.android.blueprints.AudioAdBlueprint;
import com.soundcloud.android.blueprints.CategoryBlueprint;
import com.soundcloud.android.blueprints.CommentBlueprint;
import com.soundcloud.android.blueprints.DisplayPropertiesBlueprint;
import com.soundcloud.android.blueprints.PlaybackEventBlueprint;
import com.soundcloud.android.blueprints.PlaylistBlueprint;
import com.soundcloud.android.blueprints.PlaylistSummaryBlueprint;
import com.soundcloud.android.blueprints.RecordingBlueprint;
import com.soundcloud.android.blueprints.SuggestedUserBlueprint;
import com.soundcloud.android.blueprints.TrackBlueprint;
import com.soundcloud.android.blueprints.TrackStatsBlueprint;
import com.soundcloud.android.blueprints.TrackSummaryBlueprint;
import com.soundcloud.android.blueprints.UserBlueprint;
import com.soundcloud.android.blueprints.UserSummaryBlueprint;
import com.soundcloud.android.blueprints.UserUrnBlueprint;
import com.soundcloud.android.blueprints.VisualAdBlueprint;
import com.soundcloud.android.experiments.AssignmentBlueprint;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.suggestions.Category;
import com.soundcloud.android.onboarding.suggestions.CategoryGroup;
import com.soundcloud.android.onboarding.suggestions.SuggestedUser;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackUrn;
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

import javax.inject.Provider;
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

    public static SoundCloudApplication getApplication() {
        return (SoundCloudApplication) Robolectric.application;
    }

    public static ObjectMapper getObjectMapper() {
        return PublicApiWrapper.buildObjectMapper();
    }

    public static ModelFactory getModelFactory() {
        ModelFactory modelFactory = new ModelFactory();
        try {
            modelFactory.registerBlueprint(UserBlueprint.class);
            modelFactory.registerBlueprint(UserUrnBlueprint.class);
            modelFactory.registerBlueprint(UserSummaryBlueprint.class);
            modelFactory.registerBlueprint(TrackBlueprint.class);
            modelFactory.registerBlueprint(RecordingBlueprint.class);
            modelFactory.registerBlueprint(CategoryBlueprint.class);
            modelFactory.registerBlueprint(SuggestedUserBlueprint.class);
            modelFactory.registerBlueprint(TrackSummaryBlueprint.class);
            modelFactory.registerBlueprint(PlaylistSummaryBlueprint.class);
            modelFactory.registerBlueprint(TrackStatsBlueprint.class);
            modelFactory.registerBlueprint(PlaylistBlueprint.class);
            modelFactory.registerBlueprint(PlaybackEventBlueprint.class);
            modelFactory.registerBlueprint(AssignmentBlueprint.class);
            modelFactory.registerBlueprint(CommentBlueprint.class);
            modelFactory.registerBlueprint(AffiliationBlueprint.class);
            modelFactory.registerBlueprint(AudioAdBlueprint.class);
            modelFactory.registerBlueprint(VisualAdBlueprint.class);
            modelFactory.registerBlueprint(DisplayPropertiesBlueprint.class);
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
    public static <T extends PublicApiResource> List<T> readResourceList(String path) throws IOException {
        return getObjectMapper().readValue(TestHelper.class.getResourceAsStream(path),
                PublicApiResource.ResourceHolder.class).collection;
    }

    @SuppressWarnings("unchecked")
    public static <T extends PublicApiResource> T readResource(String path) throws IOException {
        InputStream is = TestHelper.class.getResourceAsStream(path);
        return (T) getObjectMapper().readValue(is, PublicApiResource.class);
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
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK", String.valueOf(version));
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
        ContentResolver resolver = Robolectric.application.getContentResolver();
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

    public static UserAssociation insertAsUserAssociation(PublicApiUser user, UserAssociation.Type assocType) {
        UserAssociation ua = new UserAssociation(assocType, user);
        TestHelper.insertWithDependencies(Content.USER_ASSOCIATIONS.uri, ua);
        return ua;
    }

    public static UserAssociation getUserAssociationByTargetId(Uri contentUri, long targetUserId){
        String where = TableColumns.UserAssociationView._ID + " = ? AND " +
                TableColumns.UserAssociationView.USER_ASSOCIATION_TYPE + " = ?";

        ContentResolver resolver = Robolectric.application.getContentResolver();
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
        return map.insert(Robolectric.application.getContentResolver());
    }

    public static int bulkInsert(PublicApiResource... items) {
        return bulkInsert(Arrays.asList(items));
    }

    public static <T extends Persisted & Identifiable> int bulkInsert(Uri uri, Collection<T> resources) {
        List<ContentValues> items = new ArrayList<ContentValues>();
        BulkInsertMap map = new BulkInsertMap();

        for (T resource : resources) {
            resource.putDependencyValues(map);
            items.add(resource.buildContentValues());
        }
        ContentResolver resolver = Robolectric.application.getContentResolver();
        map.insert(resolver); // dependencies
        return resolver.bulkInsert(uri, items.toArray(new ContentValues[items.size()]));
    }

    public static int bulkInsertToUserAssociations(List<? extends PublicApiResource> resources, Uri collectionUri) {
        return bulkInsertToUserAssociations(resources, collectionUri, null, null, null);
    }

    public static int bulkInsertToUserAssociationsAsAdditions(List<? extends PublicApiResource> resources, Uri collectionUri) {
        return bulkInsertToUserAssociations(resources, collectionUri, new Date(), null, null);
    }

    public static int bulkInsertToUserAssociationsAsAdditionsWithToken(List<? extends PublicApiResource> resources, Uri collectionUri, String token) {
        return bulkInsertToUserAssociations(resources, collectionUri, new Date(), null, token);
    }

    public static int bulkInsertToUserAssociationsAsRemovals(List<? extends PublicApiResource> resources, Uri collectionUri) {
        return bulkInsertToUserAssociations(resources, collectionUri, null, new Date(), null);
    }

    public static CategoryGroup buildCategoryGroup(String key, int categoryCount) throws CreateModelException {
        CategoryGroup categoryGroup = new CategoryGroup();
        categoryGroup.setKey(key);
        categoryGroup.setCategories(createCategories(categoryCount));
        return categoryGroup;
    }

    public static <T> Provider<T> buildProvider(final T mock) {
        return new Provider<T>() {
            @Override
            public T get() {
                return mock;
            }
        };
    }

    private static int bulkInsertToUserAssociations(List<? extends PublicApiResource> resources, Uri collectionUri,
                                                    Date addedAt, Date removedAt, String token) {
        SoundCloudApplication application = (SoundCloudApplication) Robolectric.application;
        final long userId = application.getAccountOperations().getLoggedInUserId();

        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            PublicApiResource r = resources.get(i);
            if (r == null) continue;

            r.putFullContentValues(map);
            ContentValues contentValues = new ContentValues();

            contentValues.put(TableColumns.UserAssociations.POSITION, i);
            contentValues.put(TableColumns.UserAssociations.TARGET_ID, r.getId());
            contentValues.put(TableColumns.UserAssociations.OWNER_ID, userId);
            contentValues.put(TableColumns.UserAssociations.ADDED_AT, addedAt == null ? null : addedAt.getTime());
            contentValues.put(TableColumns.UserAssociations.REMOVED_AT, removedAt == null ? null : removedAt.getTime());
            contentValues.put(TableColumns.UserAssociations.TOKEN, token);
            map.add(collectionUri, contentValues);
        }
        ContentResolver resolver = application.getContentResolver();
        return map.insert(resolver);
    }

    public static int bulkInsertDummyIdsToUserAssociations(Uri collectionUri, int count, long userId) {
        ContentValues[] cv = new ContentValues[count];
        for (int i = 0; i < count; i++) {
            cv[i] = new ContentValues();
            cv[i].put(TableColumns.UserAssociations.POSITION, i);
            cv[i].put(TableColumns.UserAssociations.TARGET_ID, i);
            cv[i].put(TableColumns.UserAssociations.OWNER_ID, userId);
        }
        ContentResolver resolver = Robolectric.application.getContentResolver();
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
        String where = TableColumns.UserAssociationView._ID + " = " + targetId + " AND " +
                TableColumns.UserAssociationView.USER_ASSOCIATION_TYPE + " = " + content.collectionType;
        return loadLocalContentItem(content.uri, UserAssociation.class, where);
    }

    public static List<UserAssociation> loadUserAssociations(final Content content) throws Exception {
        String where = TableColumns.UserAssociationView.USER_ASSOCIATION_TYPE + " = " + content.collectionType;
        return loadLocalContent(content.uri, UserAssociation.class, where);
    }

    public static PublicApiPlaylist loadPlaylist(long playlistId) throws Exception {
        PublicApiPlaylist playlist = TestHelper.loadLocalContentItem(Content.PLAYLISTS.uri, PublicApiPlaylist.class, "_id = " + playlistId);
        playlist.tracks = TestHelper.loadLocalContent(Content.PLAYLIST_TRACKS.forQuery(String.valueOf(playlistId)), PublicApiTrack.class);
        return playlist;
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

    public static PublicApiPlaylist createNewUserPlaylist(PublicApiUser user, boolean isPrivate, List<PublicApiTrack> tracks) {
        final String title = "new playlist " + System.currentTimeMillis();
        bulkInsert(tracks);
        PublicApiPlaylist playlist = PublicApiPlaylist.newUserPlaylist(user, title, isPrivate, tracks);
        insertWithDependencies(playlist);
        return playlist;
    }

    public static void setUserId(long id) {
        ShadowAccountManager shadowAccountManager = shadowOf(ShadowAccountManager.get(DefaultTestRunner.application));
        AccountOperations accountOperations = DefaultTestRunner.application.getAccountOperations();

        if (!accountOperations.accountManagerHasSoundCloudAccount()) {
            shadowAccountManager.addAccount(new Account("name", "com.soundcloud.android.account"));
        }

        accountOperations.setAccountData(USER_ID.getKey(), Long.toString(id));
    }

    public static List<PublicApiTrack> createTracks(int count) throws CreateModelException {
        if (count < 1) return Collections.EMPTY_LIST;

        List<PublicApiTrack> items = new ArrayList<PublicApiTrack>();
        for (int i = 0; i < count; i++) {
            items.add(TestHelper.getModelFactory().createModel(PublicApiTrack.class));
        }
        return items;
    }

    public static List<TrackUrn> createTracksUrn(Long... ids){
        return Lists.transform(new ArrayList<Long>(Arrays.asList(ids)), new Function<Long, TrackUrn>() {
            @Override
            public TrackUrn apply(Long id) {
                return Urn.forTrack(id);
            }
        });
    }

    public static List<PublicApiUser> createUsers(int count) {
        if (count < 1) return Collections.EMPTY_LIST;

        List<PublicApiUser> items = new ArrayList<PublicApiUser>();
        for (long i = 100L; i <= count * 100; i += 100) {
            PublicApiUser u = new PublicApiUser();
            u.setId(i);
            u.permalink = "u" + String.valueOf(i);
            items.add(u);
        }
        return items;
    }

    public static List<SuggestedUser> createSuggestedUsers(int count) throws CreateModelException {
        List<SuggestedUser> suggestedUsers = new ArrayList<SuggestedUser>();
        for (int i = 0; i < count; i++){
            suggestedUsers.add(TestHelper.getModelFactory().createModel(SuggestedUser.class));
        }
        return suggestedUsers;
    }

    public static List<Category> createCategories(int count) throws CreateModelException {
        List<Category> categories = new ArrayList<Category>();
        for (int i = 0; i < count; i++){
            categories.add(TestHelper.getModelFactory().createModel(Category.class));
        }
        return categories;
    }

    public static List<UserAssociation> createDirtyFollowings(int count) {
        List<UserAssociation> userAssociations = new ArrayList<UserAssociation>();
        for (PublicApiUser user : createUsers(count)) {
            final UserAssociation association = new UserAssociation(Association.Type.FOLLOWING, user);
            association.markForAddition();
            userAssociations.add(association);
        }
        return userAssociations;

    }
}
