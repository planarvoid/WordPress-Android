package com.soundcloud.android;

import com.soundcloud.android.accounts.LogoutFragment;
import com.soundcloud.android.activities.ActivitiesActivity;
import com.soundcloud.android.activities.ActivitiesFragment;
import com.soundcloud.android.cast.CastOptionsProvider;
import com.soundcloud.android.cast.CastRedirectActivity;
import com.soundcloud.android.collection.CollectionFragment;
import com.soundcloud.android.collection.CollectionPreviewView;
import com.soundcloud.android.collection.ConfirmRemoveOfflineDialogFragment;
import com.soundcloud.android.collection.playhistory.PlayHistoryActivity;
import com.soundcloud.android.collection.playhistory.PlayHistoryFragment;
import com.soundcloud.android.collection.playlists.NewPlaylistsFragment;
import com.soundcloud.android.collection.playlists.PlaylistsActivity;
import com.soundcloud.android.collection.playlists.PlaylistsFragment;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedActivity;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedFragment;
import com.soundcloud.android.comments.AddCommentDialogFragment;
import com.soundcloud.android.comments.CommentsFragment;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.record.RecordFragment;
import com.soundcloud.android.creators.record.RecordPermissionsActivity;
import com.soundcloud.android.creators.record.UploadActivity;
import com.soundcloud.android.creators.upload.Encoder;
import com.soundcloud.android.creators.upload.ImageResizer;
import com.soundcloud.android.creators.upload.MetadataFragment;
import com.soundcloud.android.creators.upload.Processor;
import com.soundcloud.android.creators.upload.UploadMonitorFragment;
import com.soundcloud.android.creators.upload.UploadService;
import com.soundcloud.android.creators.upload.Uploader;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.discovery.AllGenresActivity;
import com.soundcloud.android.discovery.ChartActivity;
import com.soundcloud.android.discovery.ChartTracksFragment;
import com.soundcloud.android.discovery.DiscoveryFragment;
import com.soundcloud.android.discovery.GenresFragment;
import com.soundcloud.android.discovery.PlaylistDiscoveryActivity;
import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.discovery.ViewAllRecommendedTracksActivity;
import com.soundcloud.android.discovery.ViewAllRecommendedTracksFragment;
import com.soundcloud.android.downgrade.GoOffboardingActivity;
import com.soundcloud.android.downgrade.GoOffboardingFragment;
import com.soundcloud.android.explore.ExploreActivity;
import com.soundcloud.android.explore.ExploreGenresFragment;
import com.soundcloud.android.explore.ExploreTracksCategoryActivity;
import com.soundcloud.android.explore.ExploreTracksFragment;
import com.soundcloud.android.gcm.GcmDebugDialogFragment;
import com.soundcloud.android.gcm.GcmInstanceIDListenerService;
import com.soundcloud.android.gcm.GcmRegistrationService;
import com.soundcloud.android.gcm.ScFirebaseMessagingService;
import com.soundcloud.android.likes.TrackLikesActivity;
import com.soundcloud.android.likes.TrackLikesFragment;
import com.soundcloud.android.main.DevDrawerFragment;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.more.MoreFragment;
import com.soundcloud.android.offline.AlarmManagerReceiver;
import com.soundcloud.android.offline.OfflineContentService;
import com.soundcloud.android.offline.OfflineLikesDialog;
import com.soundcloud.android.offline.OfflineSettingsOnboardingActivity;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.onboarding.FacebookSessionCallback;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.onboarding.auth.AddUserInfoTaskFragment;
import com.soundcloud.android.onboarding.auth.AuthTaskFragment;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.onboarding.auth.LoginTaskFragment;
import com.soundcloud.android.onboarding.auth.RecoverActivity;
import com.soundcloud.android.onboarding.auth.SignupTaskFragment;
import com.soundcloud.android.onboarding.auth.tasks.GooglePlusSignInTask;
import com.soundcloud.android.payments.LegacyConversionActivity;
import com.soundcloud.android.payments.NativeConversionActivity;
import com.soundcloud.android.payments.PlanChoiceActivity;
import com.soundcloud.android.payments.TieredConversionActivity;
import com.soundcloud.android.payments.WebCheckoutActivity;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.playback.external.PlaybackActionReceiver;
import com.soundcloud.android.playback.playqueue.PlayQueueFragment;
import com.soundcloud.android.playback.service.PlayerAppWidgetProvider;
import com.soundcloud.android.playback.ui.PlayerFragment;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.widget.PlayerWidgetReceiver;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.playlists.CreatePlaylistDialogFragment;
import com.soundcloud.android.playlists.DeletePlaylistDialogFragment;
import com.soundcloud.android.playlists.LegacyPlaylistDetailFragment;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistDetailFragment;
import com.soundcloud.android.policies.DailyUpdateService;
import com.soundcloud.android.profile.MyFollowingsFragment;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.profile.UserAlbumsActivity;
import com.soundcloud.android.profile.UserAlbumsFragment;
import com.soundcloud.android.profile.UserDetailsFragment;
import com.soundcloud.android.profile.UserFollowersFragment;
import com.soundcloud.android.profile.UserFollowingsFragment;
import com.soundcloud.android.profile.UserLikesActivity;
import com.soundcloud.android.profile.UserLikesFragment;
import com.soundcloud.android.profile.UserPlaylistsActivity;
import com.soundcloud.android.profile.UserPlaylistsFragment;
import com.soundcloud.android.profile.UserRepostsActivity;
import com.soundcloud.android.profile.UserRepostsFragment;
import com.soundcloud.android.profile.UserSoundsFragment;
import com.soundcloud.android.profile.UserTracksActivity;
import com.soundcloud.android.profile.UserTracksFragment;
import com.soundcloud.android.profile.VerifyAgeActivity;
import com.soundcloud.android.search.PlayFromVoiceSearchActivity;
import com.soundcloud.android.search.PlaylistResultsFragment;
import com.soundcloud.android.search.SearchPremiumResultsActivity;
import com.soundcloud.android.search.SearchPremiumResultsFragment;
import com.soundcloud.android.search.SearchResultsFragment;
import com.soundcloud.android.search.TabbedSearchFragment;
import com.soundcloud.android.search.suggestions.SearchSuggestionsFragment;
import com.soundcloud.android.settings.ClearCacheDialog;
import com.soundcloud.android.settings.LegalActivity;
import com.soundcloud.android.settings.LegalFragment;
import com.soundcloud.android.settings.LicensesActivity;
import com.soundcloud.android.settings.OfflineSettingsActivity;
import com.soundcloud.android.settings.OfflineSettingsFragment;
import com.soundcloud.android.settings.SettingsActivity;
import com.soundcloud.android.settings.notifications.NotificationPreferencesActivity;
import com.soundcloud.android.settings.notifications.NotificationPreferencesFragment;
import com.soundcloud.android.stations.LikedStationsActivity;
import com.soundcloud.android.stations.LikedStationsFragment;
import com.soundcloud.android.stations.StationInfoActivity;
import com.soundcloud.android.stations.StationInfoFragment;
import com.soundcloud.android.stream.StreamFragment;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncAdapterService;
import com.soundcloud.android.tracks.TrackInfoFragment;
import com.soundcloud.android.upgrade.GoOnboardingActivity;
import com.soundcloud.android.upgrade.UnrecoverableErrorDialog;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.android.view.GlassLinearLayout;
import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = { ApplicationModule.class }
)
public interface ApplicationComponent {
    void inject(SoundCloudApplication application);

    //PlaybackServiceModule
    void inject(PlaybackService playbackService);

    //ApiModule
    void inject(ApiSyncService apiSyncService);

    //SyncModule
    void inject(SyncAdapterService syncAdapterService);

    //WidgetModule
    void inject(PlayerWidgetReceiver playerWidgetReceiver);
    void inject(PlayerAppWidgetProvider playerAppWidgetProvider);

    //LegacyModule
    void inject(UploadService uploadService);

    //GcmModule
    void inject(GcmRegistrationService gcmRegistrationService);
    void inject(GcmInstanceIDListenerService gcmInstanceIDListenerService);
    void inject(ScFirebaseMessagingService scFirebaseMessagingService);
    void inject(GcmDebugDialogFragment gcmDebugDialogFragment);

    //FeaturesModule --------
    //AuthenticationModule
    void inject(AuthenticatorService authenticatorService);
    void inject(LogoutFragment logoutFragment);
    void inject(ResolveActivity resolveActivity);
    void inject(LoginTaskFragment loginTaskFragment);
    void inject(AuthTaskFragment authTaskFragment);
    void inject(GooglePlusSignInTask googlePlusSignInTask);
    void inject(SignupTaskFragment signupTaskFragment);
    void inject(AddUserInfoTaskFragment addUserInfoTaskFragment);
    void inject(OnboardActivity onboardActivity);
    void inject(RecoverActivity recoverActivity);
    void inject(FacebookSessionCallback facebookSessionCallback);

    //ActivitiesModule
    void inject(ActivitiesActivity activitiesActivity);
    void inject(ActivitiesFragment activitiesFragment);

    //ExploreModule
    void inject(ExploreTracksCategoryActivity exploreTracksCategoryActivity);
    void inject(ExploreTracksFragment exploreTracksFragment);
    void inject(ExploreGenresFragment exploreGenresFragment);
    void inject(ExploreActivity exploreActivity);

    //MainModule
    void inject(LauncherActivity launcherActivity);
    void inject(RootActivity rootActivity);
    void inject(MainActivity mainActivity);
    void inject(WebViewActivity webViewActivity);
    void inject(MetadataFragment metadataFragment);
    void inject(DevDrawerFragment devDrawerFragment);
    void inject(FullImageDialog fullImageDialog);

    //Cast
    void inject(CastRedirectActivity castRedirectActivity);
    void inject(CastOptionsProvider castOptionsProvider);

    //PlayerModule
    void inject(PlayerFragment playerFragment);
    void inject(PlaybackActionReceiver playbackActionReceiver);
    void inject(WaveformView waveformView);
    void inject(PlayQueueFragment playQueueFragment);
    void inject(GlassLinearLayout glassLinearLayout);

    //PlaylistModule
    void inject(PlaylistsActivity playlistsActivity);
    void inject(PlaylistsFragment playlistsFragment);
    void inject(NewPlaylistsFragment playlistsFragment);
    void inject(PlaylistDetailActivity playlistDetailActivity);
    void inject(PlaylistDetailFragment playlistDetailFragment);
    void inject(LegacyPlaylistDetailFragment legacyPlaylistDetailFragment);
    void inject(AddToPlaylistDialogFragment addToPlaylistDialogFragment);
    void inject(CreatePlaylistDialogFragment createPlaylistDialogFragment);
    void inject(DeletePlaylistDialogFragment deletePlaylistDialogFragment);

    //ProfileModule
    void inject(ProfileActivity profileActivity);
    void inject(MyFollowingsFragment myFollowingsFragment);
    void inject(UserPlaylistsActivity userPlaylistsActivity);
    void inject(UserPlaylistsFragment userPlaylistsFragment);
    void inject(UserLikesActivity userLikesActivity);
    void inject(UserLikesFragment userLikesFragment);
    void inject(UserFollowingsFragment userFollowingsFragment);
    void inject(UserFollowersFragment userFollowersFragment);
    void inject(UserSoundsFragment userSoundsFragment);
    void inject(UserDetailsFragment userDetailsFragment);
    void inject(VerifyAgeActivity verifyAgeActivity);
    void inject(UserRepostsActivity userRepostsActivity);
    void inject(UserRepostsFragment userRepostsFragment);
    void inject(UserTracksActivity userTracksActivity);
    void inject(UserTracksFragment userTracksFragment);
    void inject(UserAlbumsActivity userAlbumsActivity);
    void inject(UserAlbumsFragment userAlbumsFragment);

    //StreamModule
    void inject(StreamFragment streamFragment);

    //TrackModule
    void inject(TrackInfoFragment trackInfoFragment);

    //CommentsModule
    void inject(TrackCommentsActivity trackCommentsActivity);
    void inject(AddCommentDialogFragment addCommentDialogFragment);
    void inject(CommentsFragment commentsFragment);

    //PaymentsModule
    void inject(NativeConversionActivity nativeConversionActivity);
    void inject(LegacyConversionActivity webConversionActivity);
    void inject(TieredConversionActivity tieredConversionActivity);
    void inject(PlanChoiceActivity planChoiceActivity);
    void inject(WebCheckoutActivity webCheckoutActivity);

    //OfflineModule
    void inject(OfflineContentService offlineContentService);
    void inject(OfflineSettingsStorage offlineSettingsStorage);
    void inject(OfflineLikesDialog offlineLikesDialog);
    void inject(AlarmManagerReceiver alarmManagerReceiver);
    void inject(OfflineSettingsOnboardingActivity offlineSettingsOnboardingActivity);

    //PoliciesModule
    void inject(DailyUpdateService dailyUpdateService);

    //UpgradeModule
    void inject(GoOnboardingActivity goOnboardingActivity);
    void inject(UnrecoverableErrorDialog unrecoverableErrorDialog);

    //DowngradeModule
    void inject(GoOffboardingActivity goOffboardingActivity);
    void inject(GoOffboardingFragment goOffboardingFragment);

    //RecordModule
    void inject(RecordPermissionsActivity recordPermissionsActivity);
    void inject(RecordActivity recordActivity);
    void inject(RecordFragment recordFragment);
    void inject(UploadMonitorFragment uploadMonitorFragment);
    void inject(UploadActivity uploadActivity);

    //UploadModule
    void inject(Uploader uploader);
    void inject(Processor processor);
    void inject(ImageResizer imageResizer);
    void inject(Encoder encoder);

    //LikesModule
    void inject(TrackLikesActivity trackLikesActivity);
    void inject(TrackLikesFragment trackLikesFragment);

    //SettingsModule
    void inject(SettingsActivity settingsActivity);
    void inject(OfflineSettingsActivity offlineSettingsActivity);
    void inject(NotificationPreferencesFragment notificationPreferencesFragment);
    void inject(OfflineSettingsFragment offlineSettingsFragment);
    void inject(ClearCacheDialog clearCacheDialog);
    void inject(NotificationPreferencesActivity notificationPreferencesActivity);
    void inject(LegalActivity legalActivity);
    void inject(LegalFragment legalFragment);
    void inject(LicensesActivity licensesActivity);

    //StationsModule
    void inject(StationInfoActivity stationInfoActivity);
    void inject(StationInfoFragment stationInfoFragment);
    void inject(LikedStationsActivity likedStationsActivity);
    void inject(LikedStationsFragment likedStationsFragment);

    //DiscoveryModule
    void inject(DiscoveryFragment discoveryFragment);
    void inject(ViewAllRecommendedTracksActivity viewAllRecommendedTracksActivity);
    void inject(ViewAllRecommendedTracksFragment viewAllRecommendedTracksFragment);
    void inject(SearchActivity searchActivity);
    void inject(SearchPremiumResultsActivity searchPremiumResultsActivity);
    void inject(PlaylistDiscoveryActivity playlistDiscoveryActivity);
    void inject(TabbedSearchFragment tabbedSearchFragment);
    void inject(SearchResultsFragment searchResultsFragment);
    void inject(SearchSuggestionsFragment searchSuggestionsFragment);
    void inject(PlaylistResultsFragment playlistResultsFragment);
    void inject(PlayFromVoiceSearchActivity playFromVoiceSearchActivity);
    void inject(SearchPremiumResultsFragment searchPremiumResultsFragment);
    void inject(ChartActivity chartActivity);
    void inject(ChartTracksFragment chartTracksFragment);
    void inject(AllGenresActivity allGenresActivity);
    void inject(GenresFragment genresFragment);

    //CollectionModule
    void inject(CollectionFragment collectionFragment);
    void inject(CollectionPreviewView collectionPreviewView);
    void inject(ConfirmRemoveOfflineDialogFragment confirmRemoveOfflineDialogFragment);
    void inject(PlayHistoryActivity playHistoryActivity);
    void inject(PlayHistoryFragment playHistoryFragment);
    void inject(RecentlyPlayedActivity recentlyPlayedActivity);
    void inject(RecentlyPlayedFragment recentlyPlayedFragmen);
    void inject(MoreFragment moreFragment);
    //Features Module --------
}
