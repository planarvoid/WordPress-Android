require 'fileutils'

module Build
  class Repacker

    def self.repack(base_package, group_id, artifact_id, pg_options = [])
      # Running ProGuard'ed build...
      Mvn.pre_proguard(pg_options).execute

      # will yield something like com/google/com
      dimension_str = "#{group_id}:#{artifact_id}"
      puts "Repacking #{dimension_str}..."
      pkg_path = base_package.gsub("\.", "/")
      repack_dir = "app/target/#{artifact_id}-repack"
      target_dir = "#{repack_dir}/#{pkg_path}/"
      FileUtils.mkdir_p target_dir

      proguard_jar = Build.apk_path.gsub("-debug.apk", "_obfuscated.jar")
      proguard_unzip_dir = "app/target/proguard_classes"
      `unzip #{proguard_jar} -d #{proguard_unzip_dir}`
      FileUtils.cp_r("#{proguard_unzip_dir}/#{pkg_path}/.", target_dir)
      repack_jar = "app/target/repack.jar"
      `jar cvfM #{repack_jar} -C #{repack_dir} .`

      puts "Installing to local Maven cache..."
      repack_group_id = group_id
      repack_artifact_id = "#{artifact_id}-repack"
      repack_version = "#{Build.version}"
      Mvn.install_file(repack_jar, repack_group_id, repack_artifact_id, repack_version).execute
      puts "Deploying to Nexus..."
      begin
        Mvn.deploy_file(repack_jar, repack_group_id, repack_artifact_id, repack_version).execute
      rescue
        puts "!DEPLOY FAILED! Make sure you have the right credentials in place and the artifact doesn't exist already on Nexus."
      end
      puts "Done. Update your POM with this:"
      puts <<-XML
      <dependency>
        <groupId>#{repack_group_id}</groupId>
        <artifactId>#{repack_artifact_id}</artifactId>
        <version>#{repack_version}</version>
      </dependency>
      XML
    end
  end
end