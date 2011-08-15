#!/usr/bin/env monkeyrunner

import sys
import time
from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice

file = sys.argv[1] if len(sys.argv) > 1 else "screenshot-%s.png" % int(time.time())

device = MonkeyRunner.waitForConnection()
snap = device.takeSnapshot()
snap.writeToFile(file)
