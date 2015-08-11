--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    trigger: manual
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    player_type: Skippy
    action: play
    client_id: '3152'
    anonymous_id: (\w|-)+
    page_name: stream:main
    user: soundcloud:users:107904111
    connection_type: wifi
    reposted_by: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_owner: soundcloud:users:[0-9]+
    consumer_subs_plan: none
    local_storage_playback: false
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    trigger: manual
    protocol: hls
    ts: '[0-9]+'
    reason: pause
    anonymous_id: (\w|-)+
    connection_type: wifi
    track: soundcloud:tracks:[0-9]+
    player_type: Skippy
    action: pause
    client_id: '3152'
    page_name: stream:main
    user: soundcloud:users:107904111
    reposted_by: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    track_owner: soundcloud:users:[0-9]+
    consumer_subs_plan: none
    local_storage_playback: false
  version: '0'
