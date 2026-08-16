import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

# Replace signingConfigs
new_signing = """  signingConfigs {
    create("release") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }"""

content = re.sub(r'  signingConfigs \{.*?(?=\n  buildTypes \{)', new_signing + '\n', content, flags=re.DOTALL)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
