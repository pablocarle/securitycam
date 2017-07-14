package com.sgrvg.security.util;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.sgrvg.security.ServerConfigHolder;
import com.sgrvg.security.rtp.server.RTPPacketHandler;
import com.sgrvg.security.rtp.server.RTPServerHandle;
import com.sgrvg.security.rtsp.RtspServerDefinition;

public class ServerConfigHolderImpl implements ServerConfigHolder {

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
}
