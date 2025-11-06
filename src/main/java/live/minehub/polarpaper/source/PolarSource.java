package live.minehub.polarpaper.source;

public interface PolarSource {
    byte[] readBytes();

    void saveBytes(byte[] save);
}
