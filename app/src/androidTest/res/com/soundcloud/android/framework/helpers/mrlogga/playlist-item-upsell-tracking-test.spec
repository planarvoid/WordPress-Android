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
  version: '0'
