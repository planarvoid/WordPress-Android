--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
- impression
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_ad
    anonymous_id: (\w|-)+
    impression_object: soundcloud:tcode:1027
    ts: '[0-9]+'
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    app_version: '[0-9]+'
    connection_type: wifi
    page_name: stream:main
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    click_object: soundcloud:tcode:1027
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    click_name: clickthrough::consumer_sub_ad
    click_category: consumer_subs
    page_name: stream:main
    app_version: '[0-9]+'
    connection_type: wifi
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: upgrade
    user: soundcloud:users:[0-9]+
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_ad
    anonymous_id: (\w|-)+
    impression_object: soundcloud:tcode:3002
    ts: '[0-9]+'
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    page_name: consumer-premium:main
    app_version: '[0-9]+'
    connection_type: wifi
  version: '0'
