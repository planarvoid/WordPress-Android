--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- impression
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_track
    page_name: collection:likes
    anonymous_id: (\w|-)+
    impression_object: soundcloud:tracks:[0-9]+
    ts: '[0-9]+'
    client_id: '3152'
    user: soundcloud:users:147986827
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    click_object: soundcloud:tracks:[0-9]+
    page_name: collection:likes
    user: soundcloud:users:147986827
    click_name: consumer_sub_track
  version: '0'
