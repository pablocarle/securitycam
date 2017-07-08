package com.sgrvg.security.rtsp.client;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Strings;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.util.AsciiString;

/**
 * TODO Deberia procesar SDP?
 * 
 * @author pabloc
 *
 */
public class DescribeState extends RtspHandshakeState {

	private static final String SPROP_PARAMETER_SETS = "sprop-parameter-sets";
	
	private String server;
	private String contentType;
	private Integer contentLength;
	private String date;
	private String contentBase;
	
	private String sdpContent;
	
	private byte[] sequenceParameterSet;
	private byte[] pictureParameterSet;
	
	public DescribeState(URI uri, int sequence, HttpResponse message) {
		super(uri, sequence, message);
		server = message.headers().get(RtspHeaderNames.SERVER);
		contentType = message.headers().get(RtspHeaderNames.CONTENT_TYPE);
		contentLength = message.headers().getInt(RtspHeaderNames.CONTENT_LENGTH);
		date = message.headers().get(RtspHeaderNames.DATE);
		contentBase = message.headers().get(RtspHeaderNames.CONTENT_BASE);
		if (message instanceof ByteBufHolder) {
			ByteBuf buf = ((ByteBufHolder) message).content();
			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			sdpContent = new String(data);
		}
		parseSDP();
	}

	private void parseSDP() {
		int index = sdpContent.indexOf("sprop-parameter-sets");
		if (index >= 0) {
			String parameterSets = sdpContent.substring(index + SPROP_PARAMETER_SETS.length() + 1, 
					sdpContent.indexOf('\n', index));
			if (!Strings.isNullOrEmpty(parameterSets)) {
				String[] params = parameterSets.split(",");
				if (params.length != 2) {
					throw new RuntimeException("Expected pps and sps");
				}
				sequenceParameterSet = Base64.getDecoder().decode(params[0].trim());
				pictureParameterSet = Base64.getDecoder().decode(params[1].trim());
			}
		}
	}

	@Override
	protected Map<AsciiString, Object> doGetState() {
		Map<AsciiString, Object> map = new HashMap<>();
		map.put(RtspHeaderNames.SERVER, server);
		map.put(RtspHeaderNames.CONTENT_TYPE, contentType);
		map.put(RtspHeaderNames.CONTENT_LENGTH, contentLength);
		map.put(RtspHeaderNames.DATE, date);
		map.put(RtspHeaderNames.CONTENT_BASE, contentBase);
		return Collections.unmodifiableMap(map);
	}
	
	public String getSDPContent() {
		return sdpContent;
	}
	
	public byte[] getSPS() {
		return sequenceParameterSet;
	}
	
	public byte[] getPPS() {
		return pictureParameterSet;
	}
}
