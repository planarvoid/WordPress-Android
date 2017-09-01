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
    client_id: 3152
    connection_type: wifi
    query_urn: soundcloud:queries:top-level-discovery-cards-list
    page_name: discovery:main
    user: soundcloud:users:50670381
  version: '1'

