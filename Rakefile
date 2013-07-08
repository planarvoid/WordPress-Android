require 'rubygems'
require 'rexml/document'
require 'net/https'
require 'uri'
require 'pp'
require 'csv'
require 'json'
require 'yaml'

DEFAULT_LEVELS = %w(
   CloudPlaybackService AwesomePlayer NuHTTPDataSource HTTPStream NuCachedSource2 ImageLoader
   StreamProxy StreamLoader StreamStorage C2DMReceiver SyncAdapterService ScContentProvider DBHelper
   ApiSyncService ApiSyncer UploadService SoundCloudApplication VorbisEncoder VorbisEncoderNative
   VorbisDecoderNative SoundRecorder WavWriter AndroidCloudAPI FacebookSSO NetworkConnectivityListener
   PlayEventTracker PlayEventTrackerApi ReactiveScheduler ScObservables ReactiveListFragment ActivitiesFragment
   DetachableObserver SyncOperations ActivitiesStorage PlaylistStorage TrackStorage FollowingOperations
   LoadItemsObserver
)
DISABLED_LEVELS = %w()

# help methods to access pom data
def pom() @pom ||= REXML::Document.new(File.read(File.dirname(__FILE__)+'/pom.xml')) end
def current_version() pom.root.elements["version"].text end
def update_version(new_version)
  sh "mvn versions:set -DnewVersion=#{new_version} -DgenerateBackupPoms=false -DupdateMatchingVersions=false"
end

[:device, :emu].each do |t|
  def android_home
    dir = %w(ANDROID_HOME ANDROID_SDK_ROOT ANDROID_SDK_HOME).map { |e| ENV[e] }.compact.find { |d| File.directory?(d) }
    dir or raise "no android home defined"
  end
  def package() "com.soundcloud.android" end
  adb = lambda do |*args|
    flag = (t == :device ? '-d' : '-e')
    adb_path = "#{android_home}/platform-tools/adb"
    if args.size == 1
      sh "#{adb_path} #{flag} #{args.first}"
    else
      sh adb_path, *args.unshift(flag)
    end
  end

  test_runner = lambda { "android.test.InstrumentationTestRunner" }

  namespace t do
    namespace :beta do
      beta_path = "/mnt/sdcard/Android/data/com.soundcloud.android/files/beta"
      desc "delete beta cache"
      task :delete do
        adb["shell 'rm #{beta_path}/*'"]
      end

      desc "list beta cache"
      task :list do
        adb["shell 'ls #{beta_path}'"]
        adb["shell 'cat #{beta_path}/*.json'"]
      end
    end

    namespace :prefs do
      pref_path = "/data/data/#{package}/shared_prefs/#{package}_preferences.xml"
      desc "get prefs from #{t}"
        task :pull do
          adb["pull #{pref_path} ."]
        end

        desc "pushes prefs to #{t}"
        task :push do
          adb["push #{package}_preferences.xml #{pref_path}"]
        end
    end

    namespace :db do
      # needs root on device
      db_path = "/data/data/#{package}/databases/SoundCloud"
      tmp_path = "/sdcard/SoundCloud.sqlite"
      desc "get db from #{t}"
        task :pull do
          case t
            when :device;
              adb["shell su -c 'cp -f #{db_path} #{tmp_path}'"]
              adb["pull #{tmp_path} ."]
            when :emu; adb["pull #{db_path} ."]
          end
        end
    end

    namespace :logging do
      %w(verbose debug info warn error).each do |level|
        task level do
          (DEFAULT_LEVELS - DISABLED_LEVELS).each do |tag|
            adb["shell setprop log.tag.#{tag} #{level.upcase}"]
          end

          DISABLED_LEVELS.each do |tag|
            adb["shell setprop log.tag.#{tag} error"]
          end
          adb["shell setprop debug.assert 1"]
        end
      end
    end

    desc "run lolcat with filtering"
    task :lolcat do
      adb["lolcat -v time #{(DEFAULT_LEVELS - DISABLED_LEVELS).join(' ')} *:S"]
    end

    desc "run integration tests"
    task :test do
      adb['shell', 'am', 'instrument', '-r', '-w', package.to_s+'.tests/'+test_runner.call]
    end

    desc "runs a single integration test [CLASS=com.soundcloud...]"
    task :test_single do
      adb['shell', 'am', 'instrument', '-r', '-w', '-e', 'class', ENV['CLASS'],
          package.to_s+'.tests/'+test_runner.call]
    end

    desc "runs the monkey [COUNT=x] [SEED=y]"
    task :monkey do
      adb['shell', 'monkey', '-p', package.to_s, '-v',
          '--throttle', '250',
          '-s', ENV['SEED'] || Time.now.to_i.to_s,
          ENV['COUNT'] || '10000'
      ]
    end

    task :anr do
      adb["pull /data/anr/traces.txt"]
    end

    task :redirect_stdio_true do
      adb["shell setprop log.redirect-stdio true"]
    end
    task :redirect_stdio_false do
      adb["shell setprop log.redirect-stdio false"]
    end

    task :remove_camera do
      adb["remount"]
      adb["shell rm -r /system/app/Gallery*apk /system/app/Camera.apk"]
    end
  end
end


# help methods to access manifest data
def manifest
  @manifest ||= REXML::Document.new(File.read(File.dirname(__FILE__)+'/app/AndroidManifest.xml'))
end

def versionCode() manifest.root.attribute('versionCode') end
def versionName() manifest.root.attribute('versionName') end
def package() manifest.root.attribute('package') end

def gitsha1()
  `git rev-parse HEAD`.tap do |head|
    raise "could not get current HEAD" unless $?.success?
    head.strip!
  end
end

namespace :debug do
    desc "Build Debug APK"
    task :build do
        sh "mvn clean install -Psign --projects app"
    end

    task :build_deploy => :build do
        sh "mvn android:deploy --projects app"
    end
end

namespace :test do
    desc "Build Debug APK"
    task :unit do
        sh "mvn clean install -Dandroid.proguard.skip=true --projects app && mvn clean test --projects tests"
    end
end

namespace :release do
  desc "tag the current release"
  task :tag do
    if versionName.to_s =~ /-BETA(\d+)?\Z/
      raise "#{versionName}: Not a release version"
    else
      sh "git tag -a #{versionName} -m #{versionName} && git push --tags && git push"
    end
  end

  desc "builds the release version"
  task :build do
    sh "mvn clean install -DskipTests -Psign,soundcloud,release"
  end

  desc "sets the release version to the version specified in the manifest, creates bump commit"
  task :bump do
    raise "#{versionName}: Not a release version" if versionName.to_s =~ /-BETA(\d+)?\Z/
    raise "Uncommitted changes in working tree" unless system("git diff --exit-code --quiet")
    update_version(versionName)
    sh "git commit -a -m 'Bumped to #{versionName}'"
  end
end

namespace :beta do
  APP_ID="31bb3a437ee0cd325e994283fb8e7da3"
  TOKEN="ef31e73570804365acba701c47568c9d"
  BETA_APK = "app/target/soundcloud-android-#{current_version}-beta.apk"
  file BETA_APK => 'beta:build'

  def get_last_published_version
    versions = JSON.parse(`curl -s -H 'X-HockeyAppToken: #{TOKEN}' https://rink.hockeyapp.net/api/2/apps/#{APP_ID}/app_versions`)
    versions['app_versions'].map { |app| app['version'].to_i }.max
  end

  task :versions do
    sh "curl -H 'X-HockeyAppToken: #{TOKEN}' https://rink.hockeyapp.net/api/2/apps/#{APP_ID}/app_versions"
  end

  task :apps do
    sh "curl -H 'X-HockeyAppToken: #{TOKEN}' https://rink.hockeyapp.net/api/2/apps"
  end

  desc "build and upload beta, then tag it"
  task :upload => [ BETA_APK, :verify ] do
    sh <<-END
      curl \
        -H "X-HockeyAppToken: #{TOKEN}" \
        -F "status=2" \
        -F "notify=1" \
        -F "notes_type=1" \
        -F "notes=#{last_release_notes}" \
        -F "ipa=@#{BETA_APK}" \
        https://rink.hockeyapp.net/api/2/apps/#{APP_ID}/app_versions
    END
    # undo changes caused by build
    sh "git checkout app/AndroidManifest.xml"

    Rake::Task['beta:tag'].invoke
  end

  desc "build beta"
  task :build do
    with_beta do |version|
      version_code = get_last_published_version
      if version_code.nil?
        version_code = 0
      end
      sh <<-END
        mvn clean install -DskipTests -Psign,soundcloud,beta \
          -Dandroid.manifest.versionCode=#{version_code+1} \
          -Dandroid.manifest.debuggable=true
      END
    end
  end

  desc "install beta on device"
  task :install => BETA_APK do
    sh "adb -d install -r #{BETA_APK}"
  end

  task :verify do
    raise "Missing file: #{BETA_APK}" unless File.exists?(BETA_APK)
    raise "#{BETA_APK} does not contain -BETA" unless BETA_APK.match(/-BETA/i)

    output = `jarsigner -verify -certs -verbose #{BETA_APK}`
    raise unless $?.success?
    if output !~ /CN=SoundCloud Android Beta/
      raise "Wrong signature"
    end
  end

  desc "list beta versions"
  task :list => :versions

  desc "tag the current beta"
  task :tag do
    with_beta do |version|
      sh "git tag -a #{version} -m #{version} && git push --tags && git push"
    end
  end

  def with_beta(&block)
    version = current_version.gsub(/-SNAPSHOT\Z/, '')
    if version.to_s =~ /-BETA(\d+)?\Z/
      block.call(version)
    else
      raise "#{version}: not a beta version"
    end
  end

  desc "bump the beta version"
  task :bump do
    version = current_version.gsub(/-SNAPSHOT\Z/, '')
    match = version[/-BETA(\d+)/]
    raise "Not a beta version" unless match
    raise "Uncommitted changes in working tree" unless system("git diff --exit-code --quiet")

    current_beta = $1.to_i
    new_version = version.gsub(/BETA#{current_beta}\Z/, "BETA#{current_beta+1}")
    update_version(new_version)
    sh "git commit -a -m 'Bumped to #{new_version}'"
  end
end

namespace :lol do
  STRINGS = "app/res/values/strings_lolcatlizr.xml"

  desc "download lolcatlizr manged string resources"
  task :fetch do
    url = "http://lol.iriscouch.com/lolcatlizr/_design/lolcatlizr/_list/android/android?key=%22%22"
    sh "curl #{url} > #{STRINGS}"
  end

  desc "revert changes"
  task :revert do
    sh "git co #{STRINGS}"
  end

  task :csv do
    url = "http://lol.iriscouch.com/lolcatlizr/_design/lolcatlizr/_list/csv/android?key=%22%22"
    sh "curl #{url} > strings.csv"
  end
end

# use for the facebook integration
namespace :keyhash do
  {
    :debug => { :alias => 'androiddebugkey', :keystore => "#{File.expand_path(__FILE__)}/debug.keystore" },
    :beta  => { :alias => 'beta-key', :keystore => 'soundcloud_sign/soundcloud.ks' },
    :production  => { :alias => 'jons keystore', :keystore => 'soundcloud_sign/soundcloud.ks' }
  }.each do |type, config|
    desc "show keyhash for #{type}"
    task type do
     sh "keytool -exportcert -alias '#{config[:alias]}' -keystore #{config[:keystore]} | openssl sha1 -binary | openssl base64"
    end
  end
end

desc "regenerate the NOTICE file"
task :regenerate_notice do
  require 'erb'
  notice = File.read(File.dirname(__FILE__)+"/NOTICE")
  entries = notice.split("\n\n").map { |e| e.gsub(/\([cC]\)/, "&copy;").split("\n") }
  template = ERB.new(<<END, nil , '-')
<html>
  <!-- NOTE: this file autogenerated - edit NOTICE instead and run 'rake regenerate_notice' -->
  <body>
  <%- entries.each do |e| %>
    <p>
      <%= e.first %><br/>
      <%= e.last %>
    </p>
  <%- end %>
  </body>
</html>
END
  output = template.result(binding)
  notice_out =File.dirname(__FILE__)+"/app/assets/about.html"
  File.open(notice_out, 'w')  { |f| f << output }
end

def last_release_notes
  changes = []
  IO.read('CHANGES').split("\n")[1..-1].each do |l|
     break if l[0] && l[0].chr == '$'
     changes << l.gsub(/\s*\*/, ' *') if (l =~ /s*\*/)
  end
  changes.join("\n")
end

desc "run lint"
task :lint do
  result = "app/target/lint-result.html"
  rm_f result
  lint_ok = system("#{android_home}/tools/lint --config app/lint.xml --exitcode --html #{result} app")
  if File.exists?(result) && RUBY_PLATFORM =~ /darwin/
    sh "open #{result}"
  end
  raise "Lint failure" unless lint_ok
end

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
