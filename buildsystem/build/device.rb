module Build
  class Device
    def self.get_screenshots
      if !$?.success?
        Build.run_command_silent "adb -d pull /sdcard/Robotium-Screenshots"
        Build.run_command_silent "adb -d shell rm -R /sdcard/Robotium-Screenshots"
      end
    end
  end
end
