import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace("androidx.navigation.NavGraph.Companion.findStartDestination(navController.graph).id", 'navController.graph.startDestinationRoute ?: "dashboard"')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

