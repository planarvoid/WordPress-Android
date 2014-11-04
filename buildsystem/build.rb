require_relative 'build/adb'
require_relative 'build/configuration'
require_relative 'build/git'
require_relative 'build/mvn'
require_relative 'build/rake_helper'
require_relative 'build/relase_strategy'
require_relative 'build/device'
require_relative 'build/github'
require_relative 'build/repack'
require 'rexml/document'
require 'version'

module Build
  extend self

  def version
    @version ||= Time.new.strftime("%y.%m.%d")
  end

  def version_code=(code)
    @version_code = code
  end

  def version_code
    @version_code ||= manifest.root.attribute('versionCode').to_s
  end

  def package_name
    @package_name = manifest.root.attribute('package').to_s
  end

  def ci?
    !!ENV['JENKINS_URL']
  end

  def version_name
    if env == 'release'
      [version, build_number].compact.join("-")
    else
      [version, build_number, env].compact.join("-")
    end
  end

  def apk_file_name
    suffix = [version, env].compact.join('-')
    "soundcloud-android-#{suffix}.apk"
  end

  def apk_path
    "app/target/#{apk_file_name}"
  end

  def artifact_path
    "app/target/soundcloud-android-#{version_name}.apk"
  end

  def build_number
    ENV['BUILD_NUMBER'] || 'local'
  end

  def run_command(command)
    puts "Executing: #{command}"
    system command
    puts "\n"

    raise "An error has occurred while executing command #{command}" unless $?.success?
  end

  def run_command_silent(command)
    res = `#{command}`

    res
  end

  private
  def manifest
    @manifest = REXML::Document.new(File.read(Rake.original_dir + '/app/AndroidManifest.xml'))
  end

  def pom
    @pom = REXML::Document.new(File.read(Rake.original_dir + '/pom.xml'))
  end

  def pom_version
    pom.root.elements["version"].text
  end

  def env
    Build::Configuration.build_env
  end
end
