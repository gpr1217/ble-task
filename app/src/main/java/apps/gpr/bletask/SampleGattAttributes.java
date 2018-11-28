package apps.gpr.bletask;

import java.util.UUID;

public class SampleGattAttributes {

    public static UUID SERVICE_UUID = covertFromInteger(0x2A02);
    public static UUID CHARACTERISTICS_UUID = covertFromInteger(0x2A03);

    private static UUID covertFromInteger(int i){

        final long msb = 0x0000000000001000L;
        final long lsb = 0x800000805f9b34fbL;

        long value = i & 0xFFFFFFFF;

        return new UUID(msb | (value << 32), lsb);
    }
}
