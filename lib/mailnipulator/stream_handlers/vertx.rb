require 'uri'
require 'nylas/stream_handlers/simple_stream'

module Mailnipulator
  module StreamHandlers
    class Vertx < Nylas::StreamHandlers::SimpleStream

      def stream_activity(url, timeout, &callback)
        parser = ::SimpleStream.new

        parser_callback = proc do |data|
          callback.call(transform_to_ruby(data))
        end

        parser.setCallback(parser_callback)

        uri = URI.parse(url)
        auth = "#{uri.user}:#{uri.password}"

        $vertx.event_bus.publish('nylas.delta.endpoint', {auth: auth, url: url})

        $vertx.event_bus.consumer('nylas.delta.stream') do |message|
          parser.stream(message.body) if message.body && message.body.strip.size > 0
        end
      end
    end
  end
end