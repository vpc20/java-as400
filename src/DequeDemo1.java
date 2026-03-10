import java.util.ArrayDeque;
import java.util.Deque;

public class DequeDemo1 {
    static void main() {
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.push(4);
        System.out.println(stack);
        stack.pop();
        System.out.println(stack);
        System.out.println(stack.peek());  // peek returns top of stack
    }
}
