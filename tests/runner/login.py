#!/usr/bin/env monkeyrunner

import sys
from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice


package  = "com.soundcloud.android"
activity = ".activity.Main"

login    = sys.argv[1] if len(sys.argv) > 1 else "aeffle:m0nk3yz"
apk      = sys.argv[2] if len(sys.argv) > 2 else None
username, password = login.split(':')

device = MonkeyRunner.waitForConnection()

def press(device, keycode, action=MonkeyDevice.DOWN_AND_UP):
  device.press(keycode, action)
  MonkeyRunner.sleep(2.0)

if apk != None:
  device.removePackage(package)
  print "removed package "+package

  device.installPackage(apk)
  print "installed package "+package

MonkeyRunner.sleep(3)
device.startActivity(component=package+'/'+activity)

# unlock screen
press(device, 'KEYCODE_MENU')

MonkeyRunner.sleep(10)

press(device, 'KEYCODE_DPAD_DOWN')
press(device, 'KEYCODE_ENTER')
device.type(username)
press(device, 'KEYCODE_DPAD_DOWN')
MonkeyRunner.sleep(0.5)
device.type(password)
press(device, 'KEYCODE_ENTER')

print "logged in"
MonkeyRunner.sleep(10)
