package com.nx.aohc.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LanDiscovery {

    public static class DiscoveredHost {
        public String address;
        public String name;
        public int playerCount;

        public DiscoveredHost(String address, String name, int playerCount) {
            this.address = address;
            this.name = name;
            this.playerCount = playerCount;
        }
    }

    private final ConcurrentLinkedQueue<DiscoveredHost> found = new ConcurrentLinkedQueue<DiscoveredHost>();
    private final Array<DiscoveredHost> hosts = new Array<DiscoveredHost>();
    private volatile boolean scanning;

    public void scan() {
        if (scanning) {
            return;
        }
        scanning = true;
        hosts.clear();
        found.clear();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    socket.setSoTimeout(2500);

                    byte[] query = NetworkSession.DISCOVERY_QUERY.getBytes("UTF-8");
                    socket.send(new DatagramPacket(query, query.length,
                            InetAddress.getByName("255.255.255.255"), NetworkSession.DISCOVERY_PORT));

                    byte[] buffer = new byte[256];
                    long deadline = System.currentTimeMillis() + 2500;
                    while (System.currentTimeMillis() < deadline) {
                        try {
                            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                            socket.receive(packet);
                            String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                            String[] parts = message.split("\u001f", -1);
                            if (parts.length >= 3 && NetworkSession.DISCOVERY_REPLY.equals(parts[0])) {
                                int count = 0;
                                try {
                                    count = Integer.parseInt(parts[2]);
                                } catch (NumberFormatException ignored) {
                                }
                                found.add(new DiscoveredHost(packet.getAddress().getHostAddress(), parts[1], count));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception exception) {
                    Gdx.app.error("LanDiscovery", "Scan failed", exception);
                } finally {
                    if (socket != null) {
                        socket.close();
                    }
                    scanning = false;
                }
            }
        }, "aohc-discovery");
        thread.setDaemon(true);
        thread.start();
    }

    public void poll() {
        DiscoveredHost host;
        while ((host = found.poll()) != null) {
            boolean duplicate = false;
            for (int index = 0; index < hosts.size; index++) {
                if (hosts.get(index).address.equals(host.address)) {
                    hosts.get(index).playerCount = host.playerCount;
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                hosts.add(host);
            }
        }
    }

    public boolean isScanning() {
        return scanning;
    }

    public Array<DiscoveredHost> getHosts() {
        return hosts;
    }
}
