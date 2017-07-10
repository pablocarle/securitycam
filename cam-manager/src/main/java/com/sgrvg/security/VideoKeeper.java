package com.sgrvg.security;

import io.netty.buffer.ByteBuf;

public interface VideoKeeper {

	void keep(long startTimestamp, long endTimestamp, ByteBuf video);

}
