package com.moqifei.rpc.single;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.Socket;

public class SimpleRpcServerHandler implements Runnable {
    private final Socket socket;
    private final Object servcie;

    public SimpleRpcServerHandler(final Socket socket, Object servcie) {
        this.socket = socket;
        this.servcie = servcie;
    }


    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
            try {
                //获取方法名称
                String methodName = inputStream.readUTF();
                //获取参数类型
                Class<?>[] parameterTypes = (Class<?>[]) inputStream.readObject();
                //获取参数数组
                Object[] arguments = (Object[]) inputStream.readObject();
                //反射执行方法
                Method method = servcie.getClass().getDeclaredMethod(methodName, parameterTypes);
                Object result = method.invoke(servcie, arguments);
                //执行结果返回远程调用方
                outputStream.writeObject(result);
            } catch (Throwable t) {
                //返回报错信息
                outputStream.writeObject(t);
            } finally {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

