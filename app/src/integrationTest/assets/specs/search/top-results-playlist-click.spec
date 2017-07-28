--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
      q: query
    click_name: search_formulation_end
    page_name: search:everything
    user: soundcloud:users:[0-9]+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
      q: query
      source: search:playlists
    click_name: item_navigation
    click_object: soundcloud:playlists:297899915
    page_name: search:everything
    user: soundcloud:users:[0-9]+
    query_position: 13
    query_urn: soundcloud:search:db00142ff19f435098f9a5972879c165
  version: '1'
