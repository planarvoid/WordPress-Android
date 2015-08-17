package com.soundcloud.android.testsupport;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.accounts.AccountOperations.AccountInfoKeys.USER_ID;
import static com.soundcloud.android.model.Urn.forTrack;
import static com.soundcloud.java.collections.Lists.transform;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.api.legacy.model.behavior.Identifiable;
import com.soundcloud.android.api.legacy.model.behavior.Persisted;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.suggestions.Category;
import com.soundcloud.android.onboarding.suggestions.CategoryGroup;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.fixtures.JsonFixtures;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.functions.Function;
import com.tobedevoured.modelcitizen.CreateModelException;
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Deprecated
public class TestHelper {
    private TestHelper() {
    }

    public static ObjectMapper getObjectMapper() {
        return PublicApi.buildObjectMapper();
    }

    public static Activities getActivities(String path) throws IOException {
        return TestHelper.readJson(Activities.class, path);
    }

    public static <T> T readJson(Class<T> klazz, String path) throws IOException {
        InputStream is = TestHelper.class.getResourceAsStream(path);
        expect(is).not.toBeNull();
        return getObjectMapper().readValue(is, klazz);
    }

    public static <T> T readJson(Class<T> modelClass, Class<?> lookupClass, String file) throws IOException {
        InputStream is = lookupClass.getResourceAsStream(file);
        expect(is).not.toBeNull();
        return getObjectMapper().readValue(is, modelClass);
    }

    public static void addPendingHttpResponse(Class klazz, String... resources) throws IOException {
        for (String r : resources) {
            Robolectric.getFakeHttpLayer().addPendingHttpResponse(
                    new TestHttpResponse(200, JsonFixtures.resourceAsBytes(klazz, r)));
        }
    }

    public static void addCannedResponse(Class klazz, String url, String resource) throws IOException {
        Robolectric.getFakeHttpLayer().addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpGet.METHOD_NAME, url),
                new TestHttpResponse(200, JsonFixtures.resourceAsBytes(klazz, resource)));
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
        Set<Uri> _uris = new HashSet<>();
        for (ShadowContentResolver.NotifiedUri u : res.getNotifiedUris()) {
            _uris.add(u.uri);
        }
        expect(_uris).toContain(uris);
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
            if (i < ids.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("] }");
        Robolectric.addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpGet.METHOD_NAME, url), new TestHttpResponse(200, sb.toString()));
    }

    public static FakeHttpLayer.UriRegexMatcher createRegexRequestMatcherForUriWithClientId(String method, String url) {
        return new FakeHttpLayer.UriRegexMatcher(method, url.replace("?", "\\?") + "(?:[&\\?]client_id=.+)?$");
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

    public static UserAssociation insertAsUserAssociation(PublicApiUser user, UserAssociation.Type assocType) {
        UserAssociation ua = new UserAssociation(assocType, user);
        TestHelper.insertWithDependencies(Content.USER_ASSOCIATIONS.uri, ua);
        return ua;
    }

    public static UserAssociation getUserAssociationByTargetId(Uri contentUri, long targetUserId) {
        String where = TableColumns.UserAssociationView._ID + " = ? AND " +
                TableColumns.UserAssociationView.USER_ASSOCIATION_TYPE + " = ?";

        ContentResolver resolver = Robolectric.application.getContentResolver();
        Cursor cursor = resolver.query(contentUri, null, where,
                new String[]{String.valueOf(targetUserId), String.valueOf(Content.match(contentUri).collectionType)},
                null);

        return cursor.moveToFirst() ? new UserAssociation(cursor) : null;
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
        categoryGroup.setCategories(ModelFixtures.create(Category.class, categoryCount));
        return categoryGroup;
    }

    private static int bulkInsertToUserAssociations(List<? extends PublicApiResource> resources, Uri collectionUri,
                                                    Date addedAt, Date removedAt, String token) {
        SoundCloudApplication application = (SoundCloudApplication) Robolectric.application;
        final long userId = application.getAccountOperations().getLoggedInUserId();

        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            PublicApiResource r = resources.get(i);
            if (r == null) {
                continue;
            }

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
        List<T> items = new ArrayList<>();
        if (itemsCursor != null) {
            Constructor<T> constructor = modelClass.getConstructor(Cursor.class);
            while (itemsCursor.moveToNext()) {
                items.add(constructor.newInstance(itemsCursor));
            }
        }
        if (itemsCursor != null) {
            itemsCursor.close();
        }
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

    @SuppressWarnings("unchecked")
    public static <T extends Persisted> T reload(final T model) {
        try {
            Class<T> clazz = (Class<T>) model.getClass();
            return loadLocalContent(model.toUri(), clazz).get(0);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static void addResourceResponse(Class<?> klazz, String url, String resource) throws IOException {
        TestHelper.addCannedResponse(klazz, url, resource);
    }

    public static void setUserId(long id) {
        ShadowAccountManager shadowAccountManager = shadowOf(ShadowAccountManager.get(DefaultTestRunner.application));
        AccountOperations accountOperations = DefaultTestRunner.application.getAccountOperations();

        if (accountOperations.getSoundCloudAccount() == null) {
            shadowAccountManager.addAccount(new Account("name", "com.soundcloud.android.account"));
        }

        accountOperations.setAccountData(USER_ID.getKey(), Long.toString(id));
    }

    public static List<Urn> createTracksUrn(Long... ids) {
        return transform(new ArrayList<>(Arrays.asList(ids)), new Function<Long, Urn>() {
            @Override
            public Urn apply(Long id) {
                return forTrack(id);
            }
        });
    }
}
