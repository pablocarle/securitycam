package com.sgrvg.security;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.inject.Inject;

/**
 * Tiene una cola, y en un thread aparte, va revisando cuando hay suficientes elementos de log para enviar (o cada determinado tiempo?)
 * 
 * @author pabloc
 *
 */
public class LoggerService implements SimpleLogger {

	private static final String SERVER_LOG_URL = "";
	private static final String USERNAME = "";
	private static final String PASSWORD = "";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

	private Queue<LogEntry> entries;
	private ThreadPoolExecutor executor;

	private enum Mode {
		SCHEDULED,
		QUEUE_SIZE
	}
	
	@Inject
	public LoggerService() {
		super();
	}

	@Override
	public void error(String arg0, Object...args) {
		LogEntry log = new LogEntry(buildMessage(arg0, args), "INFO", new Date(), null);
		System.out.println(log);
		entries.add(log);
	}

	@Override
	public void error(String arg0, Throwable arg1, Object...args) {
		LogEntry log = new LogEntry(buildMessage(arg0, args), "ERROR", new Date(), arg1);
		System.err.println(log);
		entries.add(log);
	}

	@Override
	public void info(String arg0, Object...args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(String arg0, Throwable arg1, Object...args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isErrorEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInfoEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isWarnEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void warn(String arg0, Object...args) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(String arg0, Throwable arg1, Object... args) {
		// TODO Auto-generated method stub
		
	}
	
	private String buildMessage(String messageWithMarkers, Object...args) {
		return "";
	}
	
	private class LogEntry implements Serializable {

		private static final long serialVersionUID = 1L;
		
		private String message;
		private String category;
		private Date timestamp;
		private Throwable e;
		
		public LogEntry(String message, String category, Date timestamp, Throwable e) {
			super();
			this.message = message;
			this.category = category;
			this.timestamp = timestamp;
			this.e = e;
		}

		public String getMessage() {
			return message;
		}

		public String getCategory() {
			return category;
		}

		public Date getTimestamp() {
			return timestamp;
		}
		
		@Override
		public String toString() {
			return getFullMessage();
		}
		
		public String getFullMessage() {
			StringBuilder message = new StringBuilder(sdf.format(timestamp));
			message.append(" - ");
			message.append(category);
			message.append(": ");
			message.append(message);

			if (e != null) {
				message.append(Arrays.toString(e.getStackTrace()));
			}
			return message.toString();
		}
	}
	
	private class LogSendTask implements Runnable {

		@Override
		public void run() {
			
		}
	}
}
