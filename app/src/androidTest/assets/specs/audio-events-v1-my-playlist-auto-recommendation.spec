--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    client_id: '3152'
    action: play_start
    player_type: Skippy|Flipper|MediaPlayer
    consumer_subs_plan: (\w|-)+
    page_name: collection:playlists
    in_playlist: soundcloud:playlists:[0-9]+
    user: soundcloud:users:50749473
    playlist_position: '2'
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    track: soundcloud:tracks:188740741
    connection_type: wifi
    trigger: manual
    local_storage_playback: 'false'
    policy: ALLOW
    app_version: '[0-9]+'
    monetization_model: (\w|-)+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    play_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    client_id: '3152'
    action: pause
    player_type: Skippy|Flipper|MediaPlayer
    consumer_subs_plan: (\w|-)+
    page_name: collection:playlists
    in_playlist: soundcloud:playlists:[0-9]+
    user: soundcloud:users:50749473
    playlist_position: '2'
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    track: soundcloud:tracks:188740741
    connection_type: wifi
    pause_reason: skip
    trigger: manual
    local_storage_playback: 'false'
    policy: ALLOW
    app_version: '[0-9]+'
    monetization_model: (\w|-)+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    source: recommender
    client_id: '3152'
    source_version: (\w|-)+
    action: play_start
    player_type: Skippy|Flipper|MediaPlayer
    consumer_subs_plan: none
    page_name: collection:overview
    in_playlist: soundcloud:playlists:[0-9]+
    user: soundcloud:users:50749473
    playlist_position: '3'
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    local_storage_playback: 'false'
    policy: ALLOW
    app_version: '[0-9]+'
    monetization_model: (\w|-)+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    play_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    source: recommender
    client_id: '3152'
    source_version: (\w|-)+
    action: pause
    player_type: Skippy|Flipper|MediaPlayer
    consumer_subs_plan: none
    page_name: collection:main
    in_playlist: soundcloud:playlists:[0-9]+
    user: soundcloud:users:50749473
    playlist_position: '3'
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    pause_reason: pause
    trigger: manual
    local_storage_playback: 'false'
    policy: ALLOW
    app_version: '[0-9]+'
    monetization_model: (\w|-)+
  version: '1'
