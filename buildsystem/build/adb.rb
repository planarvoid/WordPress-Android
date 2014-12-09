module Build
  class Adb

    DEFAULT_LEVELS  = %w(
      SoundCloudApplication CloudPlaybackService ImageLoader
      StreamProxy StreamLoader C2DMReceiver SyncAdapterService ScContentProvider DBHelper
      ApiSyncService ApiSyncer UploadService VorbisEncoder VorbisEncoderNative
      VorbisDecoderNative SoundRecorder WavWriter AndroidCloudAPI FacebookSSO NetworkConnectivityListener
      EventLogger PlayEventTrackerApi ReactiveScheduler ScObservables ReactiveListFragment ActivitiesFragment
      DetachableObserver SyncOperations ActivitiesStorage PlaylistStorage TrackStorage FollowingOperations
      LoadItemsObserver EventBus Propeller CastManager BaseCastManager
    )
    DISABLED_LEVELS = %w()

    def initialize(device_or_emulator = :device)
      @device_or_emulator = device_or_emulator
    end

    def install
      Build.run_command uninstall_command
      Build.run_command install_command
      Build.run_command start_application
    end

    def set_log_level(level)
      (DEFAULT_LEVELS - DISABLED_LEVELS).each do |tag|
        Build.run_command "#{adb} shell setprop log.tag.#{tag} #{level.upcase}"
      end
      DISABLED_LEVELS.each do |tag|
        Build.run_command "#{adb} shell setprop log.tag.#{tag} error"
      end
        Build.run_command "#{adb} shell setprop debug.assert 1"
    end

    def monkey_test
      count = ENV['COUNT'] || '10000'
      seed  = ENV['SEED'] || Time.now.to_i.to_s

      Build.run_command "adb shell monkey -p #{Build.package_name} --throttle 250 -s #{seed} #{count}"
    end

    private
    def uninstall_command
      'adb uninstall com.soundcloud.android'
    end

    def start_application
      'adb shell am start -n com.soundcloud.android/com.soundcloud.android.main.MainActivity'
    end

    def install_command
      "adb install #{Build.apk_path}"
    end

    def adb
      flag = @device_or_emulator == :device ? '-d' : '-e'
      adb_path = "#{android_home}/platform-tools/adb"

      "#{adb_path} #{flag}"
    end

    def android_home
      dir = %w(ANDROID_HOME ANDROID_SDK_ROOT ANDROID_SDK_HOME).map { |e| ENV[e] }.compact.find { |d| File.directory?(d) }
      dir or raise "no android home defined"
    end
  end
end
