--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- audio
- pageview
expected_events:
# Enter the new for you screen
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    page_name: new_for_you:main

    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: (\w|-)+
    user: soundcloud:users:(\w|-)+
  version: '1'
  optional: false

# Start playback from the main play button
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    action: play_start
    page_name: new_for_you:main
    source: new_for_you
    query_urn: soundcloud:newforyou:(\w|-)+
    query_position: 0

    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:(\w|-)+
    client_id: '[0-9]+'
    policy: (\w|-)+
    player_type: (\w|-)+
    consumer_subs_plan: (\w|-)+
    monetization_model: (\w|-)+
    user: soundcloud:users:(\w|-)+
    playhead_position: 0
    track_length: '[0-9]+'
    protocol: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:(\w|-)+
    connection_type: (\w|-)+
    trigger: manual
    local_storage_playback: (\w|-)+
    client_event_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
  version: '1'
  optional: false
