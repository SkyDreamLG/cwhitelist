This mod can fix the official whitelist system that fails in Minecraft offline servers.

This mod establishes an independent whitelist system that can determine whether a user is allowed to access the server based on any of their IP/username/UUID.

Before you use this mod, please make sure that the server's whitelist has been disabled.

You can find this in server.properties, and turn white-list into false.

If your server is in the online mod and don't need to use IP or username into whitelist, you should use official whitelist.

If you turn the Minecraft server into offline mod, your server's whileline maybe invalidation because the server can not get right UUID from mojang.

This mod is a way to solve this problem.

This mod set up another whitelist , which can check user's name/UUID/IP. If one of this condition can be found in the whitelist, this user will be allowed to access the server, otherwise they will be deny.

This mod can also record the user who and when try to access the server. their IP address and UUID will also record in the log file.

The log file will be cut down every day. And you can set how long the log file save.

You can use this code to add user to the whitelist

/cwhitelist add <type> <volum>

the <type> can be name/uuid/ip, and the <volum> is the user's info

You can use this code to remove user to the whitelist

/cwhitelist remove <type> <volum>

the <type> can be name/uuid/ip, and the <volum> is the user's info

You can use this code to list the whitelist

/cwhitelist list

You can use this code to reload the whitelist

/cwhitelist reload

GUI configuration tool please visit https://github.com/SkyDreamLG/cwhitelist_configuration_GUI
tips: if you change the whitelist list, you must run /cwhitelist reload in your server to reload the whitelist.
