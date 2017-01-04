module Build
  module Configuration
    class Mvn
      attr_reader :profiles, :tests

      def initialize(phases)
        @phases = phases
      end

      def phase(name)
        config = @phases[name]
        Phase.new(config['phase'], config['project'],config['tests'],config['proguard'], config['profiles'])
      end

      class Phase

        attr_reader :phase, :project, :tests, :proguard, :profiles
        def initialize(phase, project, tests, proguard, profiles)
          @phase    = phase
          @project  = project
          @tests    = tests
          @proguard = proguard
          @profiles = profiles
        end
      end
    end

  end
end
