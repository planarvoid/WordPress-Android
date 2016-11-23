--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: player::max
    click_attributes:
      trigger: auto
    user: soundcloud:users:[0-9]+
  version: 0
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    trigger: manual
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: (hls|https)
    ts: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    player_type: (Skippy|MediaPlayer)
    action: play_start
    client_id: '3152'
    anonymous_id: (\w|-)+
    page_name: stream:main
    source: stream
    user: soundcloud:users:[0-9]+
    connection_type: wifi
    reposted_by: soundcloud:users:[0-9]+
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
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_name: player::min
    click_attributes:
      trigger: auto
    user: soundcloud:users:[0-9]+
  version: 0
