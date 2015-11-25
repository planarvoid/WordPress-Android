--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    source: stream
    client_id: 3152
    action: play
    player_type: (Skippy|MediaPlayer)
    consumer_subs_plan: none
    page_name: stream:main
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: (hls|https)
    ts: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    reposted_by: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
          overflow_menu: true
          source: stream
    page_urn: soundcloud:tracks:[0-9]+
    click_name: repost::(add|remove)
    click_object: soundcloud:tracks:[0-9]+
    click_category: engagement
    page_name: tracks:main
    user: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
          overflow_menu: true
          source: stream
    page_urn: soundcloud:tracks:[0-9]+
    click_name: repost::(add|remove)
    click_object: soundcloud:tracks:[0-9]+
    click_category: engagement
    page_name: tracks:main
    user: soundcloud:users:[0-9]+
  version: '0'
