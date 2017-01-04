module Build
  class Git

    def check
      raise "Uncommitted changes in working tree" if uncommited_changes?
    end

    def commit_a(message)
      command = "git commit -a -m '#{message}'"

      Build.run_command(command)
    end

    def tag
      tag_name = Build.version_name
      command  = "git tag -a #{tag_name} -m 'Version: #{tag_name}' && git push --tags"

      puts "Applying tag: #{tag_name}"

      Build.run_command(command)
    end

    def merge_to(branch = :release)
      start_branch = branch_name

      checkout(branch)
      merge

      checkout(start_branch)
    end

    def last_tag(type = :release)
      glob = get_glob_pattern(type)
      Build.run_command("git describe --abbrev=0 --match '#{glob}'")
    end

    def merge(branch = :development)
      Build.run_command("git merge #{branch} --no-commit")
    end

    def checkout(sha)
      Build.run_command("git checkout #{sha.to_s}")
    end

    def push
      Build.run_command("git push origin #{branch_name}:#{branch_name}")
    end

    def branch_name
      Build.run_command_silent("git rev-parse --abbrev-ref HEAD").strip.to_sym
    end

    def uncommited_changes?
      Build.run_command_silent("git diff --exit-code --quiet")
      $?.exitstatus == 1
    end

    def create_branch(branch_name)
      Build.run_command("git checkout -b #{branch_name}")
    end

    private
    def get_glob_pattern(type)
      if type == :release
        return "[0-9].[0-9].[0-9]"
      else
        return "*#{type}*"
      end
    end
  end
end
