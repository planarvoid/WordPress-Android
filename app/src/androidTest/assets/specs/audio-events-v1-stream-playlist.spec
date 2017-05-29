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
    in_playlist: soundcloud:playlists:[0-9]+
    playlist_position: 0
    protocol: hls
    ts: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    player_type: Flipper|Skippy
    action: play_start
    client_id: '3152'
    anonymous_id: (\w|-)+
    page_name: stream:main
    user: soundcloud:users:102628335
    connection_type: wifi
    playhead_position: '[0-9]+'
    track_owner: soundcloud:users:[0-9]+
    consumer_subs_plan: none
    local_storage_playback: false
    policy: ALLOW
    app_version: '[0-9]+'
    client_event_id: (\w|-)+
    monetization_model: (\w|-)+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    trigger: manual
    protocol: hls
    ts: '[0-9]+'
    in_playlist: soundcloud:playlists:[0-9]+
    playlist_position: 0
    pause_reason: pause
    play_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    anonymous_id: (\w|-)+
    connection_type: wifi
    track: soundcloud:tracks:[0-9]+
    player_type: Flipper|Skippy
    action: pause
    client_id: '3152'
    page_name: stream:main
    user: soundcloud:users:102628335
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    track_owner: soundcloud:users:[0-9]+
    consumer_subs_plan: none
    local_storage_playback: false
    policy: ALLOW
    app_version: '[0-9]+'
    client_event_id: (\w|-)+
    monetization_model: (\w|-)+
  version: '0'
