import service.HelloService;
import center.ServerCenter;
import service.impl.HelloServiceImpl;
import center.ServerCenterImpl;

/**
 * @author chenchuancheng
 * @since 2021/11/10 15:32
 */
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
