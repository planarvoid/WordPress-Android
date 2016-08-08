--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    client_id: 3152
    action: play
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:main
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    client_event_id: (\w|-)+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    client_id: 3152
    action: pause
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:main
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    pause_reason: (buffer_underrun|pause)
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    client_event_id: (\w|-)+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  optional: true
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    client_id: 3152
    action: play
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:main
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    client_event_id: (\w|-)+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  optional: true
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    client_id: 3152
    action: pause
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:main
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    pause_reason: pause
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    client_event_id: (\w|-)+
  version: '1'
whitelisted_events:
- audio
