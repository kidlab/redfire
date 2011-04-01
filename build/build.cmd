call ant war >build.txt

rd "C:\Program Files\Openfire\plugins\redfire" /q /s
del "C:\Program Files\Openfire\plugins\redfire.war"
copy "D:\Work\Projects\2010.04.21-iTrader\Workspace\openfire_3_6_4\target\openfire\plugins\redfire.war" "C:\Program Files\Openfire\plugins"

del "C:\Program Files\Openfire\logs\*.*"

pause