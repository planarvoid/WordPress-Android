--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: playlists:main
    page_urn: soundcloud:playlists:[0-9]+
    user: soundcloud:users:151499536
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
whitelisted_events:
- pageview
