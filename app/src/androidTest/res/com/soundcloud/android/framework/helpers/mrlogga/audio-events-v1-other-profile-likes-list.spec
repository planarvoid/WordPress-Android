--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: e118936fa8fbffdef8117a5a46b09d19
    ts: '1462890510659'
    client_id: '3152'
    page_name: users:likes
    user: soundcloud:users:151499536
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: e118936fa8fbffdef8117a5a46b09d19
    track_owner: soundcloud:users:662337
    client_id: 3152
    action: play
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:likes
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:151499536
    playhead_position: 9179
    track_length: 295505
    protocol: hls
    ts: 1462890513134
    app_version: '410'
    track: soundcloud:tracks:25276427
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    client_event_id: 8b65363d-d5f8-42b4-a6ad-0def591345e6
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: e118936fa8fbffdef8117a5a46b09d19
    track_owner: soundcloud:users:662337
    client_id: 3152
    action: pause
    policy: ALLOW
    player_type: Skippy
    consumer_subs_plan: none
    page_name: users:likes
    monetization_model: NOT_APPLICABLE
    user: soundcloud:users:151499536
    playhead_position: 17499
    track_length: 295505
    protocol: hls
    pause_reason: pause
    ts: 1462890520861
    app_version: '410'
    track: soundcloud:tracks:25276427
    connection_type: wifi
    trigger: manual
    local_storage_playback: false
    client_event_id: c60ad437-caff-4918-9265-f4bd06d3db6f
  version: '1'
whitelisted_events: all
