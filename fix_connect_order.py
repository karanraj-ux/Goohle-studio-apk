import re

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'r') as f:
    content = f.read()

launcher1 = """    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            targetPhonePickerLauncher.launch(null)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Contacts permission is required to pick a number") }
        }
    }"""

content = content.replace(launcher1, '')

# We want to insert it after the declaration of targetPhonePickerLauncher.
# The declaration of targetPhonePickerLauncher ends when we see:
#                         }
#                     }
#                 }
#             } catch (e: Exception) {
#                 e.printStackTrace()
#             }
#         }
#     }

target_picker_pattern = r"(val targetPhonePickerLauncher = rememberLauncherForActivityResult.*?\}\n    \})"

match = re.search(target_picker_pattern, content, re.DOTALL)
if match:
    full_target_picker = match.group(1)
    content = content.replace(full_target_picker, full_target_picker + "\n\n" + launcher1)

with open('app/src/main/java/com/example/ui/screens/ConnectScreen.kt', 'w') as f:
    f.write(content)

