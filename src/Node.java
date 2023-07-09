
import java.io.IOException;


public class Node {
    private final int key;
    private String data;
    private Node left;
    private Node right;
    private Node parent;
    private int height;
    private int position;

    public Node(int key, String data) {
        this.key = key;
        this.data = data;
        left = null;
        right = null;
        parent = null;
        height = 1;
        position = -1;
    }

    public int getKey() {
        return key;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) throws IOException {
        this.data = data;
        AVLTree.updateNode(this);
    }

    public Node getLeft() {
        return left;
    }

    public void setLeft(Node left) {
        this.left = left;
        if (left != null) {
            left.setParent(this);
        }
    }

    public Node getRight() {
        return right;
    }

    public void setRight(Node right) {
        this.right = right;
        if (right != null) {
            right.setParent(this);
        }
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Node{" +
                "key=" + key +
                ", data='" + data + '\'' +
                ", height=" + height +
                '}';
    }
}

//public class Node {
//    private int key;
//    private String data;
//    private int height;
//    private Node left;
//    private Node right;
//    private int position;
//
//    public Node(int key, String data) {
//        this.key = key;
//        this.data = data;
//        this.height = 1;
//        this.left = null;
//        this.right = null;
//        this.position = -1;
//    }
//
//    public int getKey() {
//        return key;
//    }
//
//    public void setKey(int key) {
//        this.key = key;
//    }
//
//    public String getData() {
//        return data;
//    }
//
//    public void setData(String data) {
//        this.data = data;
//    }
//
//    public int getHeight() {
//        return height;
//    }
//
//    public void setHeight(int height) {
//        this.height = height;
//    }
//
//    public Node getLeft() {
//        return left;
//    }
//
//    public void setLeft(Node left) {
//        this.left = left;
//    }
//
//    public Node getRight() {
//        return right;
//    }
//
//    public void setRight(Node right) {
//        this.right = right;
//    }
//
//    public int getPosition() {
//        return position;
//    }
//
//    public void setPosition(int position) {
//        this.position = position;
//    }
//}


    