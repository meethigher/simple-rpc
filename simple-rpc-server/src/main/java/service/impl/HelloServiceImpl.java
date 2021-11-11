package service.impl;


import service.HelloService;

/**
 * @author chenchuancheng
 * @since 2021/11/10 10:45
 */
public class HelloServiceImpl implements HelloService {
    public String sayHi(String name) {
        return "Hi, " + name;
    }

    public String buyMix4(Integer money,String name) {
        return "从"+name+"手中花费"+money+"元买到MIX4 12G+256G";
    }
}
