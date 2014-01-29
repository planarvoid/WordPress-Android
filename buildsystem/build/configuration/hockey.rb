module Build
  module Configuration
    class Hockey
      attr_reader :github_url, :api_token, :app_id

      def initialize(enabled, github_url, api_token, app_id, notify, download)
        @enabled    = enabled
        @github_url = github_url
        @api_token  = api_token
        @app_id     = app_id
        @notify     = notify
        @download   = download
      end

      def enabled?
        !!@enabled
      end

      def notify?
        !!@notify
      end

      def download?
        !!@download
      end

    end
  end
end
