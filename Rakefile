require 'rexml/document'
require 'net/https'
require 'uri'
require 'pp'
require 'csv'
require 'yaml'

c2dm_credentials = 'c2dm.credentials'
file c2dm_credentials => 'c2dm:login'

DEFAULT_LEVELS = %w(CloudPlaybackService
               AwesomePlayer
               NuHTTPDataSource
               HTTPStream
               NuCachedSource2
               StreamProxy
               StreamLoader
               StreamStorage
               C2DMReceiver
               SyncAdapterService
               ScContentProvider
               ApiSyncService
               ATTracker
               UploadService
               SoundCloudApplication
              )

[:device, :emu].each do |t|
  def android_home
    dir = %w(ANDROID_HOME ANDROID_SDK_ROOT ANDROID_SDK_HOME).map { |e| ENV[e] }.find { |d| File.directory?(d) }
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
          adb["shell su -c 'cp -f #{db_path} #{tmp_path}'"]
          adb["pull #{tmp_path} ."]
        end
    end

    namespace :logging do
      %w(verbose debug info warn error).each do |level|
        task level do
          DEFAULT_LEVELS.each do |tag|
            adb["shell setprop log.tag.#{tag} #{level.upcase}"]
          end
          adb["shell setprop debug.assert 1"]
        end
      end
    end

    desc "run lolcat with filtering"
    task :lolcat do
      adb["lolcat -v time #{DEFAULT_LEVELS.join(' ')} *:S"]
    end

    desc "run integration tests"
    task :test do
      adb['shell', 'am', 'instrument', '-r', '-w', package.to_s+'.tests/android.test.InstrumentationTestRunner']
    end

    desc "runs a single integration test [CLASS=com.soundcloud...]"
    task :test_single do
      adb['shell', 'am', 'instrument', '-r', '-w', '-e', 'class', ENV['CLASS'],
          package.to_s+'.tests/android.test.InstrumentationTestRunner']
    end

    task :anr do
      adb["pull /data/anr/traces.txt"]
    end

    task :screenshots do
      adb["pull /sdcard/Robotium-Screenshots"]
    end
  end
end

def manifest
  @manifest ||= REXML::Document.new(File.read('app/AndroidManifest.xml'))
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

namespace :release do
  desc "tag the current release"
  task :tag do
    if versionName.to_s =~ /-BETA(\d+)?\Z/
      raise "#{versionName}: Not a release version"
    else
      sh "git tag -a #{versionName} -m #{versionName} && git push --tags && git push"
    end
  end
end

namespace :beta do
  BUCKET = "soundcloud-android-beta"
  DEST="s3://#{BUCKET}/#{package}-#{versionCode}.apk"
  CURRENT="s3://#{BUCKET}/#{package}-current.apk"
  BETA_APK = "app/target/soundcloud-android-beta-#{versionName}-market.apk"

  file BETA_APK => 'beta:build'

  desc "build beta"
  task :build do
    sh "sbt 'project soundcloud-android-beta' clean android:prepare-market"
  end

  desc "install beta on device"
  task :install => BETA_APK do
    sh "adb -d install -r #{BETA_APK}"
  end

  task :verify do
    raise "Missing file: #{BETA_APK}" unless File.exists?(BETA_APK)
    raise "#{BETA_APK} does not contain -BETA" unless BETA_APK.match(/-BETA/)

    output = `jarsigner -verify -certs -verbose #{BETA_APK}`
    raise unless $?.success?
    if output !~ /CN=SoundCloud Android Beta/
      raise "Wrong signature"
    end
  end

  [ :push, :release, :publish ].each { |a| task a => :upload }

  desc "upload beta to s3"
  task :upload => :verify do
    metadata = {
      'android-versionname' => versionName,
      'android-versioncode' => versionCode,
      'git-sha1'            => gitsha1
    }
    sh "s3cmd -P put #{BETA_APK} " +
       "--mime-type=application/vnd.android.package-archive " +
       metadata.inject([]) { |m,(k,v)|
         m << "--add-header=x-amz-meta-#{k}:#{v}"
         m
       }.join(' ') +
       (ENV['DRYRUN'] ? ' --dry-run ' : ' ') +
       DEST

   sh "s3cmd cp --acl-public #{DEST} #{CURRENT}"
  end

  desc "list beta bucket contents"
  task :list do
    http_url = "http://#{BUCKET}.s3.amazonaws.com"
    out = `curl -s #{http_url}`
    if $?.success?
      doc = REXML::Document.new(out)
      doc.write(STDOUT, 4)
      puts
      doc.elements.to_a("//Key").each do |key|
        sh "curl", '-I', "#{http_url}/#{key.text}"
      end
    else
      raise "error getting bucket: #{$?}"
    end
  end

  desc "tag the current beta"
  task :tag do
    if versionName.to_s =~ /-BETA(\d+)?\Z/
      sh "git tag -a #{versionName} -m #{versionName} && git push --tags && git push"
    else
      raise "#{versionName}: not a beta version"
    end
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
    :debug => { :alias => 'androiddebugkey', :keystore => '~/.android/debug.keystore' },
    :beta  => { :alias => 'beta-key', :keystore => 'soundcloud_sign/soundcloud.ks' },
    :production  => { :alias => 'jons keystore', :keystore => 'soundcloud_sign/soundcloud.ks' }
  }.each do |type, config|
    desc "show keyhash for #{type}"
    task type do
     sh "keytool -exportcert -alias '#{config[:alias]}' -keystore #{config[:keystore]} | openssl sha1 -binary | openssl base64"
    end
  end
end
