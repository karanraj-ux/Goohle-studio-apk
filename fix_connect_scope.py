import re

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'r') as f:
    content = f.read()

# Move scope and snackbarHostState to the beginning of ConnectScreen Content block
content = content.replace('    val snackbarHostState = com.example.LocalSnackbarHostState.current\n    val scope = rememberCoroutineScope()', '')

# Insert them after 'val context = LocalContext.current'
content = content.replace('val context = LocalContext.current', 'val context = LocalContext.current\n    val snackbarHostState = com.example.LocalSnackbarHostState.current\n    val scope = rememberCoroutineScope()')

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'w') as f:
    f.write(content)
