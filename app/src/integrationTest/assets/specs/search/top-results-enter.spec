--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- pageview
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: '3152'
    page_name: search:everything
    connection_type: wifi
    query_urn: soundcloud:search:(\w|-)+
    user: soundcloud:users:[0-9]+
  version: '1'
