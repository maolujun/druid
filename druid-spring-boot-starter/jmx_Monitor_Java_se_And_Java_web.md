#jmx远程监控java se、java web配置
##java se 项目配置
增加java运行参数 
    
     -Dcom.sun.management.jmxremote 
     #jmx监控地址  
	-Djava.rmi.server.hostname=127.0.0.1   
	#jmx监控端口号  
	-Dcom.sun.management.jmxremote.port=9004    
	#为false时表示不需要密码  
	-Dcom.sun.management.jmxremote.authenticate=true  
	-Dcom.sun.management.jmxremote.ssl=false  
	#权限文件的绝对路径 不配置默认路径为jdk1.8.0_60\jre\lib\management  
	-Dcom.sun.management.jmxremote.access.file="D:/Program Files/Java/jdk1.8.0_60/jre/lib/management/jmxremote.access"  
		#账号权限配置  
		monitorRole   readonly  
		controlRole   readwrite   
	#密码文件的绝对路径 不配置默认路径为jdk1.8.0_60\jre\lib\management 
	-Dcom.sun.management.jmxremote.password.file="D:/Program Files/Java/jdk1.8.0_60/jre/lib/management/jmxremote.password" 
		#账号、密码配置 
		monitorRole   123 
		controlRole   123 
		
注意：jmxremote.access文件和jmxremote.password文件需要设置权限 设置拥有者可读写，其他人不可读写执行

	chmod 600 jmxremote.access
	chmod 600 jmxremote.password 


##java web项目配置
1. 增加监听类 RegisterMBeanListener.java
	
	package com.tontisa.druid.listener;
	import javax.servlet.ServletContextEvent;
	import javax.servlet.ServletContextListener;
	import com.alibaba.druid.stat.DruidStatService;
	public class RegisterMBeanListener implements ServletContextListener  {
		public void contextInitialized(ServletContextEvent sce) {
			try {
	        	DruidStatService.registerMBean();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		public void contextDestroyed(ServletContextEvent sce) {
			// TODO Auto-generated method stub
		}
	}

2. web.xml配置  
	
	<filter>
		<filter-name>DruidWebStatFilter</filter-name>
		<filter-class>com.alibaba.druid.support.http.WebStatFilter</filter-class>
		<init-param>
			<!-- 经常需要排除一些不必要的url，比如.js,/jslib/等等。配置在init-param中 -->
			<param-name>exclusions</param-name>
			<param-value>*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*</param-value>
		</init-param>
		<!-- 缺省sessionStatMaxCount是1000个。你可以按需要进行配置 -->
		<init-param>
			<param-name>sessionStatMaxCount</param-name>
			<param-value>1000</param-value>
		</init-param>
		<!-- druid 0.2.7版本开始支持profile，配置profileEnable能够监控单个url调用的sql列表 -->
		<init-param>
			<param-name>profileEnable</param-name>
			<param-value>true</param-value>
		</init-param>
		<init-param>
			<param-name>principalSessionName</param-name>
			<param-value>users.username</param-value>
		</init-param>
		<!-- 你可以关闭session统计功能 -->
		<init-param> 
			<param-name>sessionStatEnable</param-name> 
			<param-value>true</param-value>
		</init-param>
	</filter>
	<filter-mapping>
        <filter-name>DruidWebStatFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
	<listener>
		<listener-class>com.tontisa.druid.listener.RegisterMBeanListener</listener-class>
	</listener>

3. tomcat 参数修改  
apache-tomcat-8.0.39\bin\catalina.bat
	
	setlocal后面增加参数   
	set CATALINA_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9007 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=127.0.0.1  
	权限文件路径  文件配置与java相同
	apache-tomcat-8.0.39\conf\jmxremote.access  
	apache-tomcat-8.0.39\conf\jmxremote.password  

