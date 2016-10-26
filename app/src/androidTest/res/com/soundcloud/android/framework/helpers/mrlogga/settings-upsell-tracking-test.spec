--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- impression
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_ad
    client_id: '3152'
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    impression_object: soundcloud:tcode:1007
    user: soundcloud:users:147986827
    app_version: '[0-9]+'
    connection_type: wifi
    page_name: settings:main
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    click_name: clickthrough::consumer_sub_ad
    client_id: '3152'
    anonymous_id: (\w|-)+
    click_object: soundcloud:tcode:1007
    ts: '[0-9]+'
    user: soundcloud:users:147986827
    click_category: consumer_subs
    page_name: settings:main
    app_version: '[0-9]+'
    connection_type: wifi
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_ad
    client_id: '3152'
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    impression_object: soundcloud:tcode:1008
    user: soundcloud:users:147986827
    app_version: '[0-9]+'
    connection_type: wifi
    page_name: settings:offline_sync_settings
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    click_name: clickthrough::consumer_sub_ad
    client_id: '3152'
    anonymous_id: (\w|-)+
    click_object: soundcloud:tcode:1008
    ts: '[0-9]+'
    user: soundcloud:users:147986827
    click_category: consumer_subs
    page_name: settings:offline_sync_settings
    app_version: '[0-9]+'
    connection_type: wifi
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_ad
    client_id: '3152'
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    impression_object: soundcloud:tcode:[0-9]+
    user: soundcloud:users:147986827
    page_name: consumer-premium:main
    app_version: '[0-9]+'
    connection_type: wifi
  version: 0
