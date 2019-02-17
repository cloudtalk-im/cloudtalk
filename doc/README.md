## 1、依赖

   CloudTalk需要 CentOs7.0 以上版本。推荐使用纯净的新系统进行安装。
   在安装前，推荐安装使用Bt.cn的宝塔服务器管理平台，管理安装所需要的数据库,网站等。安全,方便，专业.安装命令如下:

   yum install -y wget && wget -O install.sh http://download.bt.cn/install/install_6.0.sh && bash install.sh

    安装完宝塔面板后，请在后台管理里面，安装数据库 Mysql 5.6.x，Redis，Java 1.8以上版本等环境。

	服务端对pb,hiredis,mysql_client,log4cxx有依赖，所以服务端需要先安装pb，hiredis,mysql,log4cxx。
	在正式编译服务端之前，请先执行server/src目录下的：
	make_log4cxx.sh
	make_protobuf.sh

	这些脚本会先安装以上依赖。

	如果安装了宝塔面板，并且安装了mysql和redis库后，就不用运行下面的两个脚本，如果没有安装宝塔面板，你需要自行安装所需环境，并且运行下面两个脚本:
	make_hiredis.sh
    make_mariadb.sh
	
## 2、编译协议文件
	
	所有的协议文件在pb目录下，其中有create.sh以及sync.sh两个shell脚本。
	create.sh的作用是使用protoc将协议文件转化成相应语言的源码。
	sync.sh是将生成的源码拷贝到server的目录下。
	
## 3、编译服务端
	
	经历了编译服务端依赖，pb之后，就可以执行server目录下的build.sh脚本
	
## 4、部署说明




   官方技术交流QQ群:6445609