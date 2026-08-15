import os

file_path = "app/src/main/java/com/example/ui/screens/ConnectScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

target = "                            PhoneRulesUI(ruleViewModel)\n                        }\n                    }\n                }\n            }"
replacement = """                            PhoneRulesUI(ruleViewModel)
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = { navController.navigate(Screen.Automation.route) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Webhook, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Manage Webhooks")
                            }
                        }
                    }
                }
            }"""

content = content.replace(target, replacement)

# Add Webhook icon import if missing
if "import androidx.compose.material.icons.rounded.Webhook" not in content:
    content = content.replace("import androidx.compose.material.icons.rounded.ExpandMore", "import androidx.compose.material.icons.rounded.ExpandMore\nimport androidx.compose.material.icons.rounded.Webhook")

with open(file_path, "w") as f:
    f.write(content)
print("Patched ConnectScreen with Webhooks successfully")
