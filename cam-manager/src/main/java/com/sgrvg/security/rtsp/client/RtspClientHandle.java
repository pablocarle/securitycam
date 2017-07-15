package com.sgrvg.security.rtsp.client;

/**
 * Representa una conexion a un servidor Rtsp
 * 
 * @author pabloc
 *
 */
public interface RtspClientHandle {

	void teardown();

	RtspClientHandle addListener(Object object);

	void onDisconnect(Object object);

}
