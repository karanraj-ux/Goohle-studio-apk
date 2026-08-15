import os

file_path = "app/src/main/java/com/example/MainActivity.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("composable(Screen.Connect.route) { com.example.ui.screens.ConnectScreen(viewModel) }", "composable(Screen.Connect.route) { com.example.ui.screens.ConnectScreen(viewModel, onNavigateToWebhooks = { navController.navigate(Screen.Automation.route) }) }")

with open(file_path, "w") as f:
    f.write(content)
print("Patched MainActivity successfully")
