--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
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
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    source: the-upload
    client_id: 3152
    connection_type: wifi
    click_name: item_navigation
    click_attributes:
      source_urn: soundcloud:selections:the-upload
      source_query_urn: soundcloud:queries:ht3r3rhtr3
      source_position: 0
      source: the-upload
    query_urn: soundcloud:queries:top-level-discovery-cards-list
    click_object: soundcloud:system-playlists:the-upload:soundcloud:users:183
    page_name: discovery:main
    user: soundcloud:users:50670381
    query_position: 1
  version: '1'


