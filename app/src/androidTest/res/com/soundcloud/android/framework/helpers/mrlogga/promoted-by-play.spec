--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- click
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    monetization_type: promoted
    ad_urn: dfp:ads:[0-9,-]+
    click_object: soundcloud:tracks:[0-9]+
    ts: '[0-9]+'
    promoted_by: soundcloud:users:[0-9]+
    click_name: item_navigation
    client_id: '3152'
    anonymous_id: (\w|-)+
    page_name: stream:main
    click_target: soundcloud:tracks:[0-9]+
    user: soundcloud:users:107904111
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
          trigger: (auto|manual)
    click_name: player::(max|min)
    user: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    trigger: manual
    protocol: (hls|https)
    ts: '[0-9]+'
    anonymous_id: (\w|-)+
    connection_type: wifi
    source: stream
    monetization_type: promoted
    track_length: '[0-9]+'
    ad_urn: dfp:ads:[0-9,-]+
    track: soundcloud:tracks:[0-9]+
    player_type: (Skippy|MediaPlayer)
    action: play_start
    client_id: '3152'
    page_name: stream:main
    user: soundcloud:users:107904111
    track_owner: soundcloud:users:[0-9]+
    promoted_by: soundcloud:users:[0-9]+
    consumer_subs_plan: none
    playhead_position: '[0-9]+'
    local_storage_playback: 'false'
    policy: 'ALLOW'
    app_version: '[0-9]+'
    client_event_id: (\w|-)+
    monetization_model: (\w|-)+
  version: '0'
