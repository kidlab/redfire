call ant war >build.txt

rd "D:\Program Files\Openfire\plugins\redfire" /q /s
del "D:\Program Files\Openfire\plugins\redfire.war"
copy "D:\Work\Projects\2010.04.21-iTrader\Workspace\openfire_3_6_4\target\openfire\plugins\redfire.war" "D:\Program Files\Openfire\plugins"

del "D:\Program Files\Openfire\logs\*.*"

pause