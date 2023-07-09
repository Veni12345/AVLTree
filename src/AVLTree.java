import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 实现一个自平衡的二叉搜索树，树的数据结构和节点数据保存在磁盘上
 */
public class AVLTree {

    private Node root;
    private int nodeSize;
    private static int dataSize;
    private static final Map<Integer, Node> cache = new HashMap<>();

    public AVLTree() {
        root = null;
        nodeSize = 0;
        dataSize = 0;
    }

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
                if (key < current.getKey()) {
                    current = current.getLeft();
                    if (current == null) {
                        parent.setLeft(newNode);
                        updateNode(parent);
                        updateNode(newNode);
                        break;
                    }
                } else {
                    current = current.getRight();
                    if (current == null) {
                        parent.setRight(newNode);
                        updateNode(parent);
                        updateNode(newNode);
                        break;
                    }
                }
            }
            //平衡二叉搜索树
            balance(newNode);
        }
        nodeSize++;
    }

    /**
     * 根据节点大小遍历
     *
     * @param key
     * @return
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
     * 节点删除 再平衡
     *
     * @param key
     * @return
     * @throws IOException
     */
    public boolean delete(int key) throws IOException {
        Node current = root;
        Node parent = root;
        boolean isLeftChild = true;   //当前节点是否为其父节点的左子节点

        //查找节点
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
            //需再平衡
            balance(successor);
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

    /**
     * 平衡二叉搜索树
     * 根据节点左右子树的高度是否 >1
     *
     * @param node
     * @throws IOException
     */
    private void balance(Node node) throws IOException {
        while (node != null) {
            int leftHeight = node.getLeft() == null ? 0 : node.getLeft().getHeight();
            int rightHeight = node.getRight() == null ? 0 : node.getRight().getHeight();
            if (Math.abs(leftHeight - rightHeight) > 1) {
                node = rotate(node, leftHeight > rightHeight);
            } else {
                //无需平衡时，插入相应节点后，更新节点高度信息
                updateHeight(node);
                node = node.getParent();
            }
        }
    }

    /**
     * 旋转节点
     *
     * @param node
     * @param isLeftChild
     * @return
     * @throws IOException
     */
    private Node rotate(Node node, boolean isLeftChild) throws IOException {
        Node parent = node.getParent();
        //H(L)>H(R) -->child==N(L)
        Node child = isLeftChild ? node.getLeft() : node.getRight();
        //是否需要再次旋转
        boolean isChildLeftChild = child.getLeft() != null;
        boolean isChildRightChild = child.getRight() != null;
        if (isLeftChild) {
            if (isChildRightChild) {
                //LR
                Node grandChild = child.getRight();
                child.setRight(grandChild.getLeft());
                node.setLeft(grandChild.getRight());
                grandChild.setLeft(child);
                grandChild.setRight(node);
                //设置新的旋转后的节点
                parent.setLeft(grandChild);
            } else {
                //左节点高度 > 右节点高度  -->LL
                node.setLeft(child.getRight());
                child.setRight(node);
            }
        } else {
            if (isChildLeftChild) {
                //RL
                Node grandChild = child.getLeft();
                child.setLeft(grandChild.getRight());
                node.setRight(grandChild.getLeft());
                grandChild.setLeft(node);
                grandChild.setRight(child);
                //设置新的旋转后的节点
                parent.setRight(grandChild);
            } else {
                //H(L) < H(R)   -->RR
                node.setRight(child.getLeft());
                child.setLeft(node);
            }
        }

        //节点位置是否是根节点，设置节点关系
        if (parent == null) {
            root = child;
            child.setParent(null);
        } else if (node == parent.getLeft()) {
            parent.setLeft(child);
            child.setParent(parent);
        } else if (node == parent.getRight()) {
            parent.setRight(child);
            child.setParent(parent);
        }

        //更新节点操作
        updateHeight(node);
        updateHeight(child);
        updateHeight(parent);
        updateNode(node);
        updateNode(child);
        updateNode(parent);
        return child;
    }

    /**
     * 添加更新节点高度
     *
     * @param node
     */
    private void updateHeight(Node node) {
        int leftHeight = node.getLeft() == null ? 0 : node.getLeft().getHeight();
        int rightHeight = node.getRight() == null ? 0 : node.getRight().getHeight();
        node.setHeight(Math.max(leftHeight, rightHeight) + 1);
    }


    /**
     * 添加节点 将节点序列化到磁盘中
     *
     * @param node
     * @throws IOException
     */
    public static void updateNode(Node node) throws IOException {
        if (node == null) {
            return;
        }
        if (cache.containsKey(node.getKey())) {
            cache.replace(node.getKey(), node);
        } else {
            if (dataSize == 0) {
                node.setPosition(0);
            } else {
                node.setPosition((int) (new File("avlData.dat").length() / dataSize) * dataSize);
            }
            cache.put(node.getKey(), node);
            RandomAccessFile raf = new RandomAccessFile("avlData.dat", "rw");
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

    public int getNodeSize() {
        return nodeSize;
    }


    public static void main(String[] args) throws IOException {
        AVLTree tree = new AVLTree();
        tree.insert(50, "data 50");
        tree.insert(30, "data 30");
        tree.insert(70, "data 70");
        tree.insert(20, "data 20");
        tree.insert(40, "data 40");
        tree.insert(60, "data 60");
        tree.insert(80, "data 80");
        tree.insert(90, "data 90");
        tree.insert(100, "data 100");  //节点添加需进行平衡操作 -- RR旋
        tree.insert(85, "data 85");  //节点添加需进行平衡操作 -- RL旋

        tree.insert(10, "data 10");
        tree.insert(5, "data 5");  //节点添加需进行平衡操作 -- LL旋
        tree.insert(15, "data 15");  //节点添加需进行平衡操作 -- LR旋

        //删除节点
//        tree.delete(20);
//        tree.delete(90);

        //根据遍历直接输出节点
        System.out.println(tree.preOrderTraversal());
        System.out.println(tree.inOrderTraversal());
        System.out.println(tree.postOrderTraversal());
        //打印树结构
        tree.print();
        //直观地展示树的结构
        printTreeVisually(tree.root);

        //查找
        Node findNode = tree.find(90);
        System.out.println(findNode);

    }


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


    /**
     * 打印"二叉查找树"
     *
     * @param tree
     * @param key       节点的键值
     * @param direction --  0，表示该节点是根节点;
     *                  -1，表示该节点是它的父结点的左孩子;
     *                  1，表示该节点是它的父结点的右孩子。
     */
    private void print(Node tree, Integer key, int direction) {
        if (tree != null) {
            if (direction == 0)    // tree是根节点
                System.out.printf("%2d is root\n", tree.getKey());
            else                // tree是分支节点
                System.out.printf("%2d is %2d's %6s child\n", tree.getKey(), key, direction == 1 ? "right" : "left");
            print(tree.getLeft(), tree.getKey(), -1);
            print(tree.getRight(), tree.getKey(), 1);
        }
    }

    public void print() {
        if (root != null)
            print(root, root.getKey(), 0);
    }

    public static void printTreeVisually(Node root) {
        printTreeHelper(root, 0);
    }

    private static void printTreeHelper(Node node, int level) {
        if (node == null) {
            return;
        }

        printTreeHelper(node.getRight(), level + 1);

        for (int i = 0; i < level; i++) {
            System.out.print("   ");
        }
        System.out.println(node.getKey());
        printTreeHelper(node.getLeft(), level + 1);
    }


}

    