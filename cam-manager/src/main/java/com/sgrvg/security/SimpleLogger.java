package com.sgrvg.security;

/**
 * Simple logger interface, what else?
 * 
 * @author pabloc
 *
 */
public interface SimpleLogger {
	
	public void error(String arg0, Object...args);

	public void error(String arg0, Throwable arg1, Object...args);

	public void info(String arg0, Object...args);

	public void info(String arg0, Throwable arg1, Object...args);
	
	public void debug(String arg0, Object...args);

	public void debug(String arg0, Throwable arg1, Object...args);
	
	public void trace(String arg0, Object... args);
	
	public void trace(String arg0, Throwable arg1, Object...args);

	public boolean isErrorEnabled();

	public boolean isInfoEnabled();

	public boolean isWarnEnabled();
	
	public boolean isDebugEnabled();
	
	public boolean isTraceEnabled();

	public void warn(String arg0, Object...args);

	public void warn(String arg0, Throwable arg1, Object... args);
	
}
