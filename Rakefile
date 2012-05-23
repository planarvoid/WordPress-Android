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
              )

[:device, :emu].each do |t|
  def package() "com.soundcloud.android" end
  flag = (t == :device ? '-d' : '-e')
    namespace t do

      namespace :beta do
        beta_path = "/mnt/sdcard/Android/data/com.soundcloud.android/files/beta"
        desc "delete beta cache"
        task :delete do
          sh "adb #{flag} shell 'rm #{beta_path}/*'"
        end

        desc "list beta cache"
        task :list do
          sh "adb #{flag} shell 'ls #{beta_path}'"
          sh "adb #{flag} shell 'cat #{beta_path}/*.json'"
        end
      end

      namespace :prefs do
        pref_path = "/data/data/#{package}/shared_prefs/#{package}_preferences.xml"
        desc "get prefs from #{t}"
          task :pull do
            sh "adb #{flag} pull #{pref_path} ."
          end

          desc "pushes prefs to #{t}"
          task :push do
            sh "adb #{flag} push #{package}_preferences.xml #{pref_path}"
          end
      end

      namespace :db do
        # needs root on device
        db_path = "/data/data/#{package}/databases/SoundCloud"
        tmp_path = "/sdcard/SoundCloud.sqlite"
        desc "get db from #{t}"
          task :pull do
            sh "adb shell su -c 'cp -f #{db_path} #{tmp_path}'"
            sh "adb #{flag} pull #{tmp_path} ."
          end
      end

      namespace :logging do
        %w(verbose debug info warn error).each do |level|
          task level do
            DEFAULT_LEVELS.each do |tag|
              sh "adb #{flag} shell setprop log.tag.#{tag} #{level.upcase}"
            end
          end
        end

      end

      task :lolcat do
        sh "adb #{flag} lolcat -v time #{DEFAULT_LEVELS.join(' ')} *:S"
      end
   end
end

def manifest
  @manifest ||= REXML::Document.new(File.read('AndroidManifest.xml'))
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

task :anr do
  sh "adb -e pull /data/anr/traces.txt"
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
  BETA_APK = "target/soundcloud-android-beta-#{versionName}-market.apk"

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

namespace :c2dm do
  CL_URL   = URI.parse("https://www.google.com/accounts/ClientLogin")
  C2DM_URL = URI.parse("https://android.apis.google.com/c2dm/send")

  def login(opts={})
    raise "You need to set PASSWORD env" if ENV['PASSWORD'].nil?
    params = {
        'accountType' => 'HOSTED_OR_GOOGLE',
        'Email'       => ENV['EMAIL'] || 'android-c2dm@soundcloud.com',
        'Passwd'      => ENV['PASSWORD'],
        'service'     => 'ac2dm',
        'source'      => "SoundCloud-Android-#{versionName}"
    }.merge(opts)
    http = Net::HTTP.new(CL_URL.host, CL_URL.port)
    http.use_ssl = true
    req = Net::HTTP::Post.new(CL_URL.path)
    req.set_form_data(params)
    resp = http.start do |h|
      h.request(req)
    end
    case resp
      when Net::HTTPSuccess
        Hash[resp.body.split(/\n/).map {|l| l.split('=', 2) }]
      when Net::HTTPForbidden
        STDERR.puts "Forbidden (403)"
        keys = Hash[resp.body.split(/\n/).map {|l| l.split('=', 2) }]
        if keys['Error'] == 'CaptchaRequired'
          STDERR.puts "captcha required, enter below and hit enter"
          url = keys['CaptchaUrl']
          sh "open https://www.google.com/accounts/" + url
          c = STDIN.readline.strip
          login('logintoken'=>keys['CaptchaToken'], 'logincaptcha'=>c)
        end
    else
      STDERR.puts "Error logging in: #{resp.inspect}"
      nil
    end
  end



  desc "get a client login token (PASSWORD=)"
  task :login do
    if !File.exists?(c2dm_credentials) && tokens = login
      File.open(c2dm_credentials, 'w') do |f|
        f.puts "export TOKEN="+ tokens['Auth']
      end
    end
  end

  def post(reg_id, data={}, collapse_key='key', opts={})
    raise "need reg_id" if reg_id.nil?

    params = {
      'registration_id' => reg_id,
      'collapse_key'    => collapse_key,
    }.merge(opts)
    data.each do |k, v|
      params["data.#{k}"] = v
    end

    http = Net::HTTP.new(C2DM_URL.host, C2DM_URL.port)
    http.use_ssl = true
    http.verify_mode = OpenSSL::SSL::VERIFY_NONE
    req = Net::HTTP::Post.new(C2DM_URL.path, {
      'Authorization' => "GoogleLogin auth=#{ENV['TOKEN']}"
    })
    req.set_form_data(params)

    resp = http.start do |h|
      h.request(req)
    end
    case resp
      when Net::HTTPSuccess
        Hash[*resp.body.strip.split("=", 2)]
     when Net::HTTPUnauthorized
        STDERR.puts "unauthorized: #{resp.inspect}"
        nil
     when Net::HTTPServiceUnavailable
        STDERR.puts "service unavailable: #{resp.inspect}"
        nil
     else
        STDERR.puts "unexpected response: #{resp.inspect}"
        nil
     end
  end

  desc "post a message (REG_ID=)"
  task :post => c2dm_credentials do
    ENV['TOKEN'] = IO.read(c2dm_credentials)[/\Aexport TOKEN=(.+)$/, 1]
    puts post(ENV['REG_ID'], 'event_type' => 'like')
  end
end

namespace :lol do
  STRINGS = "res/values/strings_lolcatlizr.xml"

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

namespace :doc do
  desc "Render markdown as if it were shown on github, expects FILE=path/to/doc.md"
  task :preview do
    infile = File.expand_path(ENV['FILE'].to_s)
    outfile = "/tmp/#{File.basename(infile)}.html"
    revision = `git rev-parse HEAD`.strip
    markdown = `which markdown`.strip

    unless $?.success?
      puts "Make sure you have 'markdown' in your path, usage: brew install markdown"
      exit 1
    end

    unless File.exists?(infile)
      puts "Cannot find FILE=#{ENV['FILE'].inspect}, usage: rake soundcloud:doc:preview FILE=doc/hello.md"
      exit 2
    end

    File.open(outfile, "w") do |out|
      body = `#{markdown} #{infile}`
      template = <<-END
        <html>
          <meta http-equiv="content-type" content="text/html;charset=UTF-8" />
          <meta http-equiv="X-UA-Compatible" content="chrome=1">
          <head>
            <link href="https://assets0.github.com/stylesheets/bundle_common.css?#{revision}" media="screen" rel="stylesheet" type="text/css" />
            <link href="https://assets3.github.com/stylesheets/bundle_github.css?#{revision}" media="screen" rel="stylesheet" type="text/css" />
          </head>
          <body>
            <div id="readme" class="blob">
              <div class="wikistyle">
                #{body}
              </div>
            </div>
          </body>
        </html>
      END
      out.write(template)
    end

    puts "Launching: open #{outfile}"
    system("open #{outfile}")
  end

  desc "spellchecks the file (FILE=doc/file.md)"
  task :spellcheck do
    aspell = `which aspell`.strip
    infile = File.expand_path(ENV['FILE'].to_s)

    unless $?.success?
      puts "Make sure you have 'apsell' in your path, usage: brew install aspell --lang=en"
      exit 1
    end

    unless File.exists?(infile)
      puts "Cannot find FILE=#{ENV['FILE'].inspect}, usage: rake doc:spellcheck FILE=doc/hello.md"
      exit 2
    end
    sh aspell, "--mode", "html", "--dont-backup", "check", infile
  end

end


