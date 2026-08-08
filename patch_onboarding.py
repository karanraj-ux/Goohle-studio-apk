import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_code = """                            scope.launch {
                                settingsRepository.updateBoolean(SettingsRepository.ONBOARDING_COMPLETE, true)
                                if (tabRoute != null) {
                                    initialRoute = tabRoute
                                }
                            }"""

new_code = """                            scope.launch {
                                if (tabRoute != null) {
                                    initialRoute = tabRoute
                                }
                                settingsRepository.updateBoolean(SettingsRepository.ONBOARDING_COMPLETE, true)
                            }"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

