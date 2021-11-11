package client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author chenchuancheng
 * @since 2021/11/10 14:54
 */
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
                            //将远程服务调用所需的接口类、方法名、参数类型、参数值编码后发送给服务提供者
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
