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

    private
    attr_reader :token

    def create_milestone(title)
      response = authorized_connection.post("/repos/#{OWNER}/#{REPO}/milestones", :body => { :title => title }.to_json)
      JSON.parse response.body
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