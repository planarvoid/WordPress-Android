class HotfixStrategy
  TRUNK_BRANCH    = :development
  RELEASE_BRANCH  = :master

  attr_reader :git, :mvn

  def initialize(git, mvn)
    @git = git
    @mvn = mvn
  end

  def start
    git.check
    git.checkout(RELEASE_BRANCH)
  end

  def finish
    check_git
    unless git.branch_name.to_s == RELEASE_BRANCH
      raise "Cannot finish hotfix from a non-hotfix branch"
    end

    bump_version
    git.push

    git.checkout(TRUNK_BRANCH)
    git.merge(RELEASE_BRANCH)
    if @current_version >= Build.version
      bump_version
    end

    git.push
  end

  private
  def bump_version
    @current_version = Build.version
    Build.version.bump!(:revision)

    mvn.update_version.execute
    mvn.update_manifest.execute

    message = "Version: #{@current_version} bumped to: #{Build.version}"
    git.commit_a(message)

    puts message
  end

end
