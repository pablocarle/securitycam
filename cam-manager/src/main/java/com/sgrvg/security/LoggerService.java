package com.sgrvg.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;

import com.google.inject.Inject;

/**
 * Tiene una cola, y en un thread aparte, va revisando cuando hay suficientes elementos de log para enviar (o cada determinado tiempo?)
 * 
 * @author pabloc
 *
 */
public class LoggerService implements SimpleLogger {

	private static final String SERVER_LOG_URL = "https://sgrvg-carle.rhcloud.com/security/log";
	private static final String USERNAME = "security";
	private static final String PASSWORD = "security123";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

	private final BlockingQueue<LogEntry> entries;
	private final ExecutorService executor;

	private enum Mode {
		SCHEDULED,
		QUEUE_SIZE
	}

	@Inject
	public LoggerService() {
		super();
	}

	{
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("");
		Properties props = new Properties();
		try {
			props.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
		entries = new LinkedBlockingDeque<>(Integer.valueOf(props.getProperty("max_log_queue", "1000")));
		executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			final ThreadFactory delegate = Executors.defaultThreadFactory();

			@Override
			public Thread newThread(Runnable r) {
				final Thread result = delegate.newThread(r);
				result.setName("LoggerService-" + result.getName());
				result.setDaemon(true);
				return result;
			}
		});
	}

	@Override
	public void error(String arg0, Object...args) {
		LogEntry log = new LogEntry(buildMessage(arg0, args), "ERROR", new Date(), null);
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
		LogEntry log = new LogEntry(buildMessage(arg0, args), "INFO", new Date(), null);
		System.out.println(log);
		entries.add(log);
	}

	@Override
	public void info(String arg0, Throwable arg1, Object...args) {
		LogEntry log = new LogEntry(buildMessage(arg0, args), "INFO", new Date(), arg1);
		System.out.println(log);
		entries.add(log);
	}

	@Override
	public boolean isErrorEnabled() {
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		return true;
	}

	@Override
	public boolean isWarnEnabled() {
		return true;
	}

	@Override
	public void warn(String arg0, Object...args) {
		LogEntry log = new LogEntry(buildMessage(arg0, args), "WARN", new Date(), null);
		System.out.println(log);
		entries.add(log);
	}

	@Override
	public void warn(String arg0, Throwable arg1, Object... args) {
		LogEntry log = new LogEntry(buildMessage(arg0, args), "WARN", new Date(), null);
		System.out.println(log);
		entries.add(log);
	}

	private String buildMessage(String messageWithMarkers, Object...args) {
		StringBuilder message = new StringBuilder(messageWithMarkers);
		if (args != null && args.length > 0) {
			Arrays.stream(args).forEach(x -> {
				//TODO 
			});
		}
		return messageWithMarkers;
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
			//TODO Debe verificar si esta autenticado por ejemplo contra el servidor.
			while (!executor.isShutdown()) {
				
			}
		}
		
		private void authenticate() {
			
		}
	}
}
