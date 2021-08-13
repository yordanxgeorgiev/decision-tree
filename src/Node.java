import java.util.ArrayList;

public class Node {
    private String value;
    private Node prev;
    private Node[] children;
    private String attribute;

    public Node(String value, Node prev, Node[] children)
    {
        this.value = value;
        this.prev = prev;
        this.children = children;
    }

    public String getValue() {
        return value;
    }

    public Node getPred() {
        return prev;
    }

    public Node[] getChildren() {
        return children;
    }

    public void setChildren(Node[] children) {
        this.children = children;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void print(int level) {
        for (int i = 1; i < level; i++) {
            System.out.print("\t");
        }
        System.out.println(value);
        if (children != null
        ) {
            for (Node child : children) {
                if(child!= null)
                child.print(level + 1);
            }

        }
    }



}
