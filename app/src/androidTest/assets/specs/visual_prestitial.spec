--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events:
- impression
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: impression
  params:
    anonymous_id: '(\w|-)+'
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    client_event_id: '(\w|-)+'
    connection_type: wifi
    page_name: ads:display
    ad_urn: dfp:ads:[0-9,-]+
    impression_name: display
    monetization_type: prestitial
    user: soundcloud:users:[0-9]+
  version: '1'
