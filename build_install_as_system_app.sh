# Copyright (c) 2015-2020, Swiss Federal Institute of Technology (ETH Zurich)
# All rights reserved.
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# 
# * Redistributions in binary form must reproduce the above copyright notice,
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# 
# * Neither the name of the copyright holder nor the names of its
#   contributors may be used to endorse or promote products derived from
#   this software without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
# 
#!/bin/bash

# CHANGE THESE FOR YOUR APPa
app_package="org.eth.tik.meter"
dir_app_name="DLEmeter"
MAIN_ACTIVITY="MainActivity"

ADB="adb" # how you execute adb
ADB_SH="$ADB shell" # this script assumes using `adb root`. for `adb su` see `Caveats`

path_sysapp="/system/priv-app" # assuming the app is priviledged
apk_host="./app/build/outputs/apk/debug/app-debug.apk"
apk_name=$dir_app_name".apk"
apk_target_dir="$path_sysapp/$dir_app_name"
apk_target_sys="$apk_target_dir/$apk_name"

# Delete previous APK
rm -f $apk_host

# Compile the APK: you can adapt this for production build, flavors, etc.
./gradlew assembleDebug || exit -1 # exit on failure

echo "---------------------------------------------------------------------------------------------------------"
echo "Manual steps to install the app as a system app:"
echo " 1.) Install the application normally using >adb install ${apk_host}<"
echo " 2.) Make sure the app is not running >adb shell \"am force-stop $app_package\"<"
echo " 3.) Open an adb shell"
echo " 4.) Request a sudo shell with the >su< command."
echo " 5.) Remount the system partition as writeable >mount -o rw,remount /system<"
echo " 6.) Move the app-install to the system partition >mv /data/app/org.eth.tik.meter-1/ ${apk_target_dir}"
echo " 7.) Change the owner, group and permissions on the system directory:"
echo "   a.) chown -R root $apk_target_dir"
echo "   b.) chgrp -R root $apk_target_dir"
echo "   c.) chmod 755 $apk_target_dir"
echo "   d.) chmod 644 $apk_target_sys"
echo " 8.) Remount the system partition as read only >mount -o ro,remount /system<"
echo " 9.) Reboot the device >adb reboot<"
echo "10.) Start the app, either manually or with an intent: >adb shell \"am start -n \"$app_package/$app_package.$MAIN_ACTIVITY\" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER\"<"
echo "11.) After this procedure, app development can be done using the AndroidStudio Run option"
echo "---------------------------------------------------------------------------------------------------------"

