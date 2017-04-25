--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    app_version: (\w|-)+
    connection_type: (\w|-)+
    page_name: users:(info|main|followings|followers)
    click_category: engagement
    click_name: follow::(add|remove)
    click_object: soundcloud:users:[0-9]+
  version: '1'

