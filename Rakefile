require 'rexml/document'

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

namespace :beta do
  BUCKET = "soundcloud-android-beta"
  DEST="s3://#{BUCKET}/#{package}-#{versionCode}.apk"
  APK = "bin/soundcloud-release.apk"

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
  end
end
