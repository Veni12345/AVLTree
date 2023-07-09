import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 *二叉平衡搜索树的磁盘实现。
 * 内容：实现二叉平衡搜索树，树的数据结构保存在磁盘上
 * （树的节点和关系数据，保存在磁盘上而不是内存中。请合理设计磁盘上的数据结构，不要把内存数据直接序列化到磁盘）。需要有相关的查找和增删功能。
 */
public class DiskBasedBinarySearchTree {

    private final String filePath;
    private final int maxKeysPerPage;
    private FileChannel channel;
    private static final int INT_SIZE = Integer.SIZE / Byte.SIZE;

    private enum PageType {
        LEAF, INTERNAL
    }

    DiskBasedBinarySearchTree(String filePath, int maxKeysPerPage) {
        this.filePath = filePath;
        this.maxKeysPerPage = maxKeysPerPage;
        try {
//            channel = new RandomAccessFile(filePath, "rw").getChannel();
            channel = FileChannel.open(Path.of(filePath), StandardOpenOption.READ, StandardOpenOption.WRITE);
            long size = channel.size();
            if (size == 0) {
                initializeEmptyTree();
            }
        } catch (Exception e) {
            System.err.println("Failed to open file. Reason: " + e.getMessage());
        }
    }

    private void initializeEmptyTree() throws Exception {
        Node root = new Node(maxKeysPerPage, true);
        byte[] data = root.toBytes();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, data.length);
        buffer.put(data);
    }


    /**
     * 节点
     */
    private static class Node {
        int[] keys; //关键字  4*3
        long[] children; //存储当前节点的子节点  8*4
        int numKeys; //当前节点中包含的关键字数量
        boolean isLeaf; //是否为叶子节点
        int size; //所占用的字节数
//        private int INT_SIZE;

        Node() {
            keys = new int[0];
            children = new long[0];
            numKeys = 1;
            isLeaf = false;
            size = 0;
        }

        Node(int maxKeys, boolean isLeafNode) {
            keys = new int[maxKeys];
            children = new long[maxKeys + 1];
            Arrays.fill(keys, -1);
            Arrays.fill(children, -1);
            numKeys = 1;
            isLeaf = isLeafNode;
            size = 0;
        }

        byte[] toBytes() {
            int keySize = keys.length * INT_SIZE;
            int childSize = children.length * Long.BYTES;
            byte[] data = new byte[keySize + childSize + INT_SIZE * 3];
            int pos = 0;
            pos = writeIntArray(data, pos, keys);
            pos = writeLongArray(data, pos, children);
            pos = writeInt(data, pos, numKeys);
            pos = writeBoolean(data, pos, isLeaf);
            pos = writeInt(data, pos, size);
            return data;
        }

//        ByteBuffer toByteBuffer() {
//            int keySize = keys.length * INT_SIZE;
//            int childSize = children.length * Long.BYTES;
//            ByteBuffer buffer = ByteBuffer.allocate(keySize + childSize + INT_SIZE * 3);
//            writeIntArray(buffer, keys);
//            writeLongArray(buffer, children);
//            writeInt(buffer, numKeys);
//            writeBoolean(buffer, isLeaf);
//            writeInt(buffer, size);
//            buffer.rewind();
//            return buffer;
//        }

        /**
         * 反序列化
         *
         * @param data
         */
        void fromBytes(byte[] data) {
            int pos = 0;
            keys = readIntArray(data, pos, keys);
            pos += INT_SIZE * keys.length;
            children = readLongArray(data, pos, children);
            pos += Long.BYTES * children.length;
            numKeys = readInt(data, pos);
            pos += INT_SIZE;
            isLeaf = readBoolean(data, pos);
            pos += 1;
            size = readInt(data, pos);
        }

        void fromBytes111(byte[] data) {
            int pos = 0;
            keys = readIntArray(data, pos, keys);
            pos += INT_SIZE * 3;
            children = readLongArray(data, pos, children);
            pos += Long.BYTES * 4;
            numKeys = readInt(data, pos);
            pos += INT_SIZE;
            isLeaf = readBoolean(data, pos);
            pos += 1;
            size = readInt(data, pos);
        }

        void fromBytes(ByteBuffer buffer) {
            int pos = buffer.position();
            keys = readIntArray(buffer, pos, keys);
            pos += INT_SIZE * keys.length;
            children = readLongArray(buffer, pos, children);
            pos += Long.BYTES * children.length;
            numKeys = readInt(buffer, pos);
            if (numKeys < 0)
                numKeys = 0;
            pos += INT_SIZE;
            isLeaf = readBoolean(buffer, pos);
            pos += 1;
            size = readInt(buffer, pos);
            buffer.position(pos + 4); // 将 buffer 的 position 恢复到正确的位置
        }


//         void fromBytes(ByteBuffer buffer) {
//         this.keyCount = buffer.getInt();
//         for (int i = 0; i < keyCount; i++) {
//         this.keys[i] = buffer.getInt();
//         }
//         this.childrenPointer[0] = buffer.getLong();
//         for (int i = 0; i < keyCount; i++) {
//         this.childrenPointer[i + 1] = buffer.getLong();
//         }
//         }
//
//         private byte[] toBytes() {
//         byte[] data = new byte[size()];
//         ByteBuffer buffer = ByteBuffer.wrap(data);
//         buffer.putInt(this.keyCount);
//         for (int i = 0; i < keyCount; i++) {
//         buffer.putInt(this.keys[i]);
//         }
//         buffer.putLong(this.childrenPointer[0]);
//         for (int i = 0; i < keyCount; i++) {
//         buffer.putLong(this.childrenPointer[i + 1]);
//         }
//         return data;
//         }


        /**
         * 函数返回一个整数类型的值，表示查找结果。
         * 如果找到了元素，则返回该元素的索引值（从 0 开始）；
         * 否则返回一个负数，其值为 -(插入点索引 + 1)。
         * 插入点索引表示将该元素插入到数组中应该放置的位置，即其左侧元素都小于它，右侧元素都大于等于它
         *
         * @param key
         * @return
         * @problem: 当数组中存在多个值与 key 相等时，不能保证返回的是哪一个元素的索引值
         */
        int findKey(int key) {
//            int idx = Arrays.binarySearch(keys, 0, numKeys, key);
            int idx = Arrays.binarySearch(keys, 0, keys.length, key);
//            return idx;
            return idx >= 0 ? idx : -(idx + 1);
        }

    }


    public void insert(int key) throws Exception {
        //只适用于对文件进行读写操作  不能使用此方法来读写已存在于内存中的 ByteBuffer 对象
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
        Node root = new Node();
        byte[] byteArray = readFileToByteArray("tree.bin");
//        byte[] array = buffer.array();
        root.fromBytes111(byteArray);
//        root.fromBytes(buffer);
        Node node = insert(root, key);
        if (node != null) {
            byte[] data = node.toBytes();
            buffer = channel.map(FileChannel.MapMode.READ_WRITE, nodePointer(node), data.length);
            buffer.put(data);
        }
    }

    public static byte[] readFileToByteArray(String fileName) throws IOException {
        InputStream inputStream = new FileInputStream(fileName);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private Node insert(Node node, int key) throws IOException {
        if (node.isLeaf) {
            if (node.numKeys == maxKeysPerPage) {
                return splitLeaf(node, key);
            } else {
                insertKey(node, key);
                writeSize(node, 1);
                return node;
            }
        } else {
            int idx = node.findKey(key);
            Node child = loadNode(node.children[idx]);
            Node newChild = insert(child, key);
            if (newChild != null) {
                if (node.numKeys == maxKeysPerPage) {
                    return splitInternal(node, newChild);
                } else {
                    insertChild(node, newChild, idx + 1);
                    return node;
                }
            } else {
                return null;
            }
        }
    }

    /**
     * 插入关键字
     *
     * @param node
     * @param key
     */
    private void insertKey(Node node, int key) {
        int idx = node.findKey(key);
//        System.arraycopy(node.keys, idx, node.keys, idx + 1, node.numKeys - idx);
        System.arraycopy(node.keys, idx, node.keys, idx + 1, INT_SIZE* maxKeysPerPage- idx);
        node.keys[idx] = key;
        node.numKeys++;
    }

    private void insertKey111(Node node, int key) {
        int idx = node.findKey(key);
        System.arraycopy(node.keys, idx, node.keys, idx + 1, INT_SIZE - idx);
        node.keys[idx] = key;
        node.numKeys++;
    }

    private void insertChild(Node node, Node newChild, int idx) {
        System.arraycopy(node.children, idx, node.children, idx + 1, node.numKeys - idx);
        node.children[idx] = nodePointer(newChild);
        node.numKeys++;
    }

    private Node loadNode(long ptr) {
        try {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, ptr, maxKeysPerPage * 3 * INT_SIZE);
            Node node = new Node();
//            node.fromBytes(buffer.array());
            node.fromBytes(buffer);
            return node;
        } catch (Exception e) {
            System.err.println("Failed to load node at position " + ptr + ". Reason: " + e.getMessage());
            return null;
        }
    }

    private void writeSize(Node node, int size) throws IOException {
        while (node != null) {
            node.size += size;
            long ptr = nodePointer(node);
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, ptr + node.keys.length * INT_SIZE + node.children.length * Long.BYTES + INT_SIZE * 2, INT_SIZE);
            buffer.putInt(node.size);
            node = loadNode(node.children[node.numKeys]);
        }
    }

    private Node splitLeaf(Node node, int key) throws IOException {
        Node newNode = new Node(maxKeysPerPage, true);
        int mid = maxKeysPerPage / 2;
        if (key < node.keys[mid]) {
            System.arraycopy(node.keys, 0, newNode.keys, 0, mid);
            System.arraycopy(node.children, 0, newNode.children, 0, mid + 1);
            insertKey(node, key);
            System.arraycopy(node.keys, mid, node.keys, mid + 1, node.numKeys - mid - 1);
            System.arraycopy(node.children, mid, node.children, mid + 1, node.numKeys - mid);
            node.keys[mid] = -1;
            node.children[mid] = nodePointer(newNode);
            node.numKeys++;
        } else {
            System.arraycopy(node.keys, mid, newNode.keys, 0, maxKeysPerPage - mid);
            System.arraycopy(node.children, mid, newNode.children, 0, maxKeysPerPage - mid + 1);
            insertKey(newNode, key);
            node.numKeys = mid;
            newNode.children[maxKeysPerPage] = node.children[maxKeysPerPage];
            node.children[maxKeysPerPage] = nodePointer(newNode);
        }
        writeSize(node, 1);
        writeSize(newNode, 1);
        return newNode;
    }

    private Node splitInternal(Node node, Node newChild) throws IOException {
        Node newNode = new Node(maxKeysPerPage, false);
        int mid = maxKeysPerPage / 2;
        int median = node.keys[mid];
        if (newChild.keys[0] < median) {
            System.arraycopy(node.keys, 0, newNode.keys, 0, mid);
            System.arraycopy(node.children, 0, newNode.children, 0, mid + 1);
            insertChild(node, newChild, mid);
            newChild.size = 1 + sumSizes(newChild);
            System.arraycopy(node.keys, mid + 1, node.keys, mid, node.numKeys - mid);
            System.arraycopy(node.children, mid + 1, node.children, mid + 2, node.numKeys - mid - 1);
            node.keys[node.numKeys - 1] = -1;
            node.children[node.numKeys] = -1;
            node.numKeys--;
        } else if (newChild.keys[0] > median) {
            System.arraycopy(node.keys, mid + 1, newNode.keys, 0, maxKeysPerPage - mid - 1);
            System.arraycopy(node.children, mid + 1, newNode.children, 0, maxKeysPerPage - mid);
            insertChild(newNode, newChild, 0);
            newChild.size = 1 + sumSizes(newChild);
            node.numKeys = mid;
            newNode.children[maxKeysPerPage] = node.children[maxKeysPerPage];
            node.children[maxKeysPerPage] = -1;
        } else {
            insertChild(node, newChild, mid + 1);
            newChild.size = 1 + sumSizes(newChild);
        }
        writeSize(node, 1);
        writeSize(newNode, 1);
        return median == newChild.keys[0] ? null : newNode;
    }

    private int sumSizes(Node node) {
        int sum = node.size;
        for (int i = 0; i <= node.numKeys; i++) {
            Node child = loadNode(node.children[i]);
            sum += child.size;
        }
        return sum;
    }

    private long nodePointer(Node node) {
        return (long) maxKeysPerPage * (nodePointerIndex(node) - 1);
    }

    private int nodePointerIndex(Node node) {
        int idx = 1;
        Node current = loadNode(maxKeysPerPage);
        while (current != null) {
            if (Arrays.equals(current.keys, node.keys) && Arrays.equals(current.children, node.children)) {
                return idx;
            } else {
                idx++;
                current = loadNode(maxKeysPerPage * idx);
            }
        }
        return -1;
    }

    public boolean search(int key) throws Exception {
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
        Node root = new Node();
//        root.fromBytes(buffer.array());
        root.fromBytes(buffer);
        return search(root, key);
    }

    private boolean search(Node node, int key) {
        int idx = node.findKey(key);
        if (idx < node.numKeys && node.keys[idx] == key) {
            return true;
        } else if (node.isLeaf) {
            return false;
        } else {
            Node child = loadNode(node.children[idx]);
            return search(child, key);
        }
    }


    /**
     * 读取字节数组中整数数组  4 个字节转换成一个 int 类型的值
     *
     * @param data  要读取的字节数组
     * @param pos   从字节数组中的哪个位置开始读取
     * @param array 目标整数数组
     * @return
     */
    private static int[] readIntArray(byte[] data, int pos, int[] array) {
        int size = array.length * INT_SIZE;
        byte[] tmp = new byte[size];
        System.arraycopy(data, pos, tmp, 0, size);
        for (int i = 0; i < array.length; i++) {
            array[i] = readInt(tmp, i * INT_SIZE);
        }
        return array;
    }

    private static int[] readIntArray(ByteBuffer buffer, int pos, int[] array) {
        int size = array.length * INT_SIZE;
        byte[] tmp = new byte[size];
        buffer.position(pos);
        buffer.get(tmp, 0, size);
        for (int i = 0; i < array.length; i++) {
            array[i] = readInt(tmp, i * INT_SIZE);
        }
        return array;
    }

    private static long[] readLongArray(byte[] data, int pos, long[] array) {
        int size = array.length * Long.BYTES;
        byte[] tmp = new byte[size];
        System.arraycopy(data, pos, tmp, 0, size);
        for (int i = 0; i < array.length; i++) {
            array[i] = readLong(tmp, i * Long.BYTES);
        }
        return array;
    }

    private static long[] readLongArray(ByteBuffer buffer, int pos, long[] array) {
        int size = array.length * Long.BYTES;
        byte[] tmp = new byte[size];
        buffer.position(pos);
        buffer.get(tmp, 0, size);
        for (int i = 0; i < array.length; i++) {
            array[i] = readLong(tmp, i * Long.BYTES);
        }
        return array;
    }

    private static int readInt(byte[] data, int pos) {
        int le = (data[pos] & 0xff) << 24 | (data[pos + 1] & 0xff) << 16 | (data[pos + 2] & 0xff) << 8 | (data[pos + 3] & 0xff);
        return le;
    }

    private static int readInt(ByteBuffer buffer, int pos) {
        int anInt = buffer.getInt(pos);
        return anInt;
    }

    private static long readLong(byte[] data, int pos) {
        return ((long) readInt(data, pos)) << 32 | (readInt(data, pos + 4) & 0xffffffffL);
    }

    private static boolean readBoolean(byte[] data, int pos) {
        return data[pos] != 0;
    }

    private static boolean readBoolean(ByteBuffer buffer, int pos) {
        return buffer.get(pos) != 0;
    }


    /**
     * 将整数值写入字节数组
     * <p>
     * 将 value 的高 8 位右移 24 位，并与 0xff 进行按位与运算，得到最高位的字节值，然后将该字节值存入 data[pos] 中；
     * 将 value 的第二高的 8 位右移 16 位，并与 0xff 进行按位与运算，得到次高位的字节值，然后将该字节值存入 data[pos+1] 中；
     * 将 value 的第三高的 8 位右移 8 位，并与 0xff 进行按位与运算，得到第三高位的字节值，然后将该字节值存入 data[pos+2] 中；
     * 将 value 的最低的 8 位（即最后一个字节）与 0xff 进行按位与运算，得到最低字节的值，然后将该字节值存入 data[pos+3] 中；
     * 返回当前写入后的位置，即 pos+INT_SIZE。
     *
     * @param data  目标字节数组
     * @param pos   写入的起始位置
     * @param value 要写入的整数值
     * @return
     */
    private static int writeInt(byte[] data, int pos, int value) {
        data[pos] = (byte) ((value >> 24) & 0xff);
        data[pos + 1] = (byte) ((value >> 16) & 0xff);
        data[pos + 2] = (byte) ((value >> 8) & 0xff);
        data[pos + 3] = (byte) (value & 0xff);
        return pos + INT_SIZE;
    }

    private static int writeLong(byte[] data, int pos, long value) {
        writeInt(data, pos, (int) (value >>> 32));
        writeInt(data, pos + 4, (int) value);
        return pos + Long.BYTES;
    }

    private static int writeBoolean(byte[] data, int pos, boolean value) {
        data[pos] = (byte) (value ? 1 : 0);
        return pos + 1;
    }

    private static int writeIntArray(byte[] data, int pos, int[] array) {
        for (int i = 0; i < array.length; i++) {
            pos = writeInt(data, pos, array[i]);
        }
        return pos;
    }

    private static int writeLongArray(byte[] data, int pos, long[] array) {
        for (int i = 0; i < array.length; i++) {
            pos = writeLong(data, pos, array[i]);
        }
        return pos;
    }


    public static void main(String[] args) throws Exception {
        DiskBasedBinarySearchTree tree = new DiskBasedBinarySearchTree("tree.bin", 3);
        tree.insert(10);
        tree.insert(5);
        tree.insert(7);
        tree.insert(12);
        tree.insert(15);
        System.out.println(tree.search(10)); // true
        System.out.println(tree.search(5));  // true
        System.out.println(tree.search(7));  // true
        System.out.println(tree.search(12)); // true
        System.out.println(tree.search(15)); // true
        System.out.println(tree.search(8));  // false
    }

}
