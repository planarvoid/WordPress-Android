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
    player_type: Skippy
    action: play
    client_id: '3152'
    anonymous_id: (\w|-)+
    page_name: you:playlists
    user: soundcloud:users:107904111
    connection_type: wifi
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
    pause_reason: pause
    in_playlist: soundcloud:playlists:[0-9]+
    playlist_position: 0
    anonymous_id: (\w|-)+
    connection_type: wifi
    track: soundcloud:tracks:[0-9]+
    player_type: Skippy
    action: pause
    client_id: '3152'
    page_name: you:playlists
    user: soundcloud:users:107904111
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    track_owner: soundcloud:users:[0-9]+
    consumer_subs_plan: none
    local_storage_playback: false
  version: '0'
