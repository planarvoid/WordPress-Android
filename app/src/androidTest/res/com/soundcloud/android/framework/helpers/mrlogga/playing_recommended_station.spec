--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    source: stations:suggestions
    client_id: 3152
    source_version: default
    action: pause
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: (\w|-)+
    page_name: search:main
    source_urn: soundcloud:artist-stations:[0-9]+
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    pause_reason: (pause|buffer_underrun)
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    query_urn: soundcloud:artist-radio:(\w|-)+
    local_storage_playback: false
    client_event_id: (\w|-)+
  version: '1'
whitelisted_events:
- audio
