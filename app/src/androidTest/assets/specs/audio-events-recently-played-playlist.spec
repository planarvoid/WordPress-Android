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
    client_id: 3152
    action: play_start
    player_type: (MediaPlayer|Skippy|Flipper)
    consumer_subs_plan: none
    page_name: collection:main
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: (https|hls)
    ts: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    in_playlist: soundcloud:playlists:[0-9]+
    playlist_position: '[0-9]+'
    local_storage_playback: false
    policy: (ALLOW|SNIP|MONETIZE)
    app_version: '[0-9]+'
    monetization_model: (\w|-)+
  version: '1'
