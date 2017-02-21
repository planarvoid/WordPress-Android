--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    click_name: like::(add|remove)
    page_name: new_for_you:main
    query_urn: soundcloud:newforyou:(\w|-)+
    query_position: 1

    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: '[0-9]+'
    connection_type: (\w|-)+
    click_attributes:
      overflow_menu: true
    click_object: soundcloud:tracks:(\w|-)+
    click_category: engagement
    user: soundcloud:users:(\w|-)+
  version: '1'
  optional: false
