--- !ruby/object:MrLoggerLogger::ResultSpec
  whitelisted_events:
  - audio
  expected_events:
  - !ruby/object:MrLoggerLogger::Event
    name: audio
    params:
      track_owner: soundcloud:users:74594593
      source: ambient-new-and-hot
      client_id: 3152
      action: play_start
      in_system_playlist: soundcloud:system-playlists:the-upload:soundcloud:users:183
      page_urn: soundcloud:system-playlists:the-upload:soundcloud:users:183
      player_type: Skippy|Flipper|MediaPlayer
      consumer_subs_plan: high_tier
      monetization_model: BLACKBOX
      user: soundcloud:users:18173653
      playlist_position: 0
      ts: '[0-9]+'
      connection_type: wifi
      query_urn: soundcloud:charts:e6c087565bc5464781f9e45f2911aea3
      query_position: 0
      anonymous_id: (\w|-)+
      policy: MONETIZE
      page_name: system-playlist:main
      playhead_position: 0
      track_length: 254195
      protocol: hls
      app_version: '[0-9]+'
      track: soundcloud:tracks:329382206
      trigger: manual
      local_storage_playback: false
      client_event_id: (\w|-)+
    version: '1'
