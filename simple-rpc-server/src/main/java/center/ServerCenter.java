package center;

import java.io.IOException;

/**
 * @author chenchuancheng
 * @since 2021/11/10 10:46
 */
public interface ServerCenter {

    void stop();

    void start() throws IOException;

    void register(Class serviceInterface, Class impl);

    boolean isRunning();

    int getPort();

}
