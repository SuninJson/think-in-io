import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioHandler implements IOHandler {

    private static final int PORT = 8080;

    private Selector selector;
    private ByteBuffer buffer = ByteBuffer.allocate(1024);

    public NioHandler() {
        init();
    }

    public void init() {
        try {
            //初始化服务端的socket通道
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(PORT));
            serverSocketChannel.configureBlocking(false);
            //初始化服务端的轮询器
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        System.out.println("listen on " + PORT + ".");

        //不断轮询
        while (true) {
            try {
                //开始轮询
                selector.select();
                //拿到本次轮询的所有号码
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();
                    keyIterator.remove();
                    //处理每一个号码对应的IO请求
                    doHandle(selectionKey);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void doHandle(SelectionKey selectionKey) {
        try {
            //根据请求端状态进行不同的处理
            if (selectionKey.isAcceptable()) {
                //获取本次询问IO请求Key对应的服务端Socket通道
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
                //获取请求端Socket通道
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                //将请求端Socket通道状态修改为可读
                socketChannel.register(selector, SelectionKey.OP_READ);
            } else if (selectionKey.isReadable()) {
                SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                int len = socketChannel.read(buffer);
                if (len > 0) {
                    buffer.flip();
                    String content = new String(buffer.array(), 0, len);
                    selectionKey = socketChannel.register(selector, SelectionKey.OP_WRITE);
                    //在key上携带一个附件，一会再写出去
                    selectionKey.attach(content);
                    System.out.println("读取内容：" + content);
                }
            } else if (selectionKey.isWritable()) {
                SocketChannel channel = (SocketChannel) selectionKey.channel();
                String content = (String) selectionKey.attachment();
                channel.write(ByteBuffer.wrap(("输出：" + content).getBytes()));
                channel.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new NioHandler().listen();
    }
}
