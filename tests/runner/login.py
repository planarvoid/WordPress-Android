#!/usr/bin/env monkeyrunner

import sys
from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice

package = "com.soundcloud.android"
activity = ".activity.Main"
apk = sys.argv[1] if len(sys.argv) > 1 else None

device = MonkeyRunner.waitForConnection()

if apk != None:
  device.removePackage(package)
  print "removed package "+package

  device.installPackage(apk)
  print "installed package "+package

  MonkeyRunner.sleep(3)

device.startActivity(component=package+'/'+activity)

MonkeyRunner.sleep(10)

device.press('KEYCODE_DPAD_DOWN', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_ENTER',MonkeyDevice.DOWN_AND_UP)
device.type("jberkel_testing")
device.press('KEYCODE_DPAD_DOWN',MonkeyDevice.DOWN_AND_UP)
device.type("clever")
device.press('KEYCODE_ENTER',MonkeyDevice.DOWN_AND_UP)

print "logged in"
MonkeyRunner.sleep(10)

snap = device.takeSnapshot()
snap.writeToFile("after_login.png")
