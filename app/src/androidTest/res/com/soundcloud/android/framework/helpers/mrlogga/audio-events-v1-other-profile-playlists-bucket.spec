--- !ruby/object:MrLoggerLogger::ResultSpec
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    impression_name: promoted_playlist
    anonymous_id: e118936fa8fbffdef8117a5a46b09d19
    impression_object: soundcloud:playlists:96874443
    ts: '1462890249811'
    client_id: '3152'
    ad_urn: dfp:ads:110000061-12000000061
    page_name: stream:main
    user: soundcloud:users:151499536
    monetization_type: promoted
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  name: pageview
  params:
    anonymous_id: e118936fa8fbffdef8117a5a46b09d19
    ts: '1462890262193'
    client_id: '3152'
    page_name: playlists:main
    user: soundcloud:users:151499536
  version: '0'
whitelisted_events: all
