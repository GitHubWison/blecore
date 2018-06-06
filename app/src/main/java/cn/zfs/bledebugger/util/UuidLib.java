package cn.zfs.bledebugger.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 描述:
 * 时间: 2018/5/17 13:33
 * 作者: zengfansheng
 */
public class UuidLib {
    private static Map<String, String> serviceNames;
    private static Map<String, String> characteristicNames;
    
    static {
        serviceNames = extract("gatt/services.txt");
        characteristicNames = extract("gatt/characteristics.txt");
    }
    
    //解析已注册的一些UUID
    private static Map<String, String> extract(String path) {
        Map<String, String> names = new HashMap<>();
        try {
            InputStream inputStream = UiUtils.getContext().getAssets().open(path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split("\t");
                names.put(generateBluetoothBaseUuid(Long.parseLong(split[2].substring(2), 16)).toString(), split[0]);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return names;
    }
    
    private static UUID generateBluetoothBaseUuid(long paramLong) {
        return new UUID(4096L + (paramLong << 32), -9223371485494954757L);
    }
    
    public static String getServiceName(UUID uuid) {
        if (uuid != null) {
            return serviceNames.get(uuid.toString());
        }
        return "";
    }
    
    public static String getCharacteristicName(UUID uuid) {
        if (uuid != null) {
            return characteristicNames.get(uuid.toString());
        }
        return "";
    }
}
