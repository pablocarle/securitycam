package com.sgrvg.security.guice;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.SimpleLogger;
import com.sgrvg.security.VideoKeeper;
import com.sgrvg.security.h264.FrameBuilder;
import com.sgrvg.security.h264.H264FU_AFrameBuilder;
import com.sgrvg.security.recording.DriveVideoKeeper;
import com.sgrvg.security.recording.DropboxVideoKeeper;
import com.sgrvg.security.recording.LocalFileVideoKeeper;
import com.sgrvg.security.rtp.server.RTPPacketHandler;
import com.sgrvg.security.rtp.server.RTPServer;
import com.sgrvg.security.rtp.server.RTPServerInitializer;
import com.sgrvg.security.rtsp.client.RtspClient;
import com.sgrvg.security.rtsp.client.RtspClientInitializer;
import com.sgrvg.security.rtsp.client.RtspHandshakeOperation;
import com.sgrvg.security.util.LoggerService;
import com.sgrvg.security.util.ServerConfigHolderImpl;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import net.spy.memcached.MemcachedClient;

public class ApplicationModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ServerConfigHolder.class).to(ServerConfigHolderImpl.class).asEagerSingleton();
		bind(SimpleLogger.class).to(LoggerService.class).asEagerSingleton();
		bind(FrameBuilder.class).to(H264FU_AFrameBuilder.class).asEagerSingleton();
	}
	
	@Provides
	public RTPPacketHandler getNewRTPPacketHandler(
			SimpleLogger logger,
			FrameBuilder frameBuilder,
			ServerConfigHolder serverConfig,
			@Named("drive_keeper") VideoKeeper driveVideoKeeper,
			@Named("file_keeper") VideoKeeper localFileVideoKeeper,
			@Named("dropbox_keeper") VideoKeeper dropboxKeeper,
			ByteBufAllocator byteBufAllocator
			) {
		return new RTPPacketHandler(logger, frameBuilder, serverConfig, driveVideoKeeper, localFileVideoKeeper, dropboxKeeper, byteBufAllocator);
	}
	
	@Provides
	public RTPServerInitializer getRTPServerInitializer(
			SimpleLogger logger,
			@Named("rtp_server_boss") EventLoopGroup bossLoopGroup,
			RTPPacketHandler packetHandler,
			ServerConfigHolder serverConfig
			) {
		return new RTPServer(logger, bossLoopGroup, serverConfig, packetHandler);
	}
	
	@Provides
	public RtspClientInitializer getRtspClientInitializer(SimpleLogger logger, @Named("default_worker_group") EventLoopGroup workerGroup, 
			ServerConfigHolder serverConfig, RtspHandshakeOperation operation) {
		return new RtspClient(logger, workerGroup, serverConfig, operation);
	}
	
	@Provides
	public RtspHandshakeOperation getHandshakeOperation(SimpleLogger logger, ServerConfigHolder serverConfig) {
		return new RtspHandshakeOperation(logger, serverConfig);
	}
	
	@Provides
	@Named("drive_keeper")
	@Singleton
	public VideoKeeper getDriveVideoKeeper(MemcachedClient memcachedClient, ByteBufAllocator byteBufAllocator, SimpleLogger logger) {
		return new DriveVideoKeeper(memcachedClient, logger, byteBufAllocator, true, 500000);
	}
	
	@Provides
	@Named("file_keeper")
	@Singleton
	public VideoKeeper getLocalFileVideoKeeper(MemcachedClient memcachedClient, ByteBufAllocator byteBufAllocator, SimpleLogger logger) {
		return new LocalFileVideoKeeper(memcachedClient, logger, byteBufAllocator, true, 1000000);
	}
	
	@Provides
	@Named("dropbox_keeper")
	@Singleton
	public VideoKeeper getDropboxVideoKeeper(MemcachedClient memcachedClient, ByteBufAllocator byteBufAllocator, SimpleLogger logger) {
		return new DropboxVideoKeeper(memcachedClient, logger, byteBufAllocator, true, 500000);
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
	
	@Provides
	@Singleton
	public MemcachedClient getMemcachedClient() throws IOException {
		return new MemcachedClient(new InetSocketAddress(11211));
	}
	
	@Provides
	@Singleton
	public ByteBufAllocator getDefaultByteBufAllocator() {
		return PooledByteBufAllocator.DEFAULT;
	}
}
