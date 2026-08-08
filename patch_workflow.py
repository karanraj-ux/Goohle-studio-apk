import re

with open('.github/workflows/android-build.yml', 'r') as f:
    content = f.read()

# Replace the Build Release APK step
old_step = """    - name: Build Release APK
      run: ./gradlew assembleRelease --no-daemon || ./gradlew assembleDebug --no-daemon"""

new_step = """    - name: Build Release APK
      run: |
        cp debug.keystore release.keystore
        ./gradlew assembleRelease --no-daemon
      env:
        STORE_PASSWORD: android
        KEY_ALIAS: androiddebugkey"""

content = content.replace(old_step, new_step)

with open('.github/workflows/android-build.yml', 'w') as f:
    f.write(content)
