package com.vylote.chess.controller;

import android.content.Context;
import android.net.wifi.WifiManager;
import java.net.*;

import model.PlayerProfile;

public class DiscoveryService {
    private static final int UDP_PORT = 8888;
    private static final String BROADCAST_MSG = "CHESS_HOST:";
    private boolean running = false;
    private WifiManager.MulticastLock lock;

    public DiscoveryService(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            // Cấu hình Lock để nhận mọi gói tin Broadcast/Multicast
            lock = wifi.createMulticastLock("ChessDiscoveryLock");
            lock.setReferenceCounted(false); // Đổi thành false để quản lý thủ công chính xác hơn
        }
    }
    public void startListening(OnHostDiscovered callback) {
        running = true;
        if (lock != null) lock.acquire(); // Bắt đầu cho phép nhận gói tin UDP
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
                socket.setSoTimeout(5000);
                byte[] buffer = new byte[1024];
                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        if (msg.startsWith(BROADCAST_MSG)) {
                            String[] parts = msg.split(":");
                            callback.onDiscovered(new PlayerProfile(parts[1], 0, packet.getAddress().getHostAddress()));
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) { e.printStackTrace();
            } finally {
                if (lock != null && lock.isHeld()) lock.release();
            }
            if (lock != null && lock.isHeld()) lock.release();
        }).start();
    }

    public void stop() {
        running = false;
        if (lock != null && lock.isHeld()) lock.release();
    }

    public interface OnHostDiscovered {
        void onDiscovered(PlayerProfile host);
    }

    // Trong startBroadcasting của DiscoveryService.java
    public void startBroadcasting(String name, int color) {
        running = true;
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                // Gói tin chứa: CHESS_HOST | Tên | Màu
                String msg = "CHESS_HOST:" + name + ":" + color;
                byte[] buffer = msg.getBytes();
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                            InetAddress.getByName("255.255.255.255"), 8888);
                    socket.send(packet);
                    Thread.sleep(2000); // 2 giây phát 1 lần
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
}