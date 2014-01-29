require_relative 'build/adb'
require_relative 'build/configuration'
require_relative 'build/git'
require_relative 'build/hockey'
require_relative 'build/mvn'
require_relative 'build/rake_helper'
require_relative 'build/relase_strategy'
require 'rexml/document'
require 'version'

module Build
  extend self

  def version
    @version ||= pom_version.to_version
  end

  def version_code
    @version_code = manifest.root.attribute('versionCode').to_s
  end

  def package_name
    @package_name = manifest.root.attribute('package').to_s
  end

  def ci?
    !!ENV['JENKINS_URL']
  end

  def version_name
    [version, env, build_number].compact.join("-")
  end

  def apk_file_name
    "soundcloud-android-#{version}-#{env}.apk"
  end

  def apk_path
    "app/target/#{apk_file_name}"
  end

  def build_number
    ENV['BUILD_NUMBER']
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
