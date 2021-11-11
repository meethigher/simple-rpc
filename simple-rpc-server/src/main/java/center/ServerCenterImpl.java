package center;



import center.ServerCenter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author chenchuancheng
 * @since 2021/11/10 10:49
 */
public class ServerCenterImpl implements ServerCenter {
    private int port;

    private static boolean isRunning=false;

    private static final HashMap<String,Class> serviceRegistry=new HashMap<String, Class>();

    private static ExecutorService executor= Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    public ServerCenterImpl(int port) {
        this.port=port;
    }

    public void stop() {
        isRunning=false;
        executor.shutdown();

    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(port));
        System.out.println("启动Socket服务器");
        try {
            while(true) {
                Socket client = serverSocket.accept();
                System.out.println("收到新的调用"+new Date());
                executor.execute(new ServiceTask(client));
            }
        }finally {
            serverSocket.close();
        }
    }

    public void register(Class serviceInterface, Class impl) {
        serviceRegistry.put(serviceInterface.getName(), impl);
    }

    public boolean isRunning() {
        return false;
    }

    public int getPort() {
        return 0;
    }


    class ServiceTask implements Runnable {
        Socket client=null;

        public ServiceTask(Socket client) {
            this.client = client;
        }

        public void run() {
            ObjectInputStream inputStream=null;
            ObjectOutputStream outputStream=null;

            try {
                //将客户端发送的码流反序列化成对象，反射调用服务实现着，获取执行结果
                inputStream=new ObjectInputStream(client.getInputStream());

                //读取客户端传过来的 类名、方法名称、参数类型、参数值
                String serviceName = inputStream.readUTF();
                String methodName = inputStream.readUTF();
                Class<?>[] parameterTypes = (Class<?>[]) inputStream.readObject();
                Object[] arguments = (Object[]) inputStream.readObject();

                Class serviceClass = serviceRegistry.get(serviceName);
                if(serviceClass==null) {
                    throw new ClassNotFoundException(serviceName+" not found");
                }
                Method method = serviceClass.getMethod(methodName, parameterTypes);
                Object result = method.invoke(serviceClass.newInstance(), arguments);
                //将结果反序列化，通过socket发送给客户端
                outputStream=new ObjectOutputStream(client.getOutputStream());
                outputStream.writeObject(result);
            }catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(outputStream!=null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(inputStream!=null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(client!=null) {
                    try {
                        client.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


}
