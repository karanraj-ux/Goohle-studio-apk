import re

with open('app/src/main/java/com/example/ui/screens/AssistantOnboarding.kt', 'r') as f:
    content = f.read()

# Add rememberScrollState import
if "import androidx.compose.foundation.rememberScrollState" not in content:
    content = content.replace('import androidx.compose.foundation.shape.RoundedCornerShape', 
                              'import androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll')

# Add scroll to Column
content = content.replace('        modifier = Modifier\n            .fillMaxSize()\n            .background(MaterialTheme.colorScheme.background)\n            .statusBarsPadding()\n            .navigationBarsPadding(),',
                          '        modifier = Modifier\n            .fillMaxSize()\n            .background(MaterialTheme.colorScheme.background)\n            .statusBarsPadding()\n            .navigationBarsPadding()\n            .verticalScroll(rememberScrollState()),')

with open('app/src/main/java/com/example/ui/screens/AssistantOnboarding.kt', 'w') as f:
    f.write(content)
