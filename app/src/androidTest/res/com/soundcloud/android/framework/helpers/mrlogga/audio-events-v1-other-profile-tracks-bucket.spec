--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:218682740
    client_id: 3152
    action: play_start
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:main
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:151499536
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:263398924
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    play_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:218682740
    client_id: 3152
    action: pause
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:main
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:151499536
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    pause_reason: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:263398924
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  optional: true
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:218682740
    client_id: 3152
    action: play_start
    play_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:main
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:151499536
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:263398924
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  optional: true
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:218682740
    client_id: 3152
    action: pause
    play_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:main
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:151499536
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    pause_reason: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:263398924
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
  version: '1'
whitelisted_events:
- audio
