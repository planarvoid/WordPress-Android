--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- impression
- click
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    monetization_type: promoted
    ad_urn: dfp:ads:[0-9,-]+
    click_object: soundcloud:tracks:[0-9]+
    ts: '[0-9]+'
    promoted_by: soundcloud:users:[0-9]+
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
    anonymous_id: (\w|-)+
    connection_type: wifi
    monetization_type: promoted
    track_length: '[0-9]+'
    ad_urn: dfp:ads:[0-9,-]+
    track: soundcloud:tracks:[0-9]+
    player_type: Skippy
    action: play
    client_id: '3152'
    page_name: stream:main
    user: soundcloud:users:107904111
    track_owner: soundcloud:users:[0-9]+
    promoted_by: soundcloud:users:[0-9]+
    consumer_subs_plan: none
    playhead_position: '0'
    local_storage_playback: 'false'
  version: '0'
