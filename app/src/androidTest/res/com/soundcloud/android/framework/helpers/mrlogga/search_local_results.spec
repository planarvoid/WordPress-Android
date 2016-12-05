--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: search_formulation_init
    page_name: search:main
    user: soundcloud:users:18173653
  version: '1'
  optional: false
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    page_name: search:main
    connection_type: wifi
    user: soundcloud:users:18173653
  version: '1'
  optional: false
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
      q: skrillex
      source: search-autocomplete
    click_name: item_navigation
    click_object: soundcloud:users:(\w|-)+
    page_name: search:suggestions
    user: soundcloud:users:18173653
    query_position: 1
  version: '1'
  optional: false
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    page_urn: soundcloud:users:(\w|-)+
    page_name: users:main
    user: soundcloud:users:18173653
  version: '1'
  optional: false
whitelisted_events:
- click
- pageview
