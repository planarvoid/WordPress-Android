--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- impression
- click
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: promoted_track
    monetization_type: promoted
    ad_urn: dfp:ads:[0-9,-]+
    ts: '[0-9]+'
    impression_object: soundcloud:tracks:[0-9]+
    promoted_by: OPTIONAL
    client_id: '3152'
    anonymous_id: (\w|-)+
    page_name: stream:main
    user: soundcloud:users:107904111
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    monetization_type: promoted
    ad_urn: dfp:ads:[0-9,-]+
    click_object: soundcloud:tracks:[0-9]+
    ts: '[0-9]+'
    promoted_by: OPTIONAL
    click_name: item_navigation
    client_id: '3152'
    anonymous_id: (\w|-)+
    page_name: stream:main
    click_target: soundcloud:tracks:[0-9]+
    user: soundcloud:users:107904111
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    trigger: manual
    protocol: hls
    ts: '[0-9]+'
    promoted_by: OPTIONAL
    anonymous_id: (\w|-)+
    connection_type: wifi
    monetization_type: promoted
    duration: '[0-9]+'
    ad_urn: dfp:ads:[0-9,-]+
    sound: soundcloud:sounds:[0-9]+
    player_type: Skippy
    action: play
    client_id: '3152'
    page_name: stream:main
    user: soundcloud:users:107904111
  version: '0'
