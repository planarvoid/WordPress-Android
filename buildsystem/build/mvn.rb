module Build
  class Mvn
    attr_reader :command

    def initialize(command)
      @command = command
    end

    def self.build_command(task)
      config = Configuration.mvn.phase(task.to_s)
      mvn = self.new "mvn clean #{config.phase}"

      mvn.projects(config.project)

      if !config.tests
        mvn.skip_tests
      end

      mvn.set_version_name(Build.version_name)

      if Build.ci?
        mvn.use_local_repo
        mvn.set_version_code(Build.version_code)
      end

      if ENV['PROGUARD']
        mvn.with_proguard
      end

      mvn.with_profiles(config.profiles)

      mvn
    end

    def self.task(name)
      build_command(name)
    end

    def self.pmd
      mvn = self.new "mvn pmd:pmd"
      mvn.projects('app')
      mvn.with_profiles('static-analysis')

      mvn
    end

    def self.lint
      mvn = self.new "mvn android:lint"
      mvn.with_lint
      mvn.projects('app')
      mvn.with_profiles('static-analysis')

      mvn
    end

    def self.findbugs
      mvn = self.new "mvn findbugs:findbugs"
      mvn.projects('app')
      mvn.with_profiles('static-analysis')

      mvn
    end

    def self.update_version
      mvn = self.new("mvn versions:set -DnewVersion=#{Build.version} -DgenerateBackupPoms=false -DupdateMatchingVersions=false")
      mvn.with_profiles("update-android-manifest")

      mvn
    end

    def self.update_manifest
      self.new('mvn android:manifest-update')
    end

    def self.pre_proguard(options = [])
      mvn = self.new("mvn clean package")
      mvn.projects('app')
      mvn.skip_tests
      mvn.with_proguard(['-dontobfuscate', '-dontoptimize'] + options)
      mvn.with_profiles('ci,sign')
      mvn
    end

    def self.install_file(file, group_id, artifact_id, version, packaging = 'jar')
      cmd = "mvn install:install-file"
      cmd << " -Dfile=#{file}"
      cmd << " -DgroupId=#{group_id}"
      cmd << " -DartifactId=#{artifact_id}"
      cmd << " -Dversion=#{version}"
      cmd << " -Dpackaging=#{packaging}"
      self.new cmd
    end

    def self.deploy_file(file, group_id, artifact_id, version, packaging = 'jar')
      cmd = "mvn deploy:deploy-file"
      cmd << " -Dfile=#{file}"
      cmd << " -DgroupId=#{group_id}"
      cmd << " -DartifactId=#{artifact_id}"
      cmd << " -Dversion=#{version}"
      cmd << " -Dpackaging=#{packaging}"
      cmd << " -DrepositoryId=soundcloud.internal.releases"
      cmd << " -Durl=http://maven.int.s-cloud.net/content/repositories/releases"
      self.new cmd
    end

    def projects(*array_of_projects)
      @command << " --projects #{array_of_projects.join(',')}"
      self
    end

    def with_profiles(*array_of_profiles)
      unless array_of_profiles.empty?
        @command << " -P #{array_of_profiles.join(',')}"
      end

      self
    end

    def skip_tests
      @command << " -DskipTests"
      self
    end

    def set_version_code(version_code)
      @command << " -Dandroid.manifest.versionCode=#{version_code}"
      self
    end

    def set_version_name(version_name)
      @command << " -Dandroid.manifest.versionName=#{version_name}"
      self
    end

    def use_local_repo
      @command << " -Dmaven.repo.local=.repository"
      self
    end

    def set_debuggable
      @command << " -Dandroid.manifest.debuggable=true"
      self
    end

    def with_lint
      @command << " -Dandroid.lint.skip=false"
      self
    end

    def with_proguard(options = [])
      @command << " -Dandroid.proguard.skip=false"
      if options.any?
        @command << " -Dandroid.proguard.options=\"#{options.join(' ')}\""
      end
      self
    end

    def skip_proguard
      @command << " -Dandroid.proguard.skip=true"
      self
    end

    def execute
      Build.run_command command
    end
  end
end
