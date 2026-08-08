import re

with open('app/src/main/java/com/example/ui/screens/AddScheduleScreen.kt', 'r') as f:
    content = f.read()

bad_launcher_regex = r"    val smsPermissionLauncher = androidx\.activity\.compose\.rememberLauncherForActivityResult\(\s*androidx\.activity\.result\.contract\.ActivityResultContracts\.RequestPermission\(\)\s*\) \{ isGranted ->.*?        } else \{\n            // Can't save without permission\n        \}\n    \}\n"

content = re.sub(bad_launcher_regex, '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/AddScheduleScreen.kt', 'w') as f:
    f.write(content)

