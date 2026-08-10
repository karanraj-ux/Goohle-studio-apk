with open("app/src/main/java/com/example/SmsProcessor.kt", "r") as f:
    content = f.read()

old_otp_logic = """            if (extractOtps) {
                val otpMatcher = java.util.regex.Pattern.compile("\\b\\d{4,8}\\b").matcher(body)
                if (otpMatcher.find()) {
                    val otp = otpMatcher.group()
                    fwdMsg = "OTP: $otp\\nFwd from $sender: $body"
                }
            }"""

new_otp_logic = """            if (extractOtps) {
                val otpMatcher = java.util.regex.Pattern.compile("\\b\\d{4,8}\\b").matcher(body)
                if (otpMatcher.find()) {
                    val otp = otpMatcher.group()
                    fwdMsg = "OTP: $otp\\nFwd from $sender: $body"
                } else {
                    shouldForward = false
                }
            }"""

content = content.replace(old_otp_logic, new_otp_logic)

with open("app/src/main/java/com/example/SmsProcessor.kt", "w") as f:
    f.write(content)
