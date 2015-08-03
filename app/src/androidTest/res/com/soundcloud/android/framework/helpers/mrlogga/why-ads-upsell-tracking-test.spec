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
    impression_object: soundcloud:tcode:1012
    user: soundcloud:users:147986827
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_ad
    client_id: '3152'
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    impression_object: soundcloud:tcode:1006
    user: soundcloud:users:147986827
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: audio_ad_impression
    monetization_type: audio_ad
    ad_urn: dfp:ads:110000021-12000000021
    ts: '[0-9]+'
    impression_object: soundcloud:tracks:135376655
    monetized_object: soundcloud:tracks:163824437
    client_id: '3152'
    anonymous_id: (\w|-)+
    page_name: deeplink
    user: soundcloud:users:147986827
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    click_name: clickthrough::consumer_sub_ad
    client_id: '3152'
    anonymous_id: (\w|-)+
    click_object: soundcloud:tcode:1006
    ts: '[0-9]+'
    user: soundcloud:users:147986827
  version: '0'
