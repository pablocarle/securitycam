package com.sgrvg.security.util;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Inject;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.rtp.server.RTPPacketHandler;
import com.sgrvg.security.rtp.server.RTPServerDefinition;
import com.sgrvg.security.rtp.server.RTPServerHandle;
import com.sgrvg.security.rtsp.RtspServerDefinition;

public class ServerConfigHolderImpl implements ServerConfigHolder {

	private AtomicInteger port = new AtomicInteger(35678);
	private Map<RTPServerHandle, ServerComponent> map = new ConcurrentHashMap<>();
	
	@Inject
	public ServerConfigHolderImpl() {
		super();
	}
	
	@Override
	public void bind(RTPServerHandle rtpServer, RtspServerDefinition rtspServer) {
		if (map.containsKey(rtpServer)) {
			ServerComponent component = map.get(rtpServer);
			if (component.rtspEndpoint != null) {
				throw new IllegalStateException("Cannot re-bind rpt server to rtsp endpoint");
			}
			component.rtspEndpoint = rtspServer;
		} else {
			ServerComponent component = new ServerComponent();
			component.rtspEndpoint = rtspServer;
			map.put(rtpServer, component);
		}
	}

	@Override
	public void bind(RTPServerHandle rtpServer, RTPPacketHandler rtpPacketHandler) {
		if (map.containsKey(rtpServer)) {
			ServerComponent component = map.get(rtpServer);
			if (component.handler != null) {
				throw new IllegalStateException("Cannot re-bind rtp server to rtp handler");
			}
			component.handler = rtpPacketHandler;
		} else {
			ServerComponent component = new ServerComponent();
			component.handler = rtpPacketHandler;
			map.put(rtpServer, component);
		}
	}
	
	static class ServerComponent {
		
		RTPPacketHandler handler;
		RtspServerDefinition rtspEndpoint;
		
		ServerComponent() {
			super();
		}
		
		ServerComponent(RTPPacketHandler handler, RtspServerDefinition rtspEndpoint) {
			super();
			this.handler = handler;
			this.rtspEndpoint = rtspEndpoint;
		}
		
		boolean contains(Object o) {
			if (o instanceof RTPPacketHandler) {
				return handler != null && handler.equals(o);
			}
			if (o instanceof RtspServerDefinition) {
				return rtspEndpoint != null && rtspEndpoint.equals(o);
			}
			return false;
		}
	}

	@Override
	public Optional<RtspServerDefinition> getRtspEndpoint(RTPPacketHandler rtpPacketHandler) {
		return map.entrySet()
				.stream()
				.filter(entry -> entry.getValue().contains(rtpPacketHandler))
				.map(entry -> entry.getValue().rtspEndpoint)
				.findFirst();
	}

	@Override
	public Optional<RtspServerDefinition> getRtspEndpoint(RTPServerDefinition rtpServer) {
		return map.entrySet()
				.stream()
				.filter(entry -> {
					try {
						return entry.getKey().serverDefinition().equals(rtpServer);
					} catch (URISyntaxException e) {
						return false;
					}
				})
				.map(entry -> entry.getValue().rtspEndpoint)
				.findFirst();
	}

	@Override
	public int getNextPortInRange() {
		return port.incrementAndGet();
	}
}
