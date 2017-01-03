## encoding: utf-8
require 'rake'
require 'android/publisher'

module Build
  class RakeHelper
    include Rake::DSL

    def self.tasks
      new.tasks
    end

    def tasks
      build_task
      device_tasks
      analysis_tasks

      namespace :upload do
        upload_tasks
      end

      namespace :github do
        github_tasks
      end

      namespace :adb do
        adb_tasks
      end

      namespace :test do
        tests_tasks
      end

      namespace :release do
        release_tasks
      end

      namespace :hotfix do
        hotfix_tasks
      end

      namespace :repack do
        default_options = {:dryrun => ENV['REPACK_DRY']}

        desc "Roll your own Guava JAR!"
        task :guava do
          options = {
            :pg_rules => [
              '-keep class com.google.common.annotations.VisibleForTesting',
              '-keep class com.google.common.net.HttpHeaders { *; }'
            ]
          }
          Repacker.repack "com/google/common", "com.google.guava", "guava", default_options.merge(options)
        end

        desc "Roll your own Jackson JAR!"
        task :jackson do
          Repacker.repack "com/fasterxml/jackson", "com.fasterxml.jackson.core", "jackson"
        end

        desc "Roll your own Google Play Services AAR!"
        task :google_play do
          options = {
            :aar => true,
            :path_filter => /\/common/, # Guava stuff is under com/google as well, ignore it
            :pg_rules => [
              '-keep class com.google.android.gms.auth.GoogleAuthUtil { *; }',
              '-keep class com.google.android.gms.cast.** { *; }',
              '-keepclassmembers class com.google.android.gms.cast.** { *; }'
            ]
          }
          Repacker.repack "com/google", "com.google.android.gms", "play-services", default_options.merge(options)
        end
      end
    end

    private
    def adb
      Adb.new
    end

    def adb_tasks
      desc "installs tha application on device"
      task :install => Build.apk_path do
        adb.install
      end
    end

    def device_tasks
      [:device, :emu].each do |ns|
        namespace ns do
          namespace :logging do
            %w(verbose debug info warn error).each do |level|
              task level do
                Adb.new(ns).set_log_level(level.upcase)
              end
            end
          end
        end
      end
    end

    def mvn_task(name)
      Mvn.task(name)
    end

    def github_tasks

      desc 'Creates github issues as a base for regression tests'
      task :regression_tests do
        github.create_regression_tests_checklist("Regression tests: #{Build.version}")
      end

      desc 'Creates a GitHub issue with a release checklist'
      task :release_checklist do
        github.create_release_checklist
      end

      desc 'Commits versionCode bump to github'
      task :commit_versioncode_bump do
        new_version_code = Build.version_code.to_i + 1

        message = "VersionCode bumped to: #{new_version_code}"
        git.commit_a(message)

        git.push
        puts message
      end
    end

    def build_task
      file Build.apk_path do
        Rake::Task['build'].invoke
      end

      file Build.artifact_path do
        Rake::Task['build'].invoke
      end

      desc "builds the project, you can build different versions by setting environment variable (BUILD_ENV=beta rake build)"
      task :build do
        update_manifest
        build
      end

      desc "tags the current version"
      task :tag do
        git.tag
      end
    end

    def update_manifest
      puts "Removing DEBUG_ONLY permissions from manifest" if Configuration.build_env != 'debug'
      manifest_path = 'app/AndroidManifest.xml'

      new_file = IO.readlines(manifest_path).map do |line|
        line unless line.include?("DEBUG_ONLY")
      end

      File.open(manifest_path, 'w+') do |android_manifest_file|
        android_manifest_file << new_file.join
      end
    end

    def build
      mvn_task(:build).execute

      git.checkout('app/AndroidManifest.xml')

      if Build.ci?
        apk_name = Build.apk_file_name
        cp(
          "app/target/#{apk_name}",
          "app/target/#{apk_name.gsub('.apk','')}-#{Build.build_number}.apk"
        )
      end
    end

    def release_tasks
      desc "starts new release, merges to the release branch and pushes it to origin; updates version in development branch"
      task :start do
        start_release
      end

      desc "tags the release branch, deploys the apk to github releases"
      task :finish do
        finish_release
      end
    end

    def start_release
      release.start
    end

    def finish_release
      release.finish
    end

    def hotfix_tasks
      desc "starts new hotfix, creates new branch from last release"
      task :start do
        hotfix.start
      end

      desc "finishes hotfix, pushes changes to release and development branch"
      task :finish do
        hotfix.finish
      end
    end

    def version_tasks
      desc "shows current version"
      task :show do
        puts Build.version
      end
    end

    def analysis_tasks
      desc "runs pmd analysis"
      task :pmd do
        Mvn.pmd.execute
      end

      desc "runs lint analysis"
      task :lint do
        Mvn.lint.execute
      end

      desc "runs findbugs analysis"
      task :findbugs do
        Mvn.findbugs.execute
      end
    end

    def tests_tasks
      desc "runs unit tests"
      task :unit do
        mvn_task(:unittests).execute
        git.checkout('app/AndroidManifest.xml')
      end

      desc "runs acceptance tests"
      task :acceptance => Build.apk_path do
        begin
          mvn_task(:phone_test).execute
        rescue
          Device.get_screenshots
        end
      end

      desc "runs monkey tests"
      task :monkey do
        Adb.new.monkey_test
      end
    end

    def upload_tasks
      desc "uploads to Google Play Store"
      task :store do
        case Configuration.build_env
          when 'alpha'
            android_store_publisher.deploy_to_alpha
          when 'beta'
            android_store_publisher.deploy_to_beta
          when 'release'
            android_store_publisher.deploy_to_beta
          else
            raise 'Unsupported build type'
        end

        new_version_code = Build.version_code.to_i + 1

        Mvn.update_manifest.set_version_code(new_version_code).execute

        message = "VersionCode bumped to: #{new_version_code}"
        git.commit_a(message)

        git.push
        puts message

      end

      desc "creates a new release and uploads to Github"
      task :github do
        release = JSON.parse(github.create_release(Build.version_name, release_name, release_body).body)
        github.upload_apk(release['id'], Build.apk_path)
      end
    end

    def check_git
      raise "Uncommitted changes in working tree" if git.uncommited_changes?
    end

    def git
      Git.new
    end

    def github
      Github.new('0330604618ffbc5232a62f3e96f9d3b25377b9cb')
    end

    def android_store_publisher
      Android::Publisher.new("com.soundcloud.android", Build.apk_path)
    end

    def release
      ReleaseStrategy.new(git, Mvn)
    end

    def hotfix
      HotfixStrategy.new(git, Mvn)
    end

    def release_name
      ENV['RELEASE_NAME'] || Build.version_name
    end

    def release_body
      ENV['RELEASE_BODY'] || ''
    end
  end
end
