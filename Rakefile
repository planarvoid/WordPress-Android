require_relative 'buildsystem/build'

Build::RakeHelper::tasks

#legacy rake tasks
namespace :ci do
  task :build_app do
    Rake::Task['build'].invoke
    Rake::Task['pmd'].invoke
    Rake::Task['findbugs'].invoke
    Rake::Task['lint'].invoke
  end

  task :test_app do
    Rake::Task['test:unit'].invoke
  end

  task :test_acceptance do
    Rake::Task['test:acceptance'].invoke
  end
end
