--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
  - click
  - offline_sync
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: .*
    client_id: 3152
    connection_type: wifi
    click_name: automatic_likes_sync::enable
    page_name: collection:likes
    click_category: consumer_subs
    user: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: offline_sync
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    track_owner: soundcloud:users:[0-9]+
    event_stage: start
    app_version: .*
    track: soundcloud:tracks:[0-9]+
    client_id: 3152
    connection_type: wifi
    in_likes: true
    in_playlist: false
    user: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: offline_sync
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    track_owner: soundcloud:users:[0-9]+
    event_stage: storage_limit_reached
    app_version: .*
    track: soundcloud:tracks:[0-9]+
    client_id: 3152
    connection_type: wifi
    in_likes: true
    in_playlist: false
    user: soundcloud:users:[0-9]+
  version: '0'
