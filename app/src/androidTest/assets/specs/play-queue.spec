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
    click_name: play_queue::max
    user: soundcloud:users:[0-9]+
  version: 0
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    page_name: 'play_queue:main'
    click_name: 'shuffle::on'
    client_id: 3152
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: 0
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    page_name: 'play_queue:main'
    click_name: 'repeat::on'
    click_attributes:
      repeat: one
    client_id: 3152
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: 0
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    page_name: 'play_queue:main'
    click_name: 'repeat::on'
    click_attributes:
      repeat: all
    client_id: 3152
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: 0
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    page_name: 'play_queue:main'
    click_name: 'repeat::off'
    client_id: 3152
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: 0
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: play_queue::min
    user: soundcloud:users:[0-9]+
  version: 0
