--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- pageview
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: users:info
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: users:main
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: users:followers
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: users:followings
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
