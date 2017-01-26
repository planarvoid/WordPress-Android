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
    query_urn: soundcloud:search-autocomplete:(\w|-)+
    client_id: '3152'
    user: soundcloud:users:18173653
    click_name: search
    query_position: '1'
    page_name: search:main
    connection_type: wifi
    app_version: '[0-9]+'
    click_name: search_formulation_end
    click_attributes:
          q: 'clown'
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    query_urn: soundcloud:search-autocomplete:(\w|-)+
    client_id: '3152'
    user: soundcloud:users:18173653
    click_name: search
    page_name: search:everything
    connection_type: wifi
    app_version: '[0-9]+'
    click_name: search
    click_attributes:
          q: 'clown'
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    query_urn: soundcloud:search-autocomplete:(\w|-)+
    client_id: '3152'
    page_name: search:everything
    user: soundcloud:users:18173653
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
