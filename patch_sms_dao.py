import re

with open('app/src/main/java/com/example/data/SmsLogDao.kt', 'r') as f:
    content = f.read()

content = content.replace("status = 'SUCCESS'", "status IN ('SUCCESS', 'CALL_FORWARDED')")

with open('app/src/main/java/com/example/data/SmsLogDao.kt', 'w') as f:
    f.write(content)
