package com.sgrvg.security.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.asynchttpclient.cookie.Cookie;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;

/**
 * Tiene una cola, y en un thread aparte, va revisando cuando hay suficientes elementos de log para enviar (o cada determinado tiempo?)
 * 
 * @author pabloc
 *
 */
public final class LoggerService implements SimpleLogger {

	private enum Category {
		DEBUG, INFO, WARN, ERROR;
		
		@SuppressWarnings("unused")
		static Optional<Category> parse(String category) {
			if (Strings.isNullOrEmpty(category)) {
				throw new IllegalArgumentException("Category must be");
			}
			for (Category c : values()) {
				if (c.name().equalsIgnoreCase(category)) {
					return Optional.of(c);
				}
			}
			return Optional.empty();
		}
	}
	
	private static final String SERVER_LOG_URL = "https://sgrvg-carle.rhcloud.com/service/logging";
	private static final String SERVER_LOG_LOGIN_URL = "https://sgrvg-carle.rhcloud.com/j_spring_security_check";
	private static final String USERNAME = System.getenv("LOG_USERNAME");
	private static final String PASSWORD = System.getenv("LOG_PASSWORD");
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

	private static final String LOG_MARKER = "{}";
	
	private static final Gson GSON = new Gson();

	private final BlockingQueue<LogEntry> entries;
	private final ExecutorService executor;

	private final boolean infoEnabled;
	private final boolean warnEnabled;
	private final boolean errorEnabled;
	private final boolean debugEnabled;

	@Inject
	public LoggerService() {
		super();
	}

	{
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("logging.properties");
		Properties props = new Properties();
		try {
			if (is == null) {
				is = new FileInputStream(new File("conf/logging.properties"));
			}
			props.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
		infoEnabled = Boolean.parseBoolean(props.getProperty("info", "true"));
		warnEnabled = Boolean.parseBoolean(props.getProperty("warn", "true"));
		errorEnabled = Boolean.parseBoolean(props.getProperty("error", "true"));
		debugEnabled = Boolean.parseBoolean(props.getProperty("debug", "true"));
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
		executor.submit(new LogSendTask());
	}

	@Override
	public void error(String arg0, Object...args) {
		if (isErrorEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "ERROR", new Date(), Thread.currentThread().getName(), null);
			entries.add(log);
		}
	}

	@Override
	public void error(String arg0, Throwable arg1, Object...args) {
		if (isErrorEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "ERROR", new Date(), Thread.currentThread().getName(), arg1);
			entries.add(log);
		}
	}

	@Override
	public void info(String arg0, Object...args) {
		if (isInfoEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "INFO", new Date(), Thread.currentThread().getName(), null);
			entries.add(log);
		}
	}

	@Override
	public void info(String arg0, Throwable arg1, Object...args) {
		if (isInfoEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "INFO", new Date(), Thread.currentThread().getName(), arg1);
			entries.add(log);
		}
	}

	@Override
	public boolean isErrorEnabled() {
		return errorEnabled;
	}

	@Override
	public boolean isInfoEnabled() {
		return infoEnabled;
	}

	@Override
	public boolean isWarnEnabled() {
		return warnEnabled;
	}
	
	@Override
	public boolean isDebugEnabled() {
		return debugEnabled;
	}
	
	@SuppressWarnings("unused")
	private boolean isCategoryEnabled(Category category) {
		switch (category) {
		case DEBUG:
			return isDebugEnabled();
		case ERROR:
			return isErrorEnabled();
		case INFO:
			return isInfoEnabled();
		case WARN:
			return isWarnEnabled();
		default:
			return true;
		}
	}

	@Override
	public void warn(String arg0, Object...args) {
		if (isWarnEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "WARN", new Date(), Thread.currentThread().getName(), null);
			entries.add(log);
		}
	}

	@Override
	public void warn(String arg0, Throwable arg1, Object... args) {
		if (isWarnEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "WARN", new Date(), Thread.currentThread().getName(), arg1);
			entries.add(log);
		}
	}
	
	@Override
	public void debug(String arg0, Object... args) {
		if (isDebugEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "DEBUG", new Date(), Thread.currentThread().getName(), null);
			entries.add(log);
		}
	}

	@Override
	public void debug(String arg0, Throwable arg1, Object... args) {
		if (isDebugEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "DEBUG", new Date(), Thread.currentThread().getName(), arg1);
			entries.add(log);
		}
	}

	private String buildMessage(String messageWithMarkers, Object...args) {
		StringBuilder message = new StringBuilder(messageWithMarkers);
		if (args != null && args.length > 0) {
			Arrays.stream(args).forEach(x -> {
				int index = message.indexOf(LOG_MARKER);
				if (index >= 0) {
					message.replace(index, index + LOG_MARKER.length(), String.valueOf(x));
				}
			});
		}
		return message.toString();
	}

	private class LogEntry implements Serializable {

		private static final long serialVersionUID = 1L;

		private String message;
		private String category;
		private Date timestamp;
		private Throwable e;
		private String threadName;

		public LogEntry(String message, String category, Date timestamp, String threadName, Throwable e) {
			super();
			this.message = message;
			this.category = category;
			this.timestamp = timestamp;
			this.threadName = threadName;
			this.e = e;
		}

		@SuppressWarnings("unused")
		public String getMessage() {
			return message;
		}

		public String getCategory() {
			return category;
		}

		@SuppressWarnings("unused")
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
			message.append(this.message);
			message.append(" ["  + threadName + "]\n");

			if (e != null) {
				StringWriter stringWriter = new StringWriter();
				PrintWriter printWriter = new PrintWriter(stringWriter);
				e.printStackTrace(printWriter);
				message.append(stringWriter.toString());
				printWriter.close();
			}
			return message.toString();
		}
	}

	private class LogSendTask implements Runnable {

		private AsyncHttpClient http;
		private boolean authenticated = false;
		private List<Cookie> cookies = new ArrayList<>();

		{
			AsyncHttpClientConfig cf = new DefaultAsyncHttpClientConfig.Builder()
					.setUserAgent("SimpleLogger - CAM SECURITY")
					.setFollowRedirect(false)
					.build();
			
			http = new DefaultAsyncHttpClient(cf);
		}

		@Override
		public void run() {
			long start = System.currentTimeMillis();
			LoggerService.this.info("Start Logging Service task");
			authenticate();
			while (!executor.isShutdown()) {
				try {
					LogEntry entry = null;
					if (isAuthenticated()) {
						entry = entries.poll(10000, TimeUnit.MILLISECONDS);
						if (entry != null) {
							sendData(entry);
							System.out.println(entry);
						}
					} else {
						long now = System.currentTimeMillis();
						if (now - start > (60 * 1000)) {
							authenticate();
							start = System.currentTimeMillis();
						}
						entry = entries.poll(2000, TimeUnit.MILLISECONDS);
						if (entry != null) {
							System.out.println(entry);
						}
					}
				} catch (InterruptedException e) {
					System.err.println("Interrupted");
					e.printStackTrace();
				}
			}
		}

		private boolean isAuthenticated() {
			if (!authenticated) {
				return authenticated;
			} else {
				try {
					Response response = sendData(new LogEntry("Check Session", "PING", new Date(), Thread.currentThread().getName(), null)).get();
					String location = response.getHeader("Location");
					return response.getStatusCode() == 200 && Strings.isNullOrEmpty(location);
				} catch (InterruptedException | ExecutionException e) {
					authenticated = false;
				}
				return authenticated;
			}
		}

		private ListenableFuture<Response> sendData(LogEntry logEntry) {
			JsonObject message = new JsonObject();
			message.addProperty("message", logEntry.getFullMessage());
			message.addProperty("category", logEntry.getCategory());
			return http.preparePost(SERVER_LOG_URL)
				.addHeader("Content-Type", "application/json")
				.setBody(GSON.toJson(message))
				.setCookies(cookies)
				.execute();
		}

		private void authenticate() {
			try {
				String url = SERVER_LOG_LOGIN_URL + "?j_username=" + USERNAME + "&j_password=" + PASSWORD;  
				LoggerService.this.info("Trying authenticate to remote service in {}", url);
				Response response = http.preparePost(url)
					.addHeader("Content-Type", "application/x-www-form-urlencoded")
					.execute()
					.get();
				
				String newLocation = response.getHeader("Location");
				if (!Strings.isNullOrEmpty(newLocation)) {
					List<String> queryParams = URLUtil.getQueryParameterNames(new URL(newLocation));
					if (queryParams.contains("error")) {
						LoggerService.this.info("Failed authentication. Wrong username/password");
						authenticated = false;
					} else {
						System.out.println("Successfully authenticated");
						authenticated = true;
						cookies = response.getCookies();
					}
				} else {
					failedAuth(response, newLocation);
				}
			} catch (InterruptedException | ExecutionException e) {
				LoggerService.this.error("Failed to authenticate to url: ", e);
				authenticated = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void failedAuth(Response response, String newLocation) {
			LoggerService.this.warn("Failed to authenticate to remote service. Returned status code: {} and location {}", response.getStatusCode(), newLocation);
			authenticated = false;
		}
	}
}
