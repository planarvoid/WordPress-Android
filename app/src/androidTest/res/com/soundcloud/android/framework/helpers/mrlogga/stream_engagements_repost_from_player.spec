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
    connection_type: wifi
    click_attributes:
          trigger: (auto|manual)
    click_name: player::(max|min)
    user: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
          overflow_menu: true
          source: stream
    page_urn: soundcloud:tracks:[0-9]+
    click_name: repost::(add|remove)
    click_object: soundcloud:tracks:[0-9]+
    click_category: engagement
    page_name: tracks:main
    user: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
          overflow_menu: true
          source: stream
    page_urn: soundcloud:tracks:[0-9]+
    click_name: repost::(add|remove)
    click_object: soundcloud:tracks:[0-9]+
    click_category: engagement
    page_name: tracks:main
    user: soundcloud:users:[0-9]+
  version: '0'
