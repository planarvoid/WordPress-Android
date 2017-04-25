--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi|4g
    click_name: player::max
    click_attributes:
      trigger: auto
    user: soundcloud:users:[0-9]+
  version: 0
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi|4g
    click_name: player::min
    click_attributes:
      trigger: auto
    user: soundcloud:users:[0-9]+
  version: 0
