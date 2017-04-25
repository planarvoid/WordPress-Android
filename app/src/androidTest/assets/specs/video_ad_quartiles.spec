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
    click_name: swipe_skip
    click_category: player_interaction
    user: soundcloud:users:[0-9]+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    monetized_object: soundcloud:tracks:[0-9]+
    ad_urn: dfp:ads:[0-9,-]+
    connection_type: wifi
    click_name: ad::first_quartile
    page_name: deeplink
    user: soundcloud:users:[0-9]+
    monetization_type: video_ad
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    monetized_object: soundcloud:tracks:[0-9]+
    ad_urn: dfp:ads:[0-9,-]+
    connection_type: wifi
    click_name: ad::second_quartile
    page_name: deeplink
    user: soundcloud:users:[0-9]+
    monetization_type: video_ad
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    monetized_object: soundcloud:tracks:[0-9]+
    ad_urn: dfp:ads:[0-9,-]+
    connection_type: wifi
    click_name: ad::third_quartile
    page_name: deeplink
    user: soundcloud:users:[0-9]+
    monetization_type: video_ad
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    monetized_object: soundcloud:tracks:[0-9]+
    ad_urn: dfp:ads:[0-9,-]+
    connection_type: wifi
    click_name: ad::finish
    page_name: deeplink
    user: soundcloud:users:[0-9]+
    monetization_type: video_ad
  version: '1'
