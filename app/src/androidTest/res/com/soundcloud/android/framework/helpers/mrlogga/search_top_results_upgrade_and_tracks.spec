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
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    query_urn: soundcloud:search:(\w|-)+
    page_name: search:tracks
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
    click_name: clickthrough::consumer_sub_ad
    click_object: soundcloud:tcode:[0-9]+
    click_category: consumer_subs
    page_name: search:high_tier
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
    page_name: consumer-premium:main
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
    page_name: search:tracks
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
      source: search:tracks
    click_name: item_navigation
    query_urn: soundcloud:search:(\w|-)+
    click_object: soundcloud:tracks:[0-9]+
    page_name: search:everything
    user: soundcloud:users:[0-9]+
    query_position: '[0-9]+'
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
    click_name: player::max
    click_attributes:
      trigger: auto
    user: soundcloud:users:[0-9]+
  version: '1'
  optional: false
whitelisted_events:
- click
- pageview
