require 'yaml'
require_relative 'configuration/mvn'

module Build
  module Configuration
    extend self

    def mvn
      Mvn.new(
        mvn_setting(:tasks)
      )
    end

    def build_env
      config_env
    end

    private
    def hockey_setting(key)
      setting(:hockey, key)
    end

    def mvn_setting(key)
      setting(:mvn, key)
    end

    def camelize(string)
      string.split('_').map(&:capitalize).join
    end

    def setting(prefix, key)
      ENV["#{prefix.to_s.upcase}_#{key.to_s.upcase}"] || settings[prefix.to_s][key.to_s]
    end

    def settings
      @settings ||= YAML.load_file('.build.yml')[config_env]
    end

    def config_env
      ENV['BUILD_ENV'] || 'debug'
    end
  end
end
