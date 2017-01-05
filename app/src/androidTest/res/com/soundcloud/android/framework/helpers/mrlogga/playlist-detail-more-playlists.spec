--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- pageview
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    page_name: playlists:main
    connection_type: wifi
    client_id: '3152'
    user: soundcloud:users:50749473
  version: '1'
  optional: false
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
    client_id: '3152'
    click_name: item_navigation
    click_object: soundcloud:playlists:168229690
    page_name: playlists:main
    user: soundcloud:users:50749473
  version: '1'
  optional: false
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    page_name: playlists:main
    connection_type: wifi
    client_id: '3152'
    user: soundcloud:users:50749473
  version: '1'
  optional: false
