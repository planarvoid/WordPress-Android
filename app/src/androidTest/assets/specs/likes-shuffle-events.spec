--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: '[0-9a-z-]+'
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: shuffle:on
    click_category: playback
    page_name: collection:likes
    user: soundcloud:users:[0-9]+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
      trigger: auto
    click_name: click_player::max
    user: soundcloud:users:[0-9]+
  version: '0'
