## 1、依赖

	服务端对pb,hiredis,mysql_client,log4cxx有依赖，所以服务端需要先安装pb，hiredis,mysql,log4cxx。
	在正式编译服务端之前，请先执行server/src目录下的：
	make_hiredis.sh
	make_log4cxx.sh
	make_mariadb.sh
	make_protobuf.sh
	这些脚本会先安装以上依赖。
	
## 2、编译协议文件
	
	所有的协议文件在pb目录下，其中有create.sh以及sync.sh两个shell脚本。
	create.sh的作用是使用protoc将协议文件转化成相应语言的源码。
	sync.sh是将生成的源码拷贝到server的目录下。
	
## 3、编译服务端
	
	经历了编译服务端依赖，pb之后，就可以执行server目录下的build.sh脚本
	
## 4、部署说明
	