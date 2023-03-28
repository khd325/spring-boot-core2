package hello.proxy.config.v1_proxy.interface_proxy;

import hello.proxy.app.v1.OrderRepositoryV1;
import hello.proxy.trace.TraceStatus;
import hello.proxy.trace.logtrace.LogTrace;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderRepositoryInterfaceProxy implements OrderRepositoryV1 {

    private final OrderRepositoryV1 target;
    private final LogTrace logtrace;
    @Override
    public void save(String itemId) {

        TraceStatus status = null;
        try {
            status = logtrace.begin("OrderRepository.save()");

            target.save(itemId);
            logtrace.end(status);
        } catch (Exception e) {
            logtrace.exception(status, e);
            throw e;
        }
    }
}
