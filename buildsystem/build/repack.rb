require 'fileutils'

module Build
  class Repacker

    REPACK_JAR = "app/target/repack.jar"

    def self.repack(base_package, group_id, artifact_id, options = {})
      puts "Running ProGuard'ed build..."
      puts "Options: #{options.inspect}"
      Mvn.pre_proguard(options[:pg_rules] || []).execute unless options[:dryrun]

      puts "Repacking #{artifact_id}..."

      target_dir, repack_dir = prepare_directories(base_package, artifact_id)

      # extract surviving classes under the base package from proguarded JAR
      copy_survivors(base_package, target_dir, options)

      # roll surviving classes into a new JAR
      `jar cvfM #{REPACK_JAR} -C #{repack_dir} .`

      if (options[:aar])
        puts "Rebundling AAR..."
        dir_string = "app/target/unpack/apklibs/#{group_id}_#{artifact_id}_aar_*"
        puts "Looking for unpack location string #{dir_string}"
        unpack_dir = Dir.glob(dir_string).first
        FileUtils.cp(REPACK_JAR, "#{unpack_dir}/classes.jar")
        `cd #{unpack_dir} && zip -r ../../../repack.aar *`
      end

      puts "Installing to local Maven cache..."
      dependency = mvn_install(group_id, artifact_id, options)

      puts "Deploying to Nexus..."
      begin
        mvn_deploy(dependency, options) unless options[:dryrun]
      rescue
        puts "!DEPLOY FAILED! Make sure you have the right credentials in place and the artifact doesn't exist already on Nexus."
      end
      puts "Done. Update your POM with this:"
      puts <<-XML
      <dependency>
        <groupId>#{dependency[:gid]}</groupId>
        <artifactId>#{dependency[:aid]}</artifactId>
        <version>#{dependency[:version]}</version>
        <type>#{packaging(options)}</type>
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

    def self.copy_survivors(base_package, target_dir, options = {})
      proguard_jar = Build.apk_path.gsub("-debug.apk", "_obfuscated.jar")
      proguard_unzip_dir = "app/target/proguard_classes"
      `unzip #{proguard_jar} -d #{proguard_unzip_dir}`

      # This strips out R files, as they will clash with the ones generated later.
      # I couldn't find a way to filter these files during the copy step.
      # And my shell globbing wizardry sucks too much to make this a one liner too.
      base_dir = "#{proguard_unzip_dir}/#{base_package}"
      r_files = Dir.glob("#{base_dir}/**/R.class") + Dir.glob("#{base_dir}/**/R$*.class")
      FileUtils.rm(r_files)

      source_files = Dir.glob("#{proguard_unzip_dir}/#{base_package}/*")
      source_files.reject! {|d| d =~ options[:path_filter] } if options[:path_filter]
      FileUtils.cp_r(source_files, target_dir)
    end

    def self.mvn_install(group_id, artifact_id, options)
      repack_group_id = group_id
      repack_artifact_id = "#{artifact_id}-repack"
      repack_version = "#{Build.version}"
      unless options[:dryrun]
        Mvn.install_file(
          repack_file(options), 
          repack_group_id, 
          repack_artifact_id, 
          repack_version, 
          packaging(options)
        ).execute
      end
      {:gid => repack_group_id, :aid => repack_artifact_id, :version => repack_version}
    end

    def self.mvn_deploy(dependency, options)
      Mvn.deploy_file(
        repack_file(options),
        dependency[:gid], 
        dependency[:aid], 
        dependency[:version], 
        packaging(options)
      ).execute
    end

    def self.repack_file(options)
      file = options[:aar] ? REPACK_JAR.gsub(".jar", ".aar") : REPACK_JAR
    end

    def self.packaging(options)
      packaging = options[:aar] ? "aar" : "jar"
    end
  end
end