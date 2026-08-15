import os

file_path = "app/src/main/java/com/example/ui/screens/ConnectScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("fun ConnectScreen(viewModel: MainViewModel) {", "fun ConnectScreen(viewModel: MainViewModel, onNavigateToWebhooks: () -> Unit = {}) {")
content = content.replace("onClick = { navController.navigate(Screen.Automation.route) }", "onClick = { onNavigateToWebhooks() }")

with open(file_path, "w") as f:
    f.write(content)
print("Patched ConnectScreen with onNavigateToWebhooks successfully")
