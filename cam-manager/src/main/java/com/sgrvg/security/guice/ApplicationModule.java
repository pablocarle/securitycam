package com.sgrvg.security.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.sgrvg.security.LoggerService;
import com.sgrvg.security.SimpleLogger;

public class ApplicationModule extends AbstractModule {

	@Override
	protected void configure() {
		// TODO Auto-generated method stub

	}
	
	@Provides
	@Singleton
	public SimpleLogger getLogger() {
		return new LoggerService();
	}
}
