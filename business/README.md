# cloudtalk

CloudTalk的业务模块采用Java开发。项目地址 https://github.com/cloudtalk-im/cloudtalk-websocket

本项目基于 SpringBoot + MyBatis-plus 框架,采用 maven 构建。下载源代码后。运行 Maven install 即可在 target/distribution  下生成打包文件，采用的是基于库,配置文件，主程序分离打包，方便配置与运行。运行startup.sh 即可启动
注意：在启动前，请配置 application.properties 与  application-local.properties 等配置文件，配置端口，mysql数据库地址与密码，redis地址，还有CloudTalk主程序的 http api 服务器地址。