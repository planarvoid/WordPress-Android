#!/usr/bin/env monkeyrunner

import sys
from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice


package  = "com.soundcloud.android"
activity = ".activity.Main"

login    = sys.argv[1] if len(sys.argv) > 1 else "aeffle:m0nk3yz"
apk      = sys.argv[2] if len(sys.argv) > 2 else None

username, password = login.split(':')

device = MonkeyRunner.waitForConnection()

if apk != None:
  device.removePackage(package)
  print "removed package "+package

  device.installPackage(apk)
  print "installed package "+package

MonkeyRunner.sleep(3)
device.startActivity(component=package+'/'+activity)

# unlock screen
device.press('KEYCODE_MENU', MonkeyDevice.DOWN_AND_UP)

MonkeyRunner.sleep(10)

device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
MonkeyRunner.sleep(1.0)
device.press('KEYCODE_ENTER',MonkeyDevice.DOWN_AND_UP)
MonkeyRunner.sleep(1.0)
device.type(username)
device.press('KEYCODE_DPAD_DOWN',MonkeyDevice.DOWN_AND_UP)
MonkeyRunner.sleep(0.5)
device.type(password)
device.press('KEYCODE_ENTER',MonkeyDevice.DOWN_AND_UP)

print "logged in"
MonkeyRunner.sleep(10)
