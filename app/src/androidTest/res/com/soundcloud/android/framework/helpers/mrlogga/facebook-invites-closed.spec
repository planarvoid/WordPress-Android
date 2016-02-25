--- !ruby/object:MrLoggerLogger::ResultSpec
whitelisted_events: 
- click
expected_events:
- !ruby/object:MrLoggerLogger::Event
  name: click
  params:
    click_name: (fb::no_image::dismiss|fb::with_images::dismiss)
    anonymous_id: '[0-9a-z-]+'
    app_version: '[0-9]+'
    ts: '\d+'
    click_category: invite_friends
    client_id: 3152
    connection_type: wifi
    page_name: stream:main
    user: soundcloud:users:[0-9]+
  version: '1'
