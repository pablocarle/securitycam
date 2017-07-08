package com.sgrvg.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Param;
import com.ning.http.client.Response;

/**
 * Tiene una cola, y en un thread aparte, va revisando cuando hay suficientes elementos de log para enviar (o cada determinado tiempo?)
 * 
 * @author pabloc
 *
 */
public final class LoggerService implements SimpleLogger {

	private static final String SERVER_LOG_URL = "https://sgrvg-carle.rhcloud.com/security/log";
	private static final String SERVER_LOG_LOGIN_URL = "https://sgrvg-carle.rhcloud.com/j_spring_security_check";
	private static final String USERNAME = "security";
	private static final String PASSWORD = "security123";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

	private static final String LOG_MARKER = "{}";
	
	private static final Gson GSON = new Gson();

	private final BlockingQueue<LogEntry> entries;
	private final ExecutorService executor;

	private final boolean infoEnabled;
	private final boolean warnEnabled;
	private final boolean errorEnabled;

	@Inject
	public LoggerService() {
		super();
	}

	{
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("logging.properties");
		Properties props = new Properties();
		try {
			props.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
		infoEnabled = Boolean.parseBoolean(props.getProperty("info", "true"));
		warnEnabled = Boolean.parseBoolean(props.getProperty("warn", "true"));
		errorEnabled = Boolean.parseBoolean(props.getProperty("error", "true"));
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

		public LogEntry(String message, String category, Date timestamp, Throwable e) {
			super();
			this.message = message;
			this.category = category;
			this.timestamp = timestamp;
			this.e = e;
		}

		@SuppressWarnings("unused")
		public String getMessage() {
			return message;
		}

		@SuppressWarnings("unused")
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

		{
			AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder()
					.setUserAgent("SimpleLogger - CAM SECURITY").build();
			http = new AsyncHttpClient(cf);
		}

		@Override
		public void run() {
			LoggerService.this.info("Start Logging Service task");
			authenticate();
			while (!executor.isShutdown()) {
				try {
					if (isAuthenticated()) {
						sendData(entries.poll(10000, TimeUnit.MILLISECONDS));
					} else {
						Thread.sleep(30 * 1000);
						authenticate();
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
					Response response = sendData(new LogEntry("Check Session", "PING", new Date(), null)).get();
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
			return http.preparePost(SERVER_LOG_URL)
				.addHeader("Content-Type", "application/json")
				.setBody(GSON.toJson(message))
				.setBodyEncoding("UTF-8")
				.setFollowRedirects(false)
				.execute();
		}

		private void authenticate() {
			try {
				String url = SERVER_LOG_LOGIN_URL + "?j_username=" + USERNAME + "&j_password=" + PASSWORD;  
				url = URLEncoder.encode(url, "UTF-8");
				LoggerService.this.info("Trying authenticate to remote service in {}", url);
				Response response = http.preparePost(url)
					.addHeader("Content-Type", "application/x-www-form-urlencoded")
					.setFollowRedirects(false)
					.setFormParams(Lists.newArrayList(new Param("j_username", USERNAME),
													  new Param("j_password", PASSWORD)))
					.execute().get();
				if (isSuccess(response.getStatusCode())) {
					LoggerService.this.warn("Success authenticate to remote service");
					authenticated = true;
				} else {
					LoggerService.this.warn("Failed to authenticate to remote service. Returned status code: {}", response.getStatusCode());
					authenticated = false;
				}
			} catch (UnsupportedEncodingException | InterruptedException | ExecutionException e) {
				LoggerService.this.error("Failed to authenticate to url: ", e);
				authenticated = false;
			}
		}

		private boolean isSuccess(int statusCode) {
			return statusCode >= 200 && statusCode < 300;
		}
	}
}
