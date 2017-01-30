--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- pageview
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: '3152'
    user: soundcloud:users:18173653
    connection_type: wifi
    click_name: search_formulation_init
    page_name: search:main
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: search:main
    user: soundcloud:users:18173653
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: '3152'
    connection_type: wifi
    click_attributes:
          q: 'clownstep'
    click_name: search_formulation_end
    page_name: search:main
    query_position: 0
    user: soundcloud:users:18173653
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    query_urn: soundcloud:search:(\w|-)+
    client_id: '3152'
    page_name: search:everything
    user: soundcloud:users:18173653
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    query_urn: soundcloud:search:(\w|-)+
    client_id: '3152'
    page_name: search:albums
    user: soundcloud:users:18173653
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    click_attributes:
          q: 'clownstep'
    click_name: item_navigation
    click_object: soundcloud:playlists:(\w|-)+
    query_urn: soundcloud:search:(\w|-)+
    page_name: search:albums
    user: soundcloud:users:18173653
    query_position: '0'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: playlists:main
    user: soundcloud:users:18173653
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
