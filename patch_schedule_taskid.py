import re

with open('app/src/main/java/com/example/ui/screens/ScheduleScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('.putLong("taskId", id)', '.putInt("taskId", id.toInt())')

with open('app/src/main/java/com/example/ui/screens/ScheduleScreen.kt', 'w') as f:
    f.write(content)
