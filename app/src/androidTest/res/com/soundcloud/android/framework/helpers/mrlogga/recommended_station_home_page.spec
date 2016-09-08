--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: search:main
    user: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: stations:main
    user: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    source: stations:suggestions
    client_id: 3152
    source_version: default
    action: play_start
    policy: (ALLOW|MONETIZE)
    player_type: Skippy
    consumer_subs_plan: (\w|-)+
    page_name: stations:main
    source_urn: soundcloud:artist-stations:[0-9]+
    monetization_model: (NOT_APPLICABLE|AD_SUPPORTED)
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    query_urn: soundcloud:artist-radio:(\w|-)+
    local_storage_playback: false
    client_event_id: (\w|-)+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    anonymous_id: (\w|-)+
    track_owner: soundcloud:users:[0-9]+
    source: stations:suggestions
    client_id: 3152
    source_version: default
    action: pause
    play_id: '[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-4[a-fA-F0-9]{3}-[89ab][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'
    policy: (ALLOW|MONETIZE)
    player_type: Skippy
    consumer_subs_plan: (\w|-)+
    page_name: stations:main
    source_urn: soundcloud:artist-stations:[0-9]+
    monetization_model: (NOT_APPLICABLE|AD_SUPPORTED|SUB_HIGH_TIER)
    user: soundcloud:users:[0-9]+
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    protocol: hls
    pause_reason: (buffer_underrun|pause)
    ts: '[0-9]+'
    app_version: '[0-9]+'
    track: soundcloud:tracks:[0-9]+
    connection_type: wifi
    trigger: manual
    query_urn: soundcloud:artist-radio:(\w|-)+
    local_storage_playback: false
    client_event_id: (\w|-)+
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    page_name: search:main
    user: soundcloud:users:[0-9]+
  version: '0'
whitelisted_events:
- pageview
- audio
