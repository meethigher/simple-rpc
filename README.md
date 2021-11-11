本文源码[meethigher/simple-rpc](https://github.com/meethigher/simple-rpc)

抄袭自[Java实现简单的RPC框架 - 苍穹2018 - 博客园](https://www.cnblogs.com/codingexperience/p/5930752.html)

其他参考

1. [RPC系列：基本概念 - 海米傻傻 - 博客园](https://www.cnblogs.com/haimishasha/p/11573512.html)
2. [java RMI原理详解_xinghun_4的专栏-CSDN博客_rmi](https://blog.csdn.net/xinghun_4/article/details/45787549)
3. [Java RMI与RPC的区别 - Silentdoer - 博客园](https://www.cnblogs.com/silentdoer/p/8963645.html)
4. [RMI和RPC比较_Baron-CSDN博客](https://blog.csdn.net/Baronrothschlid/article/details/95021955)

# 一、RPC概念

RPC，全称是Remote Procedure Call，远程过程调用。

RPC是一种技术思想，通过网络从远程计算机程序上调用服务，而不需要去了解底层网络技术。

常见的RPC技术和框架

* 应用级的服务框架：阿里的Dubbo/Dubbox、谷歌的gRPC（基于HTTP2协议）、SpringCloud
* 远程通信协议：RMI、REST、SOAP
* 通信框架：Netty、Miner

RPC能够让本地应用简单、高效地调用服务器中过程，它主要应用在分布式系统。

# 二、RPC实现

使用原生Java实现RPC框架，使用Socket、动态代理、反射。

RPC架构分为三个部分

1. 提供者，运行在服务端，提供Service定义（接口）与ServiceImpl（接口实现类）
2. 注册中心：运行在服务端，将本地服务发布成远程服务，管理远程服务，提供给调用者使用。
3. 调用者：运行在客户端，通过远程代理对象调用远程服务

## 2.1 服务端

使用IDEA创建一个maven，项目，命名为simple-rpc-server

HelloService

```java
public interface HelloService {
    /**
     * 打招呼
     * @param name
     * @return
     */
    String sayHi(String name);

    /**
     * 购买mix4
     * @param money
     * @return
     */
    String buyMix4(Integer money,String name);
}
```

HelloServiceImpl

```java
public class HelloServiceImpl implements HelloService {
    public String sayHi(String name) {
        return "Hi, " + name;
    }

    public String buyMix4(Integer money,String name) {
        return "从"+name+"手中花费"+money+"元买到MIX4 12G+256G";
    }
}

```

ServerCenter

```java
public interface ServerCenter {

    void stop();

    void start() throws IOException;

    void register(Class serviceInterface, Class impl);

    boolean isRunning();

    int getPort();

}
```

ServerCenterImpl

```java
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
```

RPCServerTest

```java
public class RPCServerTest {
    public static void main(String[] args) {
        try {
            ServerCenter serverCenter=new ServerCenterImpl(1234);
            serverCenter.register(HelloService.class, HelloServiceImpl.class);
            serverCenter.start();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

将服务端启动起来，监听1234端口

## 2.2 客户端

使用IDEA创建一个maven，项目，命名为simple-rpc-client

将服务端的Service带过来，不用带ServiceImpl，一般实际使用场景中，会由服务端提供一个Service的jar包。

RPCClient

```java
public class RPCClient<T> {
    public static <T> T getRemoteProxyObj(final Class<?> serviceInterface, final InetSocketAddress addr) {
        /**
         * 将本地接口的调用，转换成JDK动态代理，在动态代理中实现远程调用
         * 三个参数
         * 1. 类加载器：真实对象.getClass().getClassLoader()
         * 2. 接口数组：真实对象.getClass().getInterfaces()，这种写法需要用实现类。如果不想用实现类，就这么搞，new Class[]{serviceInterface}
         * 3. 处理器：new InvocationHandler()，这个就是增强对象的核心方法
         */
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(),
                new Class[]{serviceInterface},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Socket socket=null;
                        ObjectOutputStream outputStream=null;
                        ObjectInputStream inputStream=null;
                        try {
                            //创建socket客户端，根据指定地址连接远程服务提供者
                            socket=new Socket();
                            socket.connect(addr);
                            //将远程服务调用所需的接口类、方法名、参数列表等编码后发送给服务提供者
                            outputStream=new ObjectOutputStream(socket.getOutputStream());
                            outputStream.writeUTF(serviceInterface.getName());
                            outputStream.writeUTF(method.getName());
                            outputStream.writeObject(method.getParameterTypes());
                            outputStream.writeObject(args);
                            //同步阻塞等待服务器返回应答，获取应答后返回
                            inputStream=new ObjectInputStream(socket.getInputStream());
                            return inputStream.readObject();
                        }catch (Exception e) {
                            e.printStackTrace();
                        }finally {
                            if(socket!=null){
                                socket.close();
                            }
                            if(outputStream!=null) {
                                outputStream.close();
                            }
                            if(inputStream!=null) {
                                inputStream.close();
                            }
                        }
                        return null;
                    }
                });
    }
}
```

RPCTest

```java
public class RPCTest {
    public static void main(String[] args) {
        //获取动态代理对象，调用某个方法时，会由动态代理，去调用远程方法，通过socket将远程方法的返回值获取到
        HelloService service= RPCClient.getRemoteProxyObj(HelloService.class,new InetSocketAddress("localhost",1234));
        System.out.println(service.sayHi("雷军"));
        System.out.println(service.buyMix4(4099,"雷军"));
    }
}
```

启动客户端，调用本地1234端口获取代理对象。