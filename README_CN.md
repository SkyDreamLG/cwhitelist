在使用此mod之前，请确保服务器的白名单已被禁用。
您可以在Server.Properties中找到它，然后将white-list设置为false。
如果您的服务器位于在线模式中，并且不需要将IP或用户名用于白名单，则应使用官方白名单系统。
如果将Minecraft Server变成离线模式，则官方的白名单系统可能是无效的，因为服务器无法从Mojang获得正确的UUID。
此mod是解决此问题的一种方法。
此mod设置了另一个白名单，可以检查用户的name/uuid/ip。如果可以在白名单中找到此条件之一，则将允许该用户访问服务器，否则将被拒绝。
此mod还可以记录用户以及何时尝试访问服务器。他们的IP地址和UUID也将在日志文件中记录。
日志文件每天都会自动分割。您可以设置日志文件保存多长时间。
您可以使用此代码将用户添加到白名单中
/cwhitelist add <type> <volum>
<type>可以是name/uuid/ip，而<volum>是用户的信息
您可以使用此代码将用户移除白名单
/cwhitelist remove <type> <volum>
<type>可以是name/uuid/ip，而<volum>是用户的信息
您可以使用此代码列出白名单
/cwhitelist list
您可以使用此代码重新加载白名单
/cwhitelist reload
