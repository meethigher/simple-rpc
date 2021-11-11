import client.RPCClient;
import service.HelloService;

import java.net.InetSocketAddress;

/**
 * @author chenchuancheng
 * @since 2021/11/10 15:10
 */
public class RPCTest {
    public static void main(String[] args) {
        //获取动态代理对象，调用某个方法时，会由动态代理，去调用远程方法，通过socket将远程方法的返回值获取到
        HelloService service= RPCClient.getRemoteProxyObj(HelloService.class,new InetSocketAddress("localhost",1234));
        System.out.println(service.sayHi("雷军"));
        System.out.println(service.buyMix4(4099,"雷军"));
    }
}
