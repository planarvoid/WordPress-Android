--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    source: personal-recommended
    client_id: 3152
    action: play
    policy: ALLOW
    player_type: Skippy|MediaPlayer
    consumer_subs_plan: high_tier
    page_name: search:main
    monetization_model: (\w|-)+
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    query_urn: soundcloud:personalizedtracks:(\w|-)+
    local_storage_playback: false
    client_event_id: (\w|-)+
    query_position: 0
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    source: personal-recommended
    client_id: 3152
    action: pause
    policy: ALLOW
    player_type: Skippy|MediaPlayer
    consumer_subs_plan: high_tier
    page_name: search:main
    monetization_model: (\w|-)+
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    pause_reason: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    query_urn: soundcloud:personalizedtracks:(\w|-)+
    local_storage_playback: false
    client_event_id: (\w|-)+
    query_position: 0
  version: '1'
