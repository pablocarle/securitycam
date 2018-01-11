package com.sgrvg.security.util;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.sgrvg.security.SimpleLogger;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.asynchttpclient.cookie.Cookie;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

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

	private static final String SERVER_LOG_URL = "http://sgrvg-sgrvg.193b.starter-ca-central-1.openshiftapps.com/sgrvg-web-1.0.0/service/logging";
	private static final String SERVER_LOG_LOGIN_URL = "http://sgrvg-sgrvg.193b.starter-ca-central-1.openshiftapps.com/sgrvg-web-1.0.0/j_spring_security_check";
	private static final String USERNAME = System.getenv("LOG_USERNAME");
	private static final String PASSWORD = System.getenv("LOG_PASSWORD");
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

	private static final String LOG_MARKER = "{}";

	private static final Gson GSON = new Gson();

	private final BlockingQueue<LogEntry> entries;
	private final ExecutorService executor;
	private final AsyncHttpClient http;

	private final boolean infoEnabled;
	private final boolean warnEnabled;
	private final boolean errorEnabled;
	private final boolean debugEnabled;
	private final boolean traceEnabled;

	@Inject
	public LoggerService(AsyncHttpClient http) {
		super();
		this.http = http;
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
		debugEnabled = Boolean.parseBoolean(props.getProperty("debug", "false"));
		traceEnabled = Boolean.parseBoolean(props.getProperty("trace", "false"));
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

	@Override
	public boolean isTraceEnabled() {
		return traceEnabled;
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

	@Override
	public void trace(String arg0, Object... args) {
		if (isTraceEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "TRACE", new Date(), Thread.currentThread().getName(), null);
			entries.add(log);
		}
	}

	@Override
	public void trace(String arg0, Throwable arg1, Object... args) {
		if (isTraceEnabled()) {
			LogEntry log = new LogEntry(buildMessage(arg0, args), "TRACE", new Date(), Thread.currentThread().getName(), arg1);
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

		LogEntry(String message, String category, Date timestamp, String threadName, Throwable e) {
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
			message.append(" [").append(threadName).append("]\n");

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

		private boolean authenticated = false;
		private List<Cookie> cookies = new ArrayList<>();

		@Override
		public void run() {
			long start = System.currentTimeMillis();
			LoggerService.this.info("Start Logging Service task");
			authenticate();
			while (!executor.isShutdown()) {
				try {
					LogEntry entry;
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
				return false;
			} else {
				try {
					Response response = sendData(new LogEntry("Check Session", "PING", new Date(), Thread.currentThread().getName(), null)).get();
					String location = response.getHeader("Location");
					return response.getStatusCode() == 200 && Strings.isNullOrEmpty(location);
				} catch (InterruptedException | ExecutionException e) {
					authenticated = false;
				}
				return false;
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
				final String url = SERVER_LOG_LOGIN_URL + "?j_username=" + USERNAME + "&j_password=" + PASSWORD;
				LoggerService.this.info("Trying authenticate to remote service in {}", url);
				final Response response = http.preparePost(url)
						.setFormParams(
								ImmutableMap.of(
										"j_username", ImmutableList.of(USERNAME),
										"j_password", ImmutableList.of(PASSWORD)))
						.addHeader("Content-Type", "application/x-www-form-urlencoded")
						.execute()
						.get();

				final int statusCode = response.getStatusCode();
				if (statusCode == 200) {
					LoggerService.this.info("Successfully authenticated");
					authenticated = true;
					cookies = response.getCookies();
				} else {
					failedAuth(response, response.getResponseBody());
				}
			} catch (InterruptedException | ExecutionException e) {
				LoggerService.this.error("Failed to authenticate to url: ", e);
				authenticated = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private void failedAuth(Response response, String body) {
			LoggerService.this.warn("Failed to authenticate to remote service. Returned status code: {} and body {}", response.getStatusCode(), body);
			authenticated = false;
		}
	}
}
