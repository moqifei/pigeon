package com.moqifei.rpc.single;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class SimpleRpcServer {
    /**
     * @param servcie 被调用服务
     * @param port    服务所在端口
     */
    public void reply(Object servcie, int port) throws IOException {
        if (Objects.isNull(servcie)) {
            throw new IllegalArgumentException("rpc service is null");
        }
        if (port <= 0 || port >= 65535) {
            throw new IllegalArgumentException("invalid port: " + port);
        }
        //使用java自带的Socket
        ServerSocket serverSocket = new ServerSocket(port);
        for (; ;) {
            final Socket socket = serverSocket.accept();
            new Thread(new SimpleRpcServerHandler(socket, servcie)
            ).start();
        }
    }
}
