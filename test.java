import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public abstract class LogReader {

    /**
     * 获取操作系统默认字符编码的方法：System.getProperties().get("sun.jnu.encoding");
     * 获取操作系统文件的字符编码的方法：System.getProperties().get("file.encoding");
     * 获取JVM默认字符编码的方法：Charset.defaultCharset();
     */
    public static final String DEFAULT_CHARSET = Charset.defaultCharset().name();

    protected String charsetName;

    abstract long getFilePointer();

    abstract void seek(long pos);

    abstract String readLine();

    /**
     * 字节数组扩容
     * @param arr
     * @return
     */
    public byte[] grow(byte[] arr) {
        int len = arr.length;
        int half = len >> 1;
        int growSize = Math.max(half, 1);
        byte[] arrNew = new byte[len + growSize];
        System.arraycopy(arr, 0, arrNew, 0, len);
        return arrNew;
    }

    /**
     * 字节数组解码成字符串
     * @param arr
     * @param arrPos
     * @return
     */
    public String decode(byte[] arr, int arrPos) {
        if (arrPos == 0)
            return null;
        try {
            return new String(arr, 0, arrPos, charsetName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}


import java.io.*;
import java.nio.charset.Charset;

public class BufferedLogReader extends LogReader implements Closeable {

    public static final int DEFAULT_BUFFER_CAPACITY = 8192;

    private byte[] buffer;

    private int position;

    private int limit = -1;

    private RandomAccessFile raf;

    public BufferedLogReader(String pathName) {
        init(new File(pathName), DEFAULT_BUFFER_CAPACITY, DEFAULT_CHARSET);
    }

    public BufferedLogReader(String pathName, String charsetName) {
        init(new File(pathName), DEFAULT_BUFFER_CAPACITY, charsetName);
    }

    public BufferedLogReader(String pathName, int bufferCapacity) {
        init(new File(pathName), bufferCapacity, DEFAULT_CHARSET);
    }

    public BufferedLogReader(String pathName, int bufferCapacity, String charsetName) {
        init(new File(pathName), bufferCapacity, charsetName);
    }

    public BufferedLogReader(File file) {
        init(file, DEFAULT_BUFFER_CAPACITY, DEFAULT_CHARSET);
    }

    public BufferedLogReader(File file, String charsetName) {
        init(file, DEFAULT_BUFFER_CAPACITY, charsetName);
    }

    public BufferedLogReader(File file, int bufferCapacity) {
        init(file, bufferCapacity, DEFAULT_CHARSET);
    }

    public BufferedLogReader(File file, int bufferCapacity, String charsetName) {
        init(file, bufferCapacity, charsetName);
    }

    private void init(File file, int bufferCapacity, String charsetName) {
        if (bufferCapacity < 1)
            throw new IllegalArgumentException("bufferCapacity");
        Charset.forName(charsetName); // 检查字符集是否合法
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        buffer = new byte[bufferCapacity];
        this.charsetName = charsetName;
    }

    @Override
    public long getFilePointer() {
        try {
            return raf.getFilePointer();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void seek(long pos) {
        try {
            raf.seek(pos);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String readLine() {
        try {
            if (position > limit) {
                if (!readMore())
                    return null;
            }
            byte[] arr = new byte[336];
            int arrPos = 0;
            while (position <= limit) {
                byte b = buffer[position++];
                switch (b) {
                    case 10: //Unix or Linux line separator
                        return decode(arr, arrPos);
                    case 13: //Windows or Mac line separator
                        if (position > limit) {
                            if (readMore())
                                judgeMacOrWindows();
                        } else
                            judgeMacOrWindows();
                        return decode(arr, arrPos);
                    default: // not line separator
                        if (arrPos >= arr.length)
                            arr = grow(arr);
                        arr[arrPos++] = b;
                        if (position > limit) {
                            if (!readMore())
                                return decode(arr, arrPos);
                        }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return null;
    }

    private void judgeMacOrWindows() {
        byte b1 = buffer[position++];
        if (b1 != 10) // Mac line separator
            position--;
    }

    private boolean readMore() throws IOException {
        limit = raf.read(buffer) - 1;
        position = 0;
        return limit >= 0;
    }

    @Override
    public void close() {
        try {
            raf.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}



import sun.misc.Cleaner;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class MemoryMapLogReader extends LogReader implements Closeable {

    private int position;

    private int limit = -1;

    private MappedByteBuffer buffer;

    private FileChannel channel;

    private RandomAccessFile raf;

    public MemoryMapLogReader(String pathName) {
        init(new File(pathName), DEFAULT_CHARSET);
    }

    public MemoryMapLogReader(String pathName, String charsetName) {
        init(new File(pathName), charsetName);
    }

    public MemoryMapLogReader(File file) {
        init(file, DEFAULT_CHARSET);
    }

    public MemoryMapLogReader(File file, String charsetName) {
        init(file, charsetName);
    }

    private void init(File file, String charsetName) {
        Charset.forName(charsetName); // 检查字符集是否合法
        this.charsetName = charsetName;
        try {
            raf = new RandomAccessFile(file, "r");
            channel = raf.getChannel();
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, raf.getFilePointer(), channel.size());
            limit = buffer.limit();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public long getFilePointer() {
        try {
            return raf.getFilePointer(); // 一直是0, 说明采用内存映射时这个方法不起作用
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void seek(long pos) {
        try {
            raf.seek(pos); // 采用内存映射时这个方法不起作用
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String readLine() {
        if (position >= limit)
            return null;
        byte[] arr = new byte[336];
        int arrPos = 0;
        while (true) {
            byte b = buffer.get(position++);
            switch (b) {
                case 10: //Unix or Linux line separator
                    return decode(arr, arrPos);
                case 13: //Windows or Mac line separator
                    if (position < limit)
                        judgeMacOrWindows();
                    return decode(arr, arrPos);
                default: // not line separator
                    if (arrPos >= arr.length)
                        arr = grow(arr);
                    arr[arrPos++] = b;
                    if (position >= limit)
                        return decode(arr, arrPos);
            }
        }
    }

    private void judgeMacOrWindows() {
        byte b1 = buffer.get(position++);
        if (b1 != 10) // Mac line separator
            position--;
    }

    @Override
    public void close() throws IOException {
        if (raf != null)
            raf.close();
        if (channel != null)
            channel.close();
        if (buffer != null)
            buffer.clear(); // 并不会真正清理buffer里的数据, 只是改变内部数组指针位置, 详情请看源码注释
        clean(); // 这个才会真正清理buffer里的数据
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void clean() {
        AccessController.doPrivileged((PrivilegedAction) () -> {
            try {
                Method getCleanerMethod = buffer.getClass().getMethod("cleaner");
                getCleanerMethod.setAccessible(true);
                Cleaner cleaner =(Cleaner) getCleanerMethod.invoke(buffer, new Object[0]);
                cleaner.clean();
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }
}
