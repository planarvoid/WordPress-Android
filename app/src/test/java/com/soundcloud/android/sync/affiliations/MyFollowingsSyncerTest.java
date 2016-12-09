package com.soundcloud.android.sync.affiliations;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.NotificationConstants;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.affiliations.MyFollowingsSyncer.ForbiddenFollowNotificationBuilder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.users.UserAssociationProperty;
import com.soundcloud.android.users.UserAssociationStorage;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.http.HttpStatus;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

import android.app.Notification;
import android.app.NotificationManager;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class MyFollowingsSyncerTest extends AndroidUnitTest {

    private static final Urn USER_1 = Urn.forUser(1);
    private static final Urn USER_2 = Urn.forUser(2);
    private static final String USERNAME_1 = "User1";
    private static final String USERNAME_2 = "User2";
    private static final String AGE_RESTRICTED_BODY = "{\"error_key\":\"age_restricted\",\"age\":19}";
    private static final String AGE_UNKNOWN_BODY = "{\"error_key\":\"age_unknown\"}";
    private static final String BLOCKED_BODY = "{\"error_key\":\"blocked\"}";


    private MyFollowingsSyncer myFollowingsSyncer;

    @Mock private ForbiddenFollowNotificationBuilder notificationBuilder;
    @Mock private ApiClient apiClient;
    @Mock private FollowingOperations followingOperations;
    @Mock private NotificationManager notificationManger;
    @Mock private Navigator navigator;
    @Mock private UserAssociationStorage userAssociationStorage;
    private JsonTransformer jsonTransformer = new JacksonJsonTransformer();
    private PublishSubject<PropertySet> revertSubject;

    @Before
    public void setUp() throws Exception {
        myFollowingsSyncer = new MyFollowingsSyncer(
                notificationBuilder,
                apiClient, followingOperations, notificationManger,
                jsonTransformer, userAssociationStorage);
    }

    @Test
    public void pushesFollowings() throws Exception {
        when(userAssociationStorage.hasStaleFollowings()).thenReturn(true);
        when(userAssociationStorage.loadStaleFollowings()).thenReturn(
                Arrays.asList(
                        getNewFollowingAddition(USER_1, USERNAME_1),
                        getNewFollowingRemoval(USER_2, USERNAME_2)
                )
        );

        mockApiFollowingAddition(USER_1, TestApiResponses.ok());
        mockApiFollowingRemoval(USER_2, TestApiResponses.ok());
        mockApiFollowingsResponse(
                Arrays.asList(
                    getApiFollowing(USER_1),
                    getApiFollowing(USER_2)
                )
        );

        assertThat(myFollowingsSyncer.call()).isTrue();

        verify(userAssociationStorage).updateFollowingFromPendingState(USER_1);
        verify(userAssociationStorage).updateFollowingFromPendingState(USER_2);
    }

    @Test
    public void storesFollowings() throws Exception {
        List<ApiFollowing> followings = Arrays.asList(
                getApiFollowing(USER_1),
                getApiFollowing(USER_2)
        );
        mockApiFollowingsResponse(followings);

        assertThat(myFollowingsSyncer.call()).isTrue();

        verify(userAssociationStorage).insertFollowedUserIds(toUserIds(followings));
    }

    @Test
    public void doesNotStoreUnchangedFollowings() throws Exception {
        List<ApiFollowing> followings = Arrays.asList(
                getApiFollowing(USER_1),
                getApiFollowing(USER_2)
        );

        mockApiFollowingsResponse(
                followings
        );
        List<Long> userIds = toUserIds(followings);
        when(userAssociationStorage.loadFollowedUserIds()).thenReturn(new HashSet<>(userIds));

        assertThat(myFollowingsSyncer.call()).isFalse();

        verify(userAssociationStorage,never()).insertFollowedUserIds(anyList());
    }

    @Test(expected = ApiRequestException.class)
    public void pushFollowingsThrowsExceptionOn500() throws Exception {
        when(userAssociationStorage.hasStaleFollowings()).thenReturn(true);
        when(userAssociationStorage.loadStaleFollowings()).thenReturn(
                singletonList(
                        getNewFollowingAddition(USER_1, USERNAME_1)
                )
        );

        mockApiFollowingAddition(USER_1, TestApiResponses.status(500));
        myFollowingsSyncer.call();
    }

    @Test
    public void pushFollowingsRevertsAndNotifiesOnUnderage() throws Exception {
        setupFailedPush(AGE_RESTRICTED_BODY);
        Notification notification = new Notification();
        when(notificationBuilder.buildUnderAgeNotification(USER_1, 19, USERNAME_1)).thenReturn(notification);

        myFollowingsSyncer.call();

        verify(notificationManger).notify(USER_1.toString(),
                                          NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID,
                                          notification);

        assertThat(revertSubject.hasObservers()).isTrue();
    }

    @Test
    public void pushFollowingsRevertsAndNotifiesOnUnknownAge() throws Exception {
        setupFailedPush(AGE_UNKNOWN_BODY);
        Notification notification = new Notification();
        when(notificationBuilder.buildUnknownAgeNotification(USER_1, USERNAME_1)).thenReturn(notification);

        myFollowingsSyncer.call();

        verify(notificationManger).notify(USER_1.toString(),
                                          NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID,
                                          notification);

        assertThat(revertSubject.hasObservers()).isTrue();
    }

    @Test
    public void pushFollowingsRevertsAndNotifiesOnBlocked() throws Exception {
        setupFailedPush(BLOCKED_BODY);
        Notification notification = new Notification();
        when(notificationBuilder.buildBlockedFollowNotification(USER_1, USERNAME_1)).thenReturn(notification);

        myFollowingsSyncer.call();

        verify(notificationManger).notify(USER_1.toString(),
                                          NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID,
                                          notification);

        assertThat(revertSubject.hasObservers()).isTrue();
    }

    @NonNull
    private void setupFailedPush(String body) throws Exception {
        mockApiFollowingsResponse(Collections.<ApiFollowing>emptyList());
        when(userAssociationStorage.loadFollowedUserIds()).thenReturn(Collections.<Long>emptySet());
        when(userAssociationStorage.hasStaleFollowings()).thenReturn(true);
        when(userAssociationStorage.loadStaleFollowings()).thenReturn(
                singletonList(
                        getNewFollowingAddition(USER_1, USERNAME_1)
                )
        );

        revertSubject = PublishSubject.create();
        when(followingOperations.toggleFollowing(USER_1, false)).thenReturn(revertSubject);

        mockApiFollowingAddition(USER_1, TestApiResponses.status(HttpStatus.FORBIDDEN,  body));
    }


    @NonNull
    private void setupFailedPushBadRequest(String body) throws Exception {
        mockApiFollowingsResponse(Collections.<ApiFollowing>emptyList());
        when(userAssociationStorage.loadFollowedUserIds()).thenReturn(Collections.<Long>emptySet());
        when(userAssociationStorage.hasStaleFollowings()).thenReturn(true);
        when(userAssociationStorage.loadStaleFollowings()).thenReturn(
                singletonList(
                        getNewFollowingAddition(USER_1, USERNAME_1)
                )
        );

        revertSubject = PublishSubject.create();
        when(followingOperations.toggleFollowing(USER_1, false)).thenReturn(revertSubject);

        mockApiFollowingAddition(USER_1, TestApiResponses.status(HttpStatus.BAD_REQUEST,  body));
    }


    @NonNull
    private List<Long> toUserIds(List<ApiFollowing> followings) {
        return Lists.transform(followings, ApiFollowing.TO_USER_IDS);
    }

    @NonNull
    private ApiFollowing getApiFollowing(Urn targetUrn) {
        return ApiFollowing.create(Urn.forUser(123), new Date(), targetUrn);
    }

    private void mockApiFollowingAddition(Urn userUrn, ApiResponse apiResponse) throws Exception {
        when(apiClient.fetchResponse(argThat(isApiRequestTo("POST", ApiEndpoints.USER_FOLLOWS.path(userUrn)))))
                .thenReturn(apiResponse);
    }

    private void mockApiFollowingRemoval(Urn userUrn, ApiResponse apiResponse) throws Exception {
        when(apiClient.fetchResponse(argThat(isApiRequestTo("DELETE", ApiEndpoints.USER_FOLLOWS.path(userUrn)))))
                .thenReturn(apiResponse);
    }

    private void mockApiFollowingsResponse(List<ApiFollowing> collection) throws Exception {
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.MY_FOLLOWINGS.path())),
                                     any(TypeToken.class))).thenReturn(new ModelCollection<>(collection));
    }

    private PropertySet getNewFollowingAddition(Urn urn, String username) {
        return PropertySet.from(
                UserProperty.URN.bind(urn),
                UserProperty.USERNAME.bind(username),
                UserAssociationProperty.ADDED_AT.bind(new Date())
        );
    }

    private PropertySet getNewFollowingRemoval(Urn urn, String username) {
        return PropertySet.from(
                UserProperty.URN.bind(urn),
                UserProperty.USERNAME.bind(username),
                UserAssociationProperty.REMOVED_AT.bind(new Date())
        );
    }
}
