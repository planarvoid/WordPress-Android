--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
- pageview
expected_events:
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
    user: soundcloud:users:18173653
    query_position: 1
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    source: ambient-new-and-hot
    client_id: 3152
    connection_type: wifi
    page_urn: soundcloud:system-playlists:the-upload:soundcloud:users:183
    page_name: systemplaylists:main
    user: soundcloud:users:18173653
    query_urn: soundcloud:[\w:]+
  version: '1'
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
    user: soundcloud:users:18173653
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    source: go-beyond
    client_id: 3152
    connection_type: wifi
    click_name: item_navigation
    click_attributes:
      source_urn: soundcloud:selections:go-beyond
      source_query_urn: soundcloud:queries:17b5d160422643dfgt423tg3eb4
      source_position: 0
      source: go-beyond
    query_urn: soundcloud:queries:top-level-discovery-cards-list
    click_object: soundcloud:system-playlists:related-tracks:soundcloud:tracks:46361966
    page_name: discovery:main
    user: soundcloud:users:18173653
    query_position: 2
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    source: ambient-new-and-hot
    client_id: 3152
    connection_type: wifi
    page_urn: soundcloud:system-playlists:the-upload:soundcloud:users:183
    page_name: systemplaylists:main
    user: soundcloud:users:18173653
    query_urn: soundcloud:[\w:]+
  version: '1'
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
    user: soundcloud:users:18173653
  version: '1'
