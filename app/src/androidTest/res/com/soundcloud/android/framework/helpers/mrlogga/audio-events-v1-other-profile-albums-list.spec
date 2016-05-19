--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: e118936fa8fbffdef8117a5a46b09d19
    ts: '1462890214085'
    client_id: '3152'
    page_name: users:albums
    user: soundcloud:users:151499536
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: e118936fa8fbffdef8117a5a46b09d19
    ts: '1462890215783'
    client_id: '3152'
    page_name: playlists:main
    user: soundcloud:users:151499536
  version: '0'
whitelisted_events: all
