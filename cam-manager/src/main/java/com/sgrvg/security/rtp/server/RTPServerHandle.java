package com.sgrvg.security.rtp.server;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Optional;

public interface RTPServerHandle {

	/**
	 * Get an unique id among RTPServers
	 * 
	 * @return
	 */
	String getID();
	
	/**
	 * Get remote server information
	 * 
	 * @return
	 * @throws URISyntaxException
	 */
	RTPServerDefinition serverDefinition() throws URISyntaxException;
	
	/**
	 * Sync waits for the server to be bound.
	 * 
	 * @throws InterruptedException
	 * @throws RTPServerInitializationException
	 */
	void waitConnected() throws InterruptedException, RTPServerInitializationException;

	/**
	 * Is the RTP Server receiving information.
	 * Consider RTP Server could be still initializing
	 * 
	 * @return
	 */
	boolean receiving();

	/**
	 * Get an optional time indicating the time on which the last packet was received
	 * by this server.
	 * 
	 * @return
	 */
	Optional<Instant> getLastReceivedPacket();

	/**
	 * Perform a clean shutdown / restart of services
	 */
	void shutdown();
	
}
