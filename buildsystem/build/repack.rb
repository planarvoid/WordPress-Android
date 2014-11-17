require 'fileutils'

module Build
  class Repacker

    REPACK_JAR = "app/target/repack.jar"

    def self.repack(base_package, group_id, artifact_id, options = {})
      puts "Running ProGuard'ed build..."
      puts "Options: #{options.inspect}"
      Mvn.pre_proguard(options[:pg_rules] || []).execute

      puts "Repacking #{artifact_id}..."

      target_dir, repack_dir = prepare_directories(base_package, artifact_id)

      # extract surviving classes under the base package from proguarded JAR
      copy_survivors(base_package, target_dir)

      # roll surviving classes into a new JAR
      `jar cvfM #{REPACK_JAR} -C #{repack_dir} .`

      puts "Installing to local Maven cache..."
      dependency = mvn_install(group_id, artifact_id)

      puts "Deploying to Nexus..."
      begin
        mvn_deploy(dependency)
      rescue
        puts "!DEPLOY FAILED! Make sure you have the right credentials in place and the artifact doesn't exist already on Nexus."
      end
      puts "Done. Update your POM with this:"
      puts <<-XML
      <dependency>
        <groupId>#{dependency[:gid]}</groupId>
        <artifactId>#{dependency[:aid]}</artifactId>
        <version>#{dependency[:version]}</version>
      </dependency>
      XML
    end

    private

    def self.prepare_directories(base_package, artifact_id)
      repack_dir = "app/target/#{artifact_id}-repack"
      target_dir = "#{repack_dir}/#{base_package}/"
      FileUtils.mkdir_p target_dir

      [target_dir, repack_dir]
    end

    def self.copy_survivors(base_package, target_dir)
      proguard_jar = Build.apk_path.gsub("-debug.apk", "_obfuscated.jar")
      proguard_unzip_dir = "app/target/proguard_classes"
      `unzip #{proguard_jar} -d #{proguard_unzip_dir}`
      FileUtils.cp_r("#{proguard_unzip_dir}/#{base_package}/.", target_dir)
    end

    def self.mvn_install(group_id, artifact_id)
      repack_group_id = group_id
      repack_artifact_id = "#{artifact_id}-repack"
      repack_version = "#{Build.version}"
      Mvn.install_file(REPACK_JAR, repack_group_id, repack_artifact_id, repack_version).execute
      {:gid => repack_group_id, :aid => repack_artifact_id, :version => repack_version}
    end

    def self.mvn_deploy(dependency)
      Mvn.deploy_file(REPACK_JAR, dependency[:gid], dependency[:aid], dependency[:version]).execute
    end
  end
end