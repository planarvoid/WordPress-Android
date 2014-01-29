class ReleaseStrategy
  TRUNK_BRANCH    = :development
  RELEASE_BRANCH  = :master

  attr_reader :git, :mvn

  def initialize(git, mvn)
    @git = git
    @mvn = mvn
  end

  def start
    git.check

    if git.branch_name != TRUNK_BRANCH
      raise "Cannot start release from #{git.branch_name}, please checkout #{TRUNK_BRANCH} branch first"
    end

    git.merge_to(RELEASE_BRANCH)

    bump_version
    git.push

    git.checkout(RELEASE_BRANCH)
    git.push
  end

  def finish
    git.check
    if git.branch_name != RELEASE_BRANCH
      raise "Cannot finish release from #{git.branch_name}, please checkout #{RELEASE_BRANCH} branch first"
    end

    git.tag
    git.checkout(TRUNK_BRANCH)
    git.merge(RELEASE_BRANCH)
    git.push
  end

  private
  def bump_version
    current = Build.version.to_s
    version_bump = ENV['VERSION_BUMP'] || :minor
    code = Build.version_code.to_i + 1

    Build.version.bump!(version_bump.to_sym)

    mvn.update_version.execute
    mvn.update_manifest.set_version_code(code).execute

    message = "Version: #{current} bumped to: #{Build.version}"
    git.commit_a(message)

    puts message
  end
end
