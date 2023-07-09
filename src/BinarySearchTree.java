import java.io.*;
import java.util.*;

/**
 * 实现一个二叉搜索树，树的数据结构保存在磁盘上
 */
public class BinarySearchTree {

    private Node root;
    private int nodeSize;
    private int dataSize;
    //使用map存储节点，快速查找
    private final Map<Integer, Node> cache = new HashMap<>();

    public BinarySearchTree() {
        root = null;
        nodeSize = 0;
        dataSize = 0;
    }

    /**
     * 遍历得到相应的插入位置，进行插入操作
     *
     * @param key
     * @param data
     * @throws IOException
     */
    public void insert(int key, String data) throws IOException {
        Node newNode = new Node(key, data);
        if (root == null) {
            root = newNode;
            updateNode(root);
        } else {
            Node parent;
            Node current = root;
            while (true) {
                parent = current;
                //左节点
                if (key < current.getKey()) {
                    current = current.getLeft();
                    if (current == null) {
                        parent.setLeft(newNode);
                        updateNode(parent);
                        updateNode(newNode);
                        break;
                    }
                } else {   //右节点中插入
                    current = current.getRight();
                    if (current == null) {
                        parent.setRight(newNode);
                        updateNode(parent);
                        updateNode(newNode);
                        break;
                    }
                }
            }
        }
        nodeSize++;
    }

    /**
     * 按照key 查找
     *
     * @param key
     * @return 找到节点则返回node
     * @throws IOException
     */
    public Node find(int key) throws IOException {
        if (root == null) {
            return null;
        }
        Node current = root;
        while (current.getKey() != key) {
            if (key < current.getKey()) {
                current = current.getLeft();
            } else {
                current = current.getRight();
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * 删除相应节点，并进行调整
     *
     * @param key
     * @return
     * @throws IOException
     */
    public boolean delete(int key) throws IOException {
        Node current = root;
        Node parent = root;
        boolean isLeftChild = true;

        while (current.getKey() != key) {
            parent = current;
            if (key < current.getKey()) {
                isLeftChild = true;
                current = current.getLeft();
            } else {
                isLeftChild = false;
                current = current.getRight();
            }
            if (current == null) {
                return false;
            }
        }

        if (current.getLeft() == null && current.getRight() == null) {
            if (current == root) {
                root = null;
                updateNode(root);
            } else if (isLeftChild) {
                parent.setLeft(null);
                updateNode(parent);
            } else {
                parent.setRight(null);
                updateNode(parent);
            }
        } else if (current.getRight() == null) {
            if (current == root) {
                root = current.getLeft();
                updateNode(root);
            } else if (isLeftChild) {
                parent.setLeft(current.getLeft());
                updateNode(parent);
            } else {
                parent.setRight(current.getLeft());
                updateNode(parent);
            }
        } else if (current.getLeft() == null) {
            if (current == root) {
                root = current.getRight();
                updateNode(root);
            } else if (isLeftChild) {
                parent.setLeft(current.getRight());
                updateNode(parent);
            } else {
                parent.setRight(current.getRight());
                updateNode(parent);
            }
        } else {
            //获取后继节点
            Node successor = getSuccessor(current);
            if (current == root) {
                root = successor;
                updateNode(root);
            } else if (isLeftChild) {
                parent.setLeft(successor);
                updateNode(parent);
            } else {
                parent.setRight(successor);
                updateNode(parent);
            }
            successor.setLeft(current.getLeft());
            updateNode(successor);
        }
        nodeSize--;
        return true;
    }

    /**
     * 获取当前节点的后继节点
     *
     * @param delNode
     * @return
     * @throws IOException
     */
    private Node getSuccessor(Node delNode) throws IOException {
        Node successorParent = delNode;
        Node successor = delNode;
        Node current = delNode.getRight();

        /** 如果 delNode的右子树不为空，则它的后继节点是它右子树中最小的点（向左遍历至最深） */
        while (current != null) {
            successorParent = successor;
            successor = current;
            current = current.getLeft();
        }
        /** 如果delNode的右子树为空，则它的后继是第一个向左走的祖先 */
        if (successor != delNode.getRight()) {
            successorParent.setLeft(successor.getRight());
            updateNode(successorParent);
            successor.setRight(delNode.getRight());
            updateNode(successor);
        }

        return successor;
    }

    private void updateNode(Node node) throws IOException {
        if (node == null) {
            return;
        }
        if (cache.containsKey(node.getKey())) {
            cache.replace(node.getKey(), node);
        } else {
            if (dataSize == 0) {
                node.setPosition(0);
            } else {
                node.setPosition((int) (new File("data.dat").length() / dataSize) * dataSize);
            }
            cache.put(node.getKey(), node);
            RandomAccessFile raf = new RandomAccessFile("data.dat", "rw");
            raf.seek(node.getPosition());
            raf.writeInt(node.getKey());
            raf.writeUTF(node.getData());
            raf.writeInt(node.getHeight());
            raf.writeInt(node.getLeft() == null ? -1 : cache.get(node.getLeft().getKey()).getPosition());
            raf.writeInt(node.getRight() == null ? -1 : cache.get(node.getRight().getKey()).getPosition());
            raf.close();
            dataSize = 14 + node.getData().getBytes().length;
        }
    }

    /**
     * 非递归前序遍历
     *
     * @return
     */
    public List<Node> preOrderTraversal() {
        List<Node> result = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        Node current = root;
        while (!stack.empty() || current != null) {
            if (current != null) {
                stack.push(current);
                result.add(current);
                current = current.getLeft();
            } else {
                current = stack.pop().getRight();
            }
        }
        return result;
    }

    /**
     * 非递归 中序遍历
     *
     * @return
     */
    public List<Node> inOrderTraversal() {
        List<Node> result = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        Node current = root;
        while (!stack.empty() || current != null) {
            if (current != null) {
                stack.push(current);
                current = current.getLeft();
            } else {
                current = stack.pop();
                result.add(current);
                current = current.getRight();
            }
        }
        return result;
    }

    /**
     * 非递归 后续遍历
     *
     * @return
     */
    public List<Node> postOrderTraversal() {
        List<Node> result = new ArrayList<>();
        Stack<Node> stack1 = new Stack<>();
        Stack<Node> stack2 = new Stack<>();
        Node current = root;
        stack1.push(current);
        while (!stack1.empty()) {
            current = stack1.pop();
            stack2.push(current);
            if (current.getLeft() != null) {
                stack1.push(current.getLeft());
            }
            if (current.getRight() != null) {
                stack1.push(current.getRight());
            }
        }
        while (!stack2.empty()) {
            result.add(stack2.pop());
        }
        return result;
    }

    public int getNodeSize() {
        return nodeSize;
    }

    public static void main(String[] args) throws IOException {
        BinarySearchTree tree = new BinarySearchTree();
        tree.insert(50, "data 50");
        tree.insert(30, "data 30");
        tree.insert(70, "data 70");
        tree.insert(20, "data 20");
        tree.insert(40, "data 40");
        tree.insert(60, "data 60");
        tree.insert(80, "data 80");
        tree.insert(90, "data 90");
        tree.insert(100, "data 100");
//        tree.delete(20);
//        tree.delete(40);
        //二叉搜索树高度 height 全部为 1，无平衡操作
        Node findNode = tree.find(30);
        System.out.println(findNode);
        System.out.println(tree.preOrderTraversal());
        System.out.println(tree.inOrderTraversal());
        System.out.println(tree.postOrderTraversal());
    }

}

