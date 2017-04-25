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
      overflow_menu: true
    click_name: like::(add|remove)
    click_object: soundcloud:tracks:[0-9]+
    click_category: engagement
    page_name: stream:main
    user: soundcloud:users:287403532
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: like::(add|remove)
    click_object: soundcloud:tracks:[0-9]+
    click_category: engagement
    page_name: stream:main
    user: soundcloud:users:287403532
  version: '0'
