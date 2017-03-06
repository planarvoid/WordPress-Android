--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
- impression
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_resubscribe
    anonymous_id: '[0-9a-z-]+'
    impression_object: soundcloud:tcode:4002
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    page_name: collection:offline_offboarding
    user: soundcloud:users:190502894
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: '[0-9a-z-]+'
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: clickthrough::consumer_sub_resubscribe
    click_object: soundcloud:tcode:4002
    page_name: collection:offline_offboarding
    click_category: consumer_subs
    user: soundcloud:users:190502894
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_ad
    anonymous_id: (\w|-)+
    impression_object: soundcloud:tcode:3002
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    page_name: consumer-premium:main
    user: soundcloud:users:190502894
  version: '1'
