source 'https://rubygems.org'

# Specify your gem's dependencies in mailnipulator.gemspec
gemspec

gem 'nylas', path: '../mguymon-nylas-ruby', require: 'nylas'

@@check ||= at_exit do
  require 'lock_jar/bundler'
  LockJar::Bundler.lock!(::Bundler)
end
