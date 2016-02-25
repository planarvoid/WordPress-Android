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
    client_id: 3152
    app_version: .*
    connection_type: wifi
    click_name: playlist_to_offline::add
    click_object: soundcloud:playlists:190342938
    click_category: consumer_subs
    page_name: playlists:main
    user: soundcloud:users:136770909
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: offline_sync
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: .*
    track_owner: soundcloud:users:16945945
    event_stage: start
    track: soundcloud:tracks:146350105
    client_id: 3152
    connection_type: wifi
    in_likes: false
    in_playlist: true
    user: soundcloud:users:136770909
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: .*
    client_id: 3152
    connection_type: wifi
    click_name: playlist_to_offline::remove
    click_object: soundcloud:playlists:190342938
    click_category: consumer_subs
    page_name: playlists:main
    user: soundcloud:users:136770909
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: offline_sync
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: .*
    track_owner: soundcloud:users:16945945
    event_stage: user_cancelled
    track: soundcloud:tracks:146350105
    client_id: 3152
    connection_type: wifi
    in_likes: false
    in_playlist: true
    user: soundcloud:users:136770909
  version: '1'
