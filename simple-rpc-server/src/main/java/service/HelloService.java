package service;
/**
 * @author chenchuancheng
 * @since 2021/11/10 10:44
 */
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
