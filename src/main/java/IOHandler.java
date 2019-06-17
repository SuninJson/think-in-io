import java.nio.channels.SelectionKey;

public interface IOHandler {

    void init();

    void listen();

    void doHandle(SelectionKey selectionKey);
}
