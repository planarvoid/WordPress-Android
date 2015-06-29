--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- impression
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: consumer_sub_track
    anonymous_id: (\w|-)+
    impression_object: soundcloud:tracks:[0-9]+
    ts: '[0-9]+'
    client_id: '3152'
    user: soundcloud:users:147986827
  version: '0'
