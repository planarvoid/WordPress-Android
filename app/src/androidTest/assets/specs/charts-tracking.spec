--- !ruby/object:MrLoggerLogger::ResultSpec
# Scenario (Android)
# The user is on search:main and clicks on the New & Hot item within the Charts
# module which brings him/her to the 'New & Hot' page (genre-independent charts) [PV1].
# The user is then navigating to the Top 50 page [PV2]. Afterwards, the user is using
# the back button to navigate back to search:main [PV3]. From there s/he clicks on
# "Browse All Genres" within the "Charts by Genre" module [PV4]. There, s/he clicks on
# the "Country" genre item which brings him/her to the charts "New & Hot Country" [PV5]
# to like the second track on that page [CL1]. Afterwards, the user uses the back button
# to navigate back to the genre page [PV6]. S/he navigates to the "Audio" page [PV7] and clicks
# on "Science" which brings him/her to the "Science" Audio Charts [PV8].
# Finally, s/he starts listening to the first track there [A1].
whitelisted_events:
- pageview
- click
- audio
expected_events:
- !ruby/object:MrLoggerLogger::Event
  # PV1
  name: pageview
  params:
    page_name: charts:music_trending:all-music
    query_urn: soundcloud:charts:(\w|-)+
    # more:
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV2
  name: pageview
  params:
    page_name: charts:music_top_50:all-music
    query_urn: soundcloud:charts:(\w|-)+
    # more:
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV3
  name: pageview
  params:
    page_name: search:main
    # more:
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV4
  name: pageview
  params:
    page_name: charts:music_genres
    # more:
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV5
  name: pageview
  params:
    page_name: charts:music_trending:country
    query_urn: soundcloud:charts:(\w|-)+
    # more:
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # CL1
  name: click
  params:
    page_name: charts:music_trending:country
    query_urn: soundcloud:charts:(\w|-)+
    query_position: 0
    click_name: like::(add|remove)
    click_object: soundcloud:tracks:(\w|-)+
    click_attributes:
        overflow_menu: true
    # more:
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
    click_category: engagement
- !ruby/object:MrLoggerLogger::Event
  # PV6
  name: pageview
  params:
    page_name: charts:music_genres
    # more:
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV7
  name: pageview
  params:
    page_name: charts:audio_genres
    # more:
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  # PV8
  name: pageview
  params:
    page_name: charts:audio_trending:business
    query_urn: soundcloud:charts:(\w|-)+
    # more:
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    connection_type: wifi
  version: '1'
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    app_version: '[0-9]+'
    client_id: 3152
    connection_type: wifi
    click_attributes:
          trigger: (auto|manual)
    click_name: player::(max|min)
    user: soundcloud:users:[0-9]+
  version: '0'
- !ruby/object:MrLoggerLogger::Event
  # A1
  name: audio
  params:
    page_name: charts:audio_trending:business
    query_urn: soundcloud:charts:(\w|-)+
    query_position: 0
    # more:
    action: play_start
    trigger: manual
    playhead_position: '[0-9]+'
    track_length: '[0-9]+'
    connection_type: wifi
    track: soundcloud:tracks:[0-9]+
    client_id: '3152'
    user: soundcloud:users:[0-9]+
    anonymous_id: (\w|-)+
    ts: '[0-9]+'
    track_owner: soundcloud:users:[0-9]+
    policy: ALLOW
    player_type: Skippy|MediaPlayer
    consumer_subs_plan: none|high_tier
    monetization_model: (\w|-)+
    protocol: hls|https
    app_version: '[0-9]+'
    local_storage_playback: false
    client_event_id: (\w|-)+
    monetization_model: (\w|-)+
    protocol: hls|https
    app_version: '[0-9]+'
    local_storage_playback: false
    client_event_id: (\w|-)+
  version: '0'
