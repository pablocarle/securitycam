package com.sgrvg.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sgrvg.security.guice.ApplicationModule;
import com.sgrvg.security.rtsp.RtspServerDefinition;
import com.sgrvg.security.rtsp.client.RtspClient;
import com.sgrvg.security.rtsp.client.RtspClientHandle;
import com.sgrvg.security.rtsp.client.RtspClientInitializer;

public class RtspClientMain {

	private static List<RtspServerDefinition> servers;

	static {
		Properties props = new Properties();
		try {
			props.load(RtspClientMain.class.getClassLoader().getResourceAsStream("rtsp_servers.properties"));
			loadServers(props);
		} catch (IOException e) {
			System.err.println("Couldn't find resource");
			e.printStackTrace();
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
		System.out.println("Starting RTSP Client");
		
		Injector injector = Guice.createInjector(new ApplicationModule());
		
		RtspClientInitializer initializer = injector.getInstance(RtspClient.class);
		List<RtspClientHandle> handles = new ArrayList<>();
		servers.stream().forEach(server -> {
			handles.add(initializer.initialize(server));
		});
	}

	private static void loadServers(final Properties props) {
		System.out.println("Load Servers");
		servers = Arrays.stream(props.getProperty("server_names").split(","))
			  .filter(x -> x != null && x.trim().length() > 0)
		      .map(serverName -> {
		    	  String host = props.getProperty(serverName + "_ip");
		    	  int port = Integer.valueOf(props.getProperty(serverName + "_port"));
		    	  String endpoint = props.getProperty(serverName + "_path");
		    	  return new RtspServerDefinition(host, port, endpoint);
		      }).collect(Collectors.toList());
	}
}
