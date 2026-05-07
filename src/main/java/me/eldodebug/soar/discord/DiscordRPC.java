package me.eldodebug.soar.discord;

import java.time.OffsetDateTime;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.discord.ipc.IPCClient;
import me.eldodebug.soar.discord.ipc.IPCListener;
import me.eldodebug.soar.discord.ipc.entities.RichPresence;
import me.eldodebug.soar.discord.ipc.exceptions.NoDiscordClientException;

public class DiscordRPC {

	private IPCClient client;
	private boolean started;
	
	public void start() {
		
		started = false;
		client = new IPCClient(1059341815205068901L);
		client.setListener(new IPCListener() {
			@Override
			public void onReady(IPCClient client) {
				
				RichPresence.Builder builder = new RichPresence.Builder();
				
				builder.setState("Playing FlaxClient " + Glide.getInstance().getVersion())
						.setStartTimestamp(OffsetDateTime.now())
						.setLargeImage("icon");
				
				client.sendRichPresence(builder.build());
			}
		});
		
			try {
				client.connect();
				started = true;
			} catch (NoDiscordClientException e) {
				started = false;
			}
	}
	
	public void stop() {
		started = false;
		if(client != null) {
			try {
				client.close();
			} catch (Exception ignored) {}
		}
	}

	public IPCClient getClient() {
		return client;
	}
	
	public boolean isStarted() {
		return started;
	}
}
