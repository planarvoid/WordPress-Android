--- !ruby/object:MrLoggerLogger::ResultSpec
  whitelisted_events:
  - click
  expected_events:
  - !ruby/object:MrLoggerLogger::Event
    name: click
    params:
      anonymous_id: (\w|-)+
      client_id: 3152
      page_urn: soundcloud:system-playlists:the-upload:soundcloud:users:183
      click_attributes:
        source_urn: soundcloud:system-playlists:the-upload:soundcloud:users:183
        overflow_menu: true
        source: ambient-new-and-hot
      page_name: system-playlist:main
      click_category: engagement
      user: soundcloud:users:18173653
      ts: '[0-9]+'
      app_version: '[0-9]+'
      connection_type: wifi
      click_name: like::add
      query_urn: soundcloud:charts:e6c087565bc5464781f9e45f2911aea3
      click_object: soundcloud:tracks:329382206
      query_position: 0
    version: '1'
