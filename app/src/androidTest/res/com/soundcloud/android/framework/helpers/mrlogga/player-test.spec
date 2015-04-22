--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- audio
- pageview
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    client_id: '3152'
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    page_name: drawer
    user: soundcloud:users:107904111
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: audio
  params:
    trigger: manual
    monetization_type: audio_ad
    duration: '89973'
    protocol: hls
    ts: '[0-9]+'
    sound: soundcloud:sounds:176405526
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
    monetization_type: audio_ad
    duration: '89973'
    sound: soundcloud:sounds:176405526
    player_type: Skippy
    action: stop
    client_id: '3152'
    page_name: stream:main
    user: soundcloud:users:107904111
  version: '0'