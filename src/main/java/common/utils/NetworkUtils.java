package common.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtils {

    /**
     * Tự động lấy địa chỉ IPv4 LAN của máy tính hiện tại.
     * Bỏ qua localhost (127.0.0.1) và các IP ảo của máy ảo (VMware, VirtualBox...).
     * * @return Chuỗi IP LAN (VD: 192.168.1.5). Nếu thất bại trả về 127.0.0.1
     */
    public static String getLocalIPv4Address() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Bỏ qua loopback (127.0.0.1), card mạng đang tắt, hoặc card mạng ảo (như VMware, VirtualBox)
                if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.isVirtual() 
                        || networkInterface.getDisplayName().toLowerCase().contains("vmware") 
                        || networkInterface.getDisplayName().toLowerCase().contains("virtualbox")) {
                    continue;
                }

                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();

                    // Chỉ lấy IPv4 và là địa chỉ nội bộ (192.168.x.x, 10.x.x.x, 172.16.x.x)
                    if (inetAddress instanceof Inet4Address && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Lỗi khi quét card mạng lấy IP LAN: " + e.getMessage());
        }
        
        // Nếu không tìm thấy card mạng Wi-Fi/LAN nào hợp lệ, an toàn nhất là trả về localhost
        return "127.0.0.1"; 
    }
}