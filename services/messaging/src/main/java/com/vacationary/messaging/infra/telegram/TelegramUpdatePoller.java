package com.vacationary.messaging.infra.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class TelegramUpdatePoller implements ApplicationRunner, DisposableBean {

	private static final Logger LOG = LoggerFactory.getLogger(TelegramUpdatePoller.class);

	private final String botToken;
	private final TelegramUpdateHandler updateHandler;

	private volatile boolean running;
	private Thread pollerThread;

	public TelegramUpdatePoller(String botToken, TelegramUpdateHandler updateHandler) {
		this.botToken = botToken == null ? "" : botToken;
		this.updateHandler = updateHandler;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (botToken.isBlank()) {
			LOG.warn("Telegram is not configured");
			return;
		}
		running = true;
		pollerThread = new Thread(this::loop, "telegram-poller");
		pollerThread.setDaemon(true);
		pollerThread.start();
	}

	@Override
	public void destroy() {
		running = false;
		if (pollerThread != null) {
			pollerThread.interrupt();
		}
	}

	private void loop() {
		while (running) {
			try {
				updateHandler.poll();
			}
			catch (RuntimeException error) {
				if (!running) {
					return;
				}
				LOG.warn("Telegram getUpdates failed; retrying", error);
				try {
					Thread.sleep(3000);
				}
				catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}
}
