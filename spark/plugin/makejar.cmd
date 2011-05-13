"C:\Program Files\Java\jdk1.6.0_01\bin\jar" cvf redfire-plugin.jar .

del "D:\Documents and Settings\803292695\Application Data\Spark\plugins\redfire-plugin.jar"
rd "D:\Documents and Settings\803292695\Application Data\Spark\plugins\redfire-plugin" /q /s
copy redfire-plugin.jar "C:\Program Files\Spark\plugins\redfire-plugin.jar"

del redfire-plugin.jar

pause