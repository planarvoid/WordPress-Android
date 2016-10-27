--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- impression
- click
- pageview
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: clickthrough::consumer_sub_ad
    click_object: soundcloud:tcode:1008
    page_name: settings:offline_sync_settings
    click_category: consumer_subs
    user: soundcloud:users:147986827
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: consumer-premium:main
    user: soundcloud:users:147986827
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_ad
    anonymous_id: (\w|-)+
    impression_object: soundcloud:tcode:[0-9]+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    page_name: consumer-premium:main
    user: soundcloud:users:147986827
  version: '1'
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
    page_name: consumer-premium:main
    click_category: consumer_subs
    user: soundcloud:users:147986827
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: consumer-premium:checkout
    user: soundcloud:users:147986827
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
