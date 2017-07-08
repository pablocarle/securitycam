package com.sgrvg.security.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sgrvg.security.LoggerService;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.ServerConfigHolderImpl;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.rtp.server.RTPServer;
import com.sgrvg.security.rtp.server.RTPServerInitializer;
import com.sgrvg.security.rtsp.client.RtspClient;
import com.sgrvg.security.rtsp.client.RtspClientInitializer;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class ApplicationModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(RtspClientInitializer.class).to(RtspClient.class);
		bind(RTPServerInitializer.class).to(RTPServer.class);
		
		bind(ServerConfigHolder.class).to(ServerConfigHolderImpl.class).asEagerSingleton();
	}
	
	@Provides
	@Singleton
	public SimpleLogger getLogger() {
		return new LoggerService();
	}

	@Provides
	@Named("default_worker_group")
	@Singleton
	public EventLoopGroup defaultEventLoop() {
		return new NioEventLoopGroup();
	}
	
	@Provides
	@Named("rtp_server_worker")
	@Singleton
	public EventLoopGroup rtpServerWorkerLoopGroup() {
		return new NioEventLoopGroup();
	}
	
	@Provides
	@Named("rtp_server_boss")
	@Singleton
	public EventLoopGroup rtpServerBossLoopGroup() {
		return new NioEventLoopGroup();
	}
}
