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
    page_name: tracks:main
    click_name: item_navigation
    user: soundcloud:users:[0-9]+
    click_object: soundcloud:users:[0-9]+
  version: 1
- !ruby/object:MrLoggerLogger::Event
  name: click
  optional: true
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
      trigger: (auto|manual)
    click_name: player::(max|min)
    user: soundcloud:users:[0-9]+
  version: '0'