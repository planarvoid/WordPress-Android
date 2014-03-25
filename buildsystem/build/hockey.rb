require 'rest-client'

module Build
  class Hockey
    # http://support.hockeyapp.net/kb/api/api-apps#upload-app

    STATUS_RESTRICTED    = 1
    STATUS_AVAILABLE     = 2
    NOTIFY_DO_NOT_NOTIFY = 0
    NOTIFY_ALL_TESTERS   = 1
    NOTES_TYPE_TEXTILE   = 0
    NOTES_TYPE_MARKDOWN  = 1
    API_URL   = "https://rink.hockeyapp.net/api/2/apps"
    attr_reader :url

    def initialize
      @token      = Configuration.hockey.api_token
      @app_id     = Configuration.hockey.app_id
      @url        = "#{API_URL}/#{@app_id}/app_versions"
    end

    def app_versions
      RestClient.get(url, :'X-HockeyAppToken' => @token){|response, _, _|
        case response.code
          when 200
            JSON.parse(response.body)
          else
            nil
        end
      }
    end

    def last_published_version
      versions = app_versions
      if versions
        versions['app_versions'].map{ |app|
          app['version'].to_i
        }.max
      else
        0
      end
    end

    def notes
      changes = []
      IO.read('CHANGES').split("\n")[1..-1].each do |line|
        break if line[0] && line[0].chr == '$'
        changes << line.gsub(/\s*\*/, ' *') if (line =~ /s*\*/)
      end
      changes.join("\n")
    end

    def upload(path_to_file)
      params = {
        :status     => STATUS_RESTRICTED,
        :notify     => NOTIFY_DO_NOT_NOTIFY,
        :notes_type => NOTES_TYPE_MARKDOWN,
        :notes      => notes,
        :ipa        => File.new(path_to_file, 'rb')
      }

      RestClient.post(@url, params, :'X-HockeyAppToken' => @token )
    end

    def list
      puts "Version:\t Date released:"
      app_versions['app_versions'][0..10].each{|info|
        puts "#{info['shortversion']}\t #{Time.at(info['timestamp'])}"
      }
    end

  end
end
