require 'lock_jar'
LockJar.load
require 'nylas'
require 'mailnipulator/stream_handlers/vertx'

begin
  puts 'Connecting to Nylas Sync Engine'
  nylas = Nylas::API.new(nil, nil, nil, 'http://localhost:5555/')

  accounts = nylas.accounts
  accounts.each { |a| puts [a.account_id, a.sync_state] }

  # Get the id of the first account -- this is the access token we're
  # going to use.
  account_id = nylas.accounts.first.id

  nylas = Nylas::API.new(nil, nil, account_id, 'http://localhost:5555/')
  nylas.stream_handler = Mailnipulator::StreamHandlers::Vertx.new

  cursor = nylas.latest_cursor

  nylas.delta_stream(cursor) do |event, object|
    puts event.inspect
  end
rescue => ex
  puts "Error! #{ex.inspect}"
  puts ex.backtrace
end
