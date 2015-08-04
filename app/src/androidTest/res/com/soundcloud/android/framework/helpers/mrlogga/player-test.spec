--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    trigger: manual
    duration: '[0-9]+'
    protocol: hls
    ts: '[0-9]+'
    sound: soundcloud:sounds:[0-9]+
    player_type: Skippy
    action: play
    client_id: '3152'
    anonymous_id: (\w|-)+
    page_name: stream:main
    user: soundcloud:users:107904111
    connection_type: wifi
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    trigger: manual
    protocol: hls
    ts: '[0-9]+'
    reason: pause
    anonymous_id: (\w|-)+
    connection_type: wifi
    duration: '[0-9]+'
    sound: soundcloud:sounds:[0-9]+
    player_type: Skippy
    action: stop
    client_id: '3152'
    page_name: stream:main
    user: soundcloud:users:107904111
  version: '0'
