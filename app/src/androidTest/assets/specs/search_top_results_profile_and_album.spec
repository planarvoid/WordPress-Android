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
    user: soundcloud:users:[0-9]+
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
    user: soundcloud:users:[0-9]+
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
      q: coldplay
    click_name: search_formulation_end
    page_name: search:everything
    user: soundcloud:users:[0-9]+
    query_position: 0
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
    query_urn: soundcloud:search:(\w|-)+
    page_name: search:everything
    user: soundcloud:users:[0-9]+
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
      q: coldplay
      source: search:people
    click_name: item_navigation
    query_urn: soundcloud:search:(\w|-)+
    click_object: soundcloud:users:[0-9]+
    page_name: search:everything
    user: soundcloud:users:[0-9]+
    query_position: '[0-9]+'
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
    page_urn: soundcloud:users:[0-9]+
    page_name: users:main
    user: soundcloud:users:[0-9]+
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
    query_urn: soundcloud:search:(\w|-)+
    page_name: search:everything
    user: soundcloud:users:[0-9]+
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
      q: coldplay
      source: search:albums
    click_name: item_navigation
    query_urn: soundcloud:search:(\w|-)+
    click_object: soundcloud:playlists:[0-9]+
    page_name: search:everything
    user: soundcloud:users:[0-9]+
    query_position: '[0-9]+'
  version: '1'
  optional: false
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    page_name: playlists:main
    connection_type: wifi
    user: soundcloud:users:[0-9]+
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
    query_urn: soundcloud:search:(\w|-)+
    page_name: search:everything
    user: soundcloud:users:[0-9]+
  version: '1'
  optional: false
whitelisted_events:
- click
- pageview
