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
    page_name: discovery:main
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
    query_position: '0'
    client_id: '3152'
    user: soundcloud:users:285710974
    page_name: search:main
    connection_type: wifi
    app_version: '[0-9]+'
    click_name: search_formulation_update
    click_attributes:
          q: '$user_query$'
          term: '$selected_search_term$'
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
    click_name: search_formulation_exit
    click_attributes:
          q: '$selected_search_term$'
    page_name: search:main
  version: '1'
