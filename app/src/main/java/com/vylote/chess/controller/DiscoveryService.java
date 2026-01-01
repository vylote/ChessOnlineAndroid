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
    // Trong lớp DiscoveryService của Android
    public void startListening(OnHostDiscovered callback) {
        running = true;
        if (lock != null) lock.acquire(); // BẮT BUỘC để Android nhận được gói tin UDP

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
                socket.setSoTimeout(5000); // Tránh treo luồng
                byte[] buffer = new byte[1024];

                while (running) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());

                        if (msg.startsWith(BROADCAST_MSG)) { // "CHESS_HOST:"
                            String[] parts = msg.split(":");
                            String name = parts[1];
                            int color = Integer.parseInt(parts[2]);
                            String ip = packet.getAddress().getHostAddress();

                            // Trả về thông tin Host tìm thấy cho UI
                            callback.onDiscovered(new PlayerProfile(name, color, ip));
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stop(); // Giải phóng lock khi kết thúc
            }
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