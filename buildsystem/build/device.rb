module Build
  class Device
    def self.get_screenshots
      if !$?.success?
        Build.run_command_silent "adb pull /sdcard/Robotium-Screenshots"
        Build.run_command_silent "adb shell rm -R /sdcard/Robotium-Screenshots"
      end
    end
  end
end
