require 'oauth2'

module Build
  class Github
    OWNER = 'soundcloud'
    REPO  = 'soundcloud-android'
    def initialize(token)
      @token = token
    end

    def create_regression_tests_checklist(milestone_title, issues)
      milestone_number = create_milestone(milestone_title)['number']
       issues.each{|issue|
         puts "Creating checklist: #{issue.title}"

         authorized_connection.post("/repos/#{OWNER}/#{REPO}/issues", :body=> {
           :title      => issue.title,
           :body       => issue.body,
           :milestone  => milestone_number
         }.to_json)
       }
    end

    def create_release(tag_name, name, body)
      authorized_connection.post("/repos/#{OWNER}/#{REPO}/releases", :body => {
        :tag_name => tag_name,
        :name     => name,
        :body     => body
      }.to_json)
    end

    def upload_apk(release_id, apk_file_path)
      file = File.new(apk_file_path)

      upload_url = "/repos/#{OWNER}/#{REPO}/releases/#{release_id}/assets?name=soundcloud-android.zip"
      params = {
        :headers => { 'Content-Type' => 'application/zip', 'Content-Length'=> file.size.to_s },
        :body    => Faraday::UploadIO.new(file.path, 'application/zip')
      }

      authorized_uploader.post(upload_url, params)
    end

    private
    attr_reader :token

    def create_milestone(title)
      response = authorized_connection.post("/repos/#{OWNER}/#{REPO}/milestones", :body => { :title => title }.to_json)
      JSON.parse response.body
    end


    def authorized_uploader
      @uploader ||= OAuth2::AccessToken.new(
        OAuth2::Client.new("", "", :site => "https://uploads.github.com/"),
        token
      )
    end

    def authorized_connection
      @connection ||= OAuth2::AccessToken.new(
            OAuth2::Client.new("", "", :site => "https://api.github.com/"),
            token
      )
    end
    class Issue
      attr_reader :title, :body
      def initialize(title, body = nil)
        raise 'A title should start with "#", please fix ensure that the template is correct' unless title.start_with? '#'
        @title = title.gsub('#', '').strip
        @body  = body.join
      end
    end
  end
end