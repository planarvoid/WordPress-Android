--- !ruby/object:MrLoggerLogger::ResultSpec
# Scenario (Android)
# The user is landing on a user profile page and navigating to the info page of the user [PV1].
# From there, s/he is visiting followers page of the user [PV2], and returning back to the info page via the back button [PV3].
# After that, s/he is visiting the followings page of the user [PV4], and returning back to the info page via the back button [PV5].
# Finally, s/he is then navigating to the sounds page of the user [PV6].
#
whitelisted_events:
- pageview
expected_events:
- !ruby/object:MrLoggerLogger::Event
  # PV1
  name: pageview
  params:
    page_name: users:info
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV2
  name: pageview
  params:
    page_name: users:followers
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV3
  name: pageview
  params:
    page_name: users:info
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV4
  name: pageview
  params:
    page_name: users:followings
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV5
  name: pageview
  params:
    page_name: users:info
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV6
  name: pageview
  params:
    page_name: users:main
    page_urn: soundcloud:users:[0-9]+
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    client_id: '3152'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
