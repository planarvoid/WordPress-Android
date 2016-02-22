--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
- impression
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_resubscribe
    anonymous_id: e701578a826ac3a39b67fa31e5d97a91
    impression_object: soundcloud:tcode:4002
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    page_name: collection:offline_offboarding
    user: soundcloud:users:190276054
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: e701578a826ac3a39b67fa31e5d97a91
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: clickthrough::consumer_sub_resubscribe
    click_object: soundcloud:tcode:4002
    page_name: collection:offline_offboarding
    click_category: consumer_subs
    user: soundcloud:users:190276054
  version: '1'
