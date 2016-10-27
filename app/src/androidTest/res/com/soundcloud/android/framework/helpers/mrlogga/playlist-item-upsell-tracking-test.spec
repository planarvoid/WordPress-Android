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
    impression_object: soundcloud:tcode:1011
    user: soundcloud:users:147986827
    app_version: '[0-9]+'
    connection_type: wifi
    page_name: collection:playlists
    page_urn: soundcloud:playlists:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    click_name: clickthrough::consumer_sub_ad
    client_id: '3152'
    anonymous_id: (\w|-)+
    click_object: soundcloud:tcode:1011
    ts: '[0-9]+'
    user: soundcloud:users:147986827
    app_version: '[0-9]+'
    connection_type: wifi
    page_name: collection:playlists
    page_urn: soundcloud:playlists:[0-9]+
    click_category: consumer_subs
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
    app_version: '[0-9]+'
    connection_type: wifi
    page_name: consumer-premium:main
  version: '0'
