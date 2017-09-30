package com.alibaba.druid.support.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.servlet.ServletException;

import org.springframework.util.StringUtils;

import com.alibaba.druid.stat.DruidStatService;
import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import com.alibaba.fastjson.JSONObject;

public class StatViewServlet extends ResourceServlet{
	
	public StatViewServlet(){
        super("support/http/resources");
    }

	private final static Log      LOG                     = LogFactory.getLog(StatViewServlet.class);

    private static final long     serialVersionUID        = 1L;

    public static final String    PARAM_NAME_RESET_ENABLE = "resetEnable";

    public static final String    PARAM_NAME_JMX_URL      = "jmxUrl";
    public static final String    PARAM_NAME_JMX_USERNAME = "jmxUsername";
    public static final String    PARAM_NAME_JMX_PASSWORD = "jmxPassword";
    public static final String    PARAM_NAME_JMX_URLS      = "jmxUrls";
    public static final String    PARAM_NAME_JMX_USERNAMES = "jmxUsernames";
    public static final String    PARAM_NAME_JMX_PASSWORDS = "jmxPasswords";
    public static final String    PARAM_NAME_ONE_JMX_KEY= "oneJmxKey";

    private DruidStatService      statService             = DruidStatService.getInstance();

    /** web.xml中配置的jmx的连接地址 */
    private String                jmxUrl                  = null;
    /** web.xml中配置的jmx的用户名 */
    private String                jmxUsername             = null;
    /** web.xml中配置的jmx的密码 */
    private String                jmxPassword             = null;
    private MBeanServerConnection conn                    = null;
    private Map<String,MBeanServerConnection> connMap     = null;
    private Map<String,String> urlMap                     = null;
    private Map<String,String> nameMap                    = null;
    private Map<String,String> passwordMap                = null;
    
    
    public void init() throws ServletException {
        super.init();

        try {
            String param = getInitParameter(PARAM_NAME_RESET_ENABLE);
            if (param != null && param.trim().length() != 0) {
                param = param.trim();
                boolean resetEnable = Boolean.parseBoolean(param);
                statService.setResetEnable(resetEnable);
            }
        } catch (Exception e) {
            String msg = "initParameter config error, resetEnable : " + getInitParameter(PARAM_NAME_RESET_ENABLE);
            LOG.error(msg, e);
        }

        //获取jmx的连接信息(支持配置多个连接)
        String jmxUrls = readInitParam(PARAM_NAME_JMX_URLS);
        urlMap = JSONObject.parseObject(jmxUrls,Map.class);
        String userNames = readInitParam(PARAM_NAME_JMX_USERNAMES);
    	nameMap = StringUtils.isEmpty(userNames) ? null : JSONObject.parseObject( userNames,Map.class);
    	String passwords = readInitParam(PARAM_NAME_JMX_PASSWORDS);
    	passwordMap = StringUtils.isEmpty(userNames) ? null : JSONObject.parseObject( passwords,Map.class);
        
    	String paramJmxUrl = readInitParam(PARAM_NAME_JMX_URL);
    	String paramJmxUsername = readInitParam(PARAM_NAME_JMX_USERNAME);
    	String paramJmxPassword = readInitParam(PARAM_NAME_JMX_PASSWORD);
        //把配置的单个连接放进MAp中
    	if(paramJmxUrl != null) {
    		urlMap.put(PARAM_NAME_ONE_JMX_KEY, paramJmxUrl);
    		nameMap.put(PARAM_NAME_ONE_JMX_KEY, paramJmxUsername);
    		passwordMap.put(PARAM_NAME_ONE_JMX_KEY, paramJmxPassword);
    	}
        
        if(urlMap != null) {
    		urlMap.forEach((key,value) ->{
    			jmxUrl = value;
                jmxUsername = nameMap.get(key);
                jmxPassword = passwordMap.get(key);
                try {
                    initJmxConn();
                } catch (IOException e) {
                    LOG.error("init jmxs connection error", e);
                }
    		});
        }
    }
    
    /**
     * 读取servlet中的配置参数.
     * 
     * @param key 配置参数名
     * @return 配置参数值，如果不存在当前配置参数，或者为配置参数长度为0，将返回null
     */
    private String readInitParam(String key) {
        String value = null;
        try {
            String param = getInitParameter(key);
            if (param != null) {
                param = param.trim();
                if (param.length() > 0) {
                    value = param;
                }
            }
        } catch (Exception e) {
            String msg = "initParameter config [" + key + "] error";
            LOG.warn(msg, e);
        }
        return value;
    }
    
    
	/**
     * 初始化jmx连接
     * 
     * @throws IOException
     */
    private void initJmxConn() throws IOException {
        if (jmxUrl != null) {
            JMXServiceURL url = new JMXServiceURL(jmxUrl);
            Map<String, String[]> env = null;
            if (jmxUsername != null) {
                env = new HashMap<String, String[]>();
                String[] credentials = new String[] { jmxUsername, jmxPassword };
                env.put(JMXConnector.CREDENTIALS, credentials);
            }
            JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
            conn = jmxc.getMBeanServerConnection();
            if(connMap == null) {
            	connMap = new HashMap<String, MBeanServerConnection>();
            }
            connMap.put(jmxUrl, conn);
        }
    }
    
    /**
     * 初始化jmx连接
     * 
     * @throws IOException
     */
    private void initJmxConn(String key) throws IOException {
    	jmxUrl = urlMap.get(key);
    	jmxUsername = nameMap.get(key);
    	jmxPassword = passwordMap.get(key);
    	if(jmxUrl != null) {
    		JMXServiceURL url = new JMXServiceURL(jmxUrl);
            Map<String, String[]> env = null;
            if (jmxUsername != null) {
                env = new HashMap<String, String[]>();
                String[] credentials = new String[] { jmxUsername, jmxPassword };
                env.put(JMXConnector.CREDENTIALS, credentials);
            }
            JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
            conn = jmxc.getMBeanServerConnection();
            if(connMap == null) {
            	connMap = new HashMap<String, MBeanServerConnection>();
            }
            connMap.put(jmxUrl, conn);
    	}
    }
    
    /**
     * 根据指定的url来获取jmx服务返回的内容.
     * 
     * @param connetion jmx连接
     * @param url url内容
     * @return the jmx返回的内容
     * @throws Exception the exception
     */
    private String getJmxResult(MBeanServerConnection connetion, String url) throws Exception {
        ObjectName name = new ObjectName(DruidStatService.MBEAN_NAME);
        String result = (String) conn.invoke(name, "service", new String[] { url }, new String[] { String.class.getName() });
        return result;
    }
    
    String errorResp = null;
    /**
     * 程序首先判断是否存在jmx连接地址，如果不存在，则直接调用本地的duird服务； 如果存在，则调用远程jmx服务。在进行jmx通信，首先判断一下jmx连接是否已经建立成功，如果已经
     * 建立成功，则直接进行通信，如果之前没有成功建立，则会尝试重新建立一遍。.
     * 
     * @param url 要连接的服务地址
     * @return 调用服务后返回的json字符串
     */
    protected synchronized String process(String url) {
    	String resp = null;
    	if(jmxUrl == null && urlMap == null) {
    		resp = statService.service(url);
    	}else if(urlMap != null) {
    		List<String> list = new ArrayList<String>();
    		urlMap.forEach((key,jmxurl)->{ 
    			String respStr = null;
    			if(jmxurl != null) {
    				conn = connMap != null ? connMap.get(jmxurl) :null;
    				if (conn == null) {// 连接在初始化时创建失败
                        try {// 尝试重新连接
                            initJmxConn(key);
                        } catch (IOException e) {
                            LOG.error("init jmx connection error", e);
                            errorResp = DruidStatService.returnJSONResult(DruidStatService.RESULT_CODE_ERROR, "init jmx connection error" + e.getMessage());
                        }
                        if (conn != null) {// 连接成功
                            try {
                            	respStr = getJmxResult(conn, url);
//                            	LOG.info("jmxUrl:"+ key + ";respStr:"+respStr);
                            } catch (Exception e) {
                                LOG.error("get jmx data error", e);
                                errorResp = DruidStatService.returnJSONResult(DruidStatService.RESULT_CODE_ERROR, "get data error:" + e.getMessage());
                            }
                        }
                    } else {// 连接成功
                        try {
                        	respStr = getJmxResult(conn, url);
//                        	LOG.info("jmxUrl:"+ key + ";respStr:"+respStr);
                        } catch (Exception e) {
                        	if(respStr == null) {
                        		if(connMap == null) connMap = new HashMap<String ,MBeanServerConnection>();
                        		connMap.put(jmxurl, null);
                        	}
                            LOG.error("get jmx data error", e);
                            errorResp = DruidStatService.returnJSONResult(DruidStatService.RESULT_CODE_ERROR, "get data error" + e.getMessage());
                        }
                    }
    				
    				if(respStr != null) {
	            		if(url.startsWith("/sql.json")) {
	                		Map<String , Object> map = JSONObject.parseObject(respStr, Map.class);
	                		if(map.get("Content") != null) {
	                			List sublist = (List) map.get("Content");
	                    		list.addAll(sublist);
	                		}
	            		}else if(url.startsWith("/sql-")){
	            			JSONObject object = JSONObject.parseObject(respStr);
	            			String content = object.getString("Content");
	            			if(content != null) list.add(content);
	            		}else {
	            			JSONObject object = JSONObject.parseObject(respStr);
	            			String content = object.getString("Content");
	            			list.add(content);
	            		}
    				}
    			}
    		});
    		
    		if(list.size() >0) {
    			if(url.startsWith("/sql.json")) {
        			resp = "{\"ResultCode\":1,\"Content\":"+ list.toString() +"}";
            		LOG.info("returnString:"+resp);
        		}else if(url.startsWith("/sql-")){
        			resp = "{\"ResultCode\":1,\"Content\":"+ list.get(0) +"}";
            		LOG.info("returnString:"+resp);
        		}else {
        			resp = "{\"ResultCode\":1,\"Content\":"+ list.get(0) +"}";
            		LOG.info("returnString:"+resp);
        		}	
    		}
    			
        	if(StringUtils.isEmpty(list.isEmpty())) {
        		resp = errorResp;
        	}
    	}
    	return resp;
    }
    
}
