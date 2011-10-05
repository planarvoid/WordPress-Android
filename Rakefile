require 'rexml/document'
require 'net/https'
require 'uri'
require 'pp'
require 'csv'
require 'yaml'

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
      sh "git tag -a #{versionName} -m #{versionName} && git push --tags"
    end
  end
end

namespace :beta do
  BUCKET = "soundcloud-android-beta"
  DEST="s3://#{BUCKET}/#{package}-#{versionCode}.apk"
  CURRENT="s3://#{BUCKET}/#{package}-current.apk"
  APK = "bin/soundcloud-release.apk"
  REG_IDS  = 'reg_ids.yaml'
  ACRA_CSV = '1.3.4-BETA3.csv'
  file ACRA_CSV
  file REG_IDS

  desc "build beta"
  task :build do
    sh "ant clean release -Dkey.alias=beta-key"
  end

  desc "install beta on device"
  task :install => :build do
    sh "adb -d install -r #{APK}"
  end

  task :verify do
    raise "Missing file: #{APK}" unless File.exists?(APK)

    output = `jarsigner -verify -certs -verbose #{APK}`
    raise unless $?.success?
    if output !~ /CN=SoundCloud Android Beta/
      raise "Wrong signature"
    end
  end

  task :push => :upload

  desc "upload beta to s3"
  task :upload => :verify do
    metadata = {
      'android-versionname' => versionName,
      'android-versioncode' => versionCode,
      'git-sha1'            => gitsha1
    }
    sh "s3cmd -P put #{APK} " +
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
      sh "git tag -a #{versionName} -m #{versionName} && git push --tags"
    else
      raise "#{versionName}: not a beta version"
    end
  end

  task :parse => ACRA_CSV do
    reg_ids = []
    CSV.foreach(ACRA_CSV, :col_sep=>',') do |row|
      custom =  row[13]
      if custom =~ /\Amessage = registration_id=(.+)\Z/
        reg_ids << $1
      end
    end
    File.open(REG_IDS, 'w') do |f|
      f << reg_ids.to_yaml
    end
  end

  desc "announce new beta via C2DM"
  task :notify => REG_IDS do
    YAML.load_file(REG_IDS).each do |reg_id|
      if resp = post(reg_id, 'beta-version' => [versionCode, versionName].join(':'))
        puts resp
      end
    end
  end
end

namespace :c2dm do
  CL_URL   = URI.parse("https://www.google.com/accounts/ClientLogin")
  C2DM_URL = URI.parse("https://android.apis.google.com/c2dm/send")

  def login(opts={})
    params = {
        'accountType' => 'HOSTED_OR_GOOGLE',
        'Email'       => ENV['EMAIL'] || 'jan@soundcloud.com',
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


  desc "get a client login token"
  task :login do
    if tokens = login
      puts "AuthToken:"+ tokens['Auth']
    end
  end

  def post(reg_id, data={}, collapse_key='key', opts={})
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

  desc "post a message"
  task :post do
    puts post(ENV['REG_ID'], 'beta-version' => [versionCode, versionName].join(':'))
  end
end

namespace :lol do
  task :fetch do
    url = "http://lol.iriscouch.com/lolcatlizr/_design/lolcatlizr/_list/android/android?key=%22%22"
    sh "curl #{url} > res/values/strings_lolcatlizr.xml"
  end
end
