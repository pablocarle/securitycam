package com.sgrvg.security.rtp.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageAggregator;

/**
 * TODO Definir estructura de paquetes RTP y lo que terminan generando, este seria el ultimo paso para 
 * obtener un Frame y/o nal unit, me queda por ver las definiciones correctas al respecto y definir que hago.
 * 
 * @author pabloc
 *
 */
public class NalRtpPacketAggregator extends MessageAggregator<Object, Object, ByteBufHolder, ByteBufHolder> {

	protected NalRtpPacketAggregator(int maxContentLength) {
		super(maxContentLength);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean isStartMessage(Object msg) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isContentMessage(Object msg) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isLastContentMessage(ByteBufHolder msg) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isAggregated(Object msg) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isContentLengthInvalid(Object start, int maxContentLength) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Object newContinueResponse(Object start, int maxContentLength, ChannelPipeline pipeline)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean closeAfterContinueResponse(Object msg) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean ignoreContentAfterContinueResponse(Object msg) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected ByteBufHolder beginAggregation(Object start, ByteBuf content) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
