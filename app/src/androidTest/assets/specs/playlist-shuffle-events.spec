--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- pageview
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: '[0-9a-z-]+'
    ts: '[0-9]+'
    client_id: '3152'
    page_name: playlists:main
    page_urn: soundcloud:playlists:[0-9]+
    user: soundcloud:users:50749473
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: '[0-9a-z-]+'
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    page_urn: soundcloud:playlists:[0-9]+
    click_name: shuffle:on
    click_category: playback
    page_name: playlists:main
    user: soundcloud:users:50749473
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: '[0-9a-z-]+'
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: player::max
    click_attributes:
      trigger: auto
    user: soundcloud:users:50749473
  version: '1'
  optional: true
