--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- foreground
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: foreground
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    referrer: t6001
    page_name: deeplink
    user: soundcloud:users:147986827
  version: '0'
