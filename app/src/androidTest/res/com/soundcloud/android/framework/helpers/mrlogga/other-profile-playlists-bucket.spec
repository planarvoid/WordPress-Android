--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: playlists:main
    user: soundcloud:users:151499536
  version: '1'
whitelisted_events:
- pageview
