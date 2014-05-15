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
      codestyle_task
      analysis_tasks
      namespace :hockey do
        hockey_tasks
      end

      namespace :upload do
        upload_tasks
      end

      namespace :adb do
        adb_tasks
      end

      namespace :test do
        tests_tasks
      end

      namespace :version do
        version_tasks
      end

      namespace :release do
        release_tasks
      end

      namespace :hotfix do
        hotfix_tasks
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

    def codestyle_task
      desc "set up shared codestyle in IntelliJ IDEA"
      task :setup_codestyle do
        template = File.join(Rake.original_dir, '.idea-codestyle.xml')
        pref_dirs = Dir.glob(ENV['HOME'] +'/Library/Preferences/*Idea*').
          map { |d| d + '/codestyles' }.
          select { |d| File.directory?(d) }

        pref_dirs.each do |dir|
          sh "ln -sf #{template} #{dir}/SoundCloud-Android.xml"
        end
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

    def build_task
      file Build.apk_path do
        Rake::Task['build'].invoke
      end

      file Build.artifact_path do
        Rake::Task['build'].invoke
      end

      desc "builds the project, you can build different versions by setting environment variable (BUILD_ENV=beta rake build)"
      task :build do
        build
      end

      desc "tags the current version"
      task :tag do
        git.tag
      end
    end

    def build
      if Build::Configuration.hockey.enabled? && Build.ci?
        Build.version_code=(hockey.last_published_version + 1)
      end

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
      namespace :bump do

        [ :minor, :major, :revision ].each do |name|
          desc "bumps #{name} version \t# Does not push the change to remote repo #"
          task(name) do
            check_git
            Build.version_code.to_i + 1
            bump_version(name)
          end
        end

      end
    end

    def bump_version(part)
      current = Build.version.to_s

      Build.version.bump!(part)

      Mvn.update_version.execute
      Mvn.update_manifest.set_version_code(Build.version_code.to_i + 1).execute

      message = "Version: #{current} bumped to: #{Build.version}"

      git.commit_a(message)
      puts message
    end

    def hockey
      Hockey.new
    end

    def hockey_tasks
      desc "uploads to hockey"
      task :upload => Build.artifact_path do
        hockey_upload
      end

      desc "lists last 10 hockeyapp versions"
      task :list do
        hockey_list
      end
    end

    def hockey_upload
      if Build::Configuration.hockey.enabled?
        hockey.upload(Build.artifact_path)
      end
    end

    def hockey_list
      if Build::Configuration.hockey.enabled?
        hockey.list
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
      desc "uploads to hockey"
      task :hockey do
        if Build::Configuration.hockey.enabled?
          hockey.upload(Build.artifact_path)
        end
      end

      desc "uploads to Google Play Store"
      task :store do
        publisher = Android::Publisher.new("com.soundcloud.android", Build.apk_path)
        publisher.deploy_to_beta
      end
    end

    def check_git
      raise "Uncommitted changes in working tree" if git.uncommited_changes?
    end

    def git
      Git.new
    end

    def release
      ReleaseStrategy.new(git, Mvn)
    end

    def hotfix
      HotfixStrategy.new(git, Mvn)
    end
  end
end
