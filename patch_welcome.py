import re

with open('app/src/main/java/com/example/ui/screens/WelcomeOnboarding.kt', 'r') as f:
    content = f.read()

# Add rememberScrollState import
if "import androidx.compose.foundation.rememberScrollState" not in content:
    content = content.replace('import androidx.compose.foundation.shape.RoundedCornerShape', 
                              'import androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll')

# Add scroll to Column
content = content.replace('.background(MaterialTheme.colorScheme.background)\n            .statusBarsPadding()\n            .navigationBarsPadding(),',
                          '.background(MaterialTheme.colorScheme.background)\n            .statusBarsPadding()\n            .navigationBarsPadding()\n            .verticalScroll(rememberScrollState()),')

# Remove weight(1f) spacer and replace with a fixed spacer to allow scrolling instead of flex layout
content = content.replace('Spacer(modifier = Modifier.weight(1f))', 'Spacer(modifier = Modifier.height(32.dp))')

with open('app/src/main/java/com/example/ui/screens/WelcomeOnboarding.kt', 'w') as f:
    f.write(content)
