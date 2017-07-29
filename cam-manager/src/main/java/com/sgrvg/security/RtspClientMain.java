package com.sgrvg.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sgrvg.security.guice.ApplicationModule;
import com.sgrvg.security.rtp.server.RTPServerHandle;
import com.sgrvg.security.rtp.server.RTPServerInitializationException;
import com.sgrvg.security.rtp.server.RTPServerInitializer;
import com.sgrvg.security.rtsp.RtspServerDefinition;
import com.sgrvg.security.rtsp.client.RtspClientHandle;
import com.sgrvg.security.rtsp.client.RtspClientInitializer;

/**
 * Entry point
 * 
 * @author pabloc
 *
 */
public class RtspClientMain {

	private static SimpleLogger logger;
	
	private static List<RtspServerDefinition> servers;
	private static List<RTPServerHandle> rtpServerInstances;
	private static List<RtspClientHandle> rtspClientInstances;
	
	private static Injector injector; 
	
	static {
		System.setProperty("org.bytedeco.javacpp.maxphysicalbytes", "0"); 
		System.setProperty("org.bytedeco.javacpp.maxbytes", "0");

		Properties props = new Properties();
		InputStream is = null;
		try {
			is = RtspClientMain.class.getClassLoader().getResourceAsStream("rtsp_servers.properties");
			if (is == null) {
				is = new FileInputStream(new File("conf/rtsp_servers.properties"));
			}
			props.load(is);
			injector = Guice.createInjector(new ApplicationModule());
			logger = injector.getInstance(SimpleLogger.class);
			loadServers(props);
		} catch (IOException e) {
			System.err.println("Couldn't find resource");
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					System.err.println("Failed while closing properties resource");
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Inicializa los clientes Rtsp (handshake para que empiecen a 
	 * transmitir.
	 * 
	 * Luego inicializa los sevidores y mantiene los handles de todos
	 * para administrar logging y status para reconexiones por ejemplo.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("Starting Services");
		
		rtpServerInstances = new ArrayList<>();
		rtspClientInstances = new ArrayList<>();
		
		servers.stream().forEach(RtspClientMain::initialize);
		if (rtspClientInstances == null || rtspClientInstances.isEmpty()) {
			System.exit(1);
		}
	}
	
	private static void initialize(RtspServerDefinition serverDefinition) {
		RtspClientInitializer rtspClientInitializer = injector.getInstance(RtspClientInitializer.class);
		RTPServerInitializer rtpServerInitializer = injector.getInstance(RTPServerInitializer.class);
		try {
			RTPServerHandle rtpServer = rtpServerInitializer.initialize(serverDefinition);
			rtpServer.waitConnected();
			rtpServerInstances.add(rtpServer);
			RtspClientHandle rtspClient = rtspClientInitializer.initialize(serverDefinition, rtpServer); 
			rtspClientInstances.add(rtspClient);
		} catch (InterruptedException | RTPServerInitializationException e) {
			logger.error("Failed to initialize server {}", e, serverDefinition);
		}
	}
	
	private static void loadServers(final Properties props) {
		logger.info("Load Servers");
		servers = Arrays.stream(props.getProperty("server_names").split(","))
			  .filter(x -> x != null && x.trim().length() > 0)
		      .map(serverName -> {
		    	  return new RtspServerDefinition(serverName, props);
		      }).collect(Collectors.toList());
	}
}
