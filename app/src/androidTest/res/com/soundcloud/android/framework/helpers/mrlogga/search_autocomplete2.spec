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
    user: soundcloud:users:285710974
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
    user: soundcloud:users:285710974
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
    user: soundcloud:users:285710974
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
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    query_urn: soundcloud:search-autocomplete:(\w|-)+
    client_id: '3152'
    page_name: search:everything
    user: soundcloud:users:285710974
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
    user: soundcloud:users:285710974
    connection_type: wifi
    click_name: search_formulation_init
    page_name: search:everything
    click_attributes:
          q: 'clowns'
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    user: soundcloud:users:285710974
    click_name: search
    click_object: soundcloud:users:(\w|-)+
    query_position: '[0-9]+'
    page_name: search:suggestions
    connection_type: wifi
    app_version: '[0-9]+'
    click_name: item_navigation
    click_attributes:
          q: 'a'
          source: 'search-autocomplete'
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: '3152'
    connection_type: wifi
    page_urn: soundcloud:users:(\w|-)+
    page_name: users:main
    user: soundcloud:users:285710974
  version: '1'
