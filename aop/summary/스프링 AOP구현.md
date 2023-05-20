# 스프링 AOP 구현

## 예제 프로젝트

```java
@Slf4j
@Repository
public class OrderRepository {

    public String save(String itemId) {
        log.info("[orderRepository] 실행");

        if(itemId.equals("ex")) {
            throw new IllegalStateException("예외 발생!");
        }

        return "ok";
    }
}


@Slf4j
@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void orderItem(String itemId) {
        log.info("[orderService] 실행");
        orderRepository.save(itemId);
    }
}
```

## 스프링 AOP 구현

`@Aspect`를 사용해서 간단한 AOP를 구현

```java
@Slf4j
@Aspect
public class AspectV1 {

    @Around("execution(* hello.aop.order..*(..))")
    public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[log] {}", joinPoint.getSignature());
        return joinPoint.proceed();
    }
}
```

`@Around` 에 사용한 포인트컷 표현식으로 인해 order 하위 클래스의 모든 메서드에 포인트컷이 적용된다.

`OrderService`, `OrderRepository`의 모든 메서드는 AOP 적용 대상이 된다.

스프링 빈으로 등록 하는 방법

- `@Bean`
- `@Component`
- `@Import`

`@Import`는 주로 설정 파일을 추가할 때 사용하지만, 스프링 빈으로 등록할 수도 있다.

```java
@Slf4j
@SpringBootTest
@Import(AspectV1.class)
public class AopTest {
    //...
}
```

## 스프링 AOP 구현2 - 포인트컷 분리

`@Around`에 포인트컷 표현식을 직접 넣어도 되고, `@Pointcut` 애노테이션을 사용해서 분리할 수 있다.

```java
@Slf4j
@Aspect
public class AspectV2 {


    //hello.aop.order 패키지와 하위 패키지
    @Pointcut("execution(* hello.aop.order..*(..))")
    private void allOrder() {} //pointcut signature


    @Around("allOrder()")
    public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[log] {}", joinPoint.getSignature());
        return joinPoint.proceed();
    }
}
```

**@Pointcut**

- `@Pointcut`에 포인트컷 표현식을 사용
- 메서드 이름과 파라미터를 합쳐서 포인트컷 시그니쳐(signature)라 한다.
- 메서드의 반환타입은 **void**
- 위 예제에서 포인트컷 시그니쳐는 `allOrder()`
- `@Around`에서는 포인트컷 표현식이 아닌, 포인트컷 시그니쳐를 사용해도 된다.
  - @Around("allOrder()")
- 다른 애스펙트에서 사용하려면 `public`을 사용

## 스프링 AOP 구현3 - 어드바이스 추가

로그를 출력하는 기능에 추가로 트랜잭션을 적용하는 코드 추가.

진짜 트랜잭션이 아닌 기능이 동작하는 것 처럼 로그만 남기는 코드

트랜잭션 기능의 동작

- 핵심 로직 실행 직전에 트랜잭션 시작
- 로직 실행
- 문제가 없으면 커밋
- 문제가 있으면 롤백

```java
@Slf4j
@Aspect
public class AspectV3 {


    //hello.aop.order 패키지와 하위 패키지
    @Pointcut("execution(* hello.aop.order..*(..))")
    private void allOrder() {} //pointcut signature

    //클래스 이름 패턴이 *Service
    @Pointcut("execution(* *..*Service.*(..))")
    private void allService(){}


    @Around("allOrder()")
    public Object doLog(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("[log] {}", joinPoint.getSignature());
        return joinPoint.proceed();
    }

    //hello.aop.order 패키지와 하위 패키지 이면서 클래스 이름 패턴이 *Service
    @Around("allOrder() && allService()")
    public Object doTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            log.info("[트랜잭션 시작] {}", joinPoint.getSignature());
            Object result = joinPoint.proceed();
            log.info("[트랜잭션 커밋] {}", joinPoint.getSignature());
            return result;
        } catch (Exception e) {
            log.info("[트랜잭션 롤백] {}", joinPoint.getSignature());
            throw e;
        } finally {
            log.info("[리소르 릴리즈] {}", joinPoint.getSignature());
        }
    }
}
```

`allOrder()`는 `hello.aop.order`패키지와 하위 패키지를 대상으로 한다.

`allService()`는 포인트컷 타입 이름 패턴이 `*Service`를 대상으로 한다.

**타입 이름**: 클래스, 인터페이스에 모두 적용

`@Around("allOrder() && allService()")`

- 포인트컷 조합. &&, ||, ! 3가지 조합 가능
- 위 예제는 `hello.aop.order` 패캐지와 하위 패키지 이면서 타입 이름이 `*Service`인 것을 대상으로 한다.

doTransaction() 어드바이스는 OrderService에만 적용된다.


`AspectV3` 적용 후 

클라이언트 -> `[doLog() -> doTransaction()]`-> `orderService.orderItem()` -> `[doLog()]` -> `[orderRepository.save()]`

orderService에는 doLog(), doTransaction() 어드바이스가 적용되고 orderRepository에는 doLog() 어드바이스 하나만 적용된다.

```text
2023-05-20 21:16:32.201  INFO 4142 --- [    Test worker] hello.aop.order.aop.AspectV3             : [log] void hello.aop.order.OrderService.orderItem(String)
2023-05-20 21:16:32.202  INFO 4142 --- [    Test worker] hello.aop.order.aop.AspectV3             : [트랜잭션 시작] void hello.aop.order.OrderService.orderItem(String)
2023-05-20 21:16:32.208  INFO 4142 --- [    Test worker] hello.aop.order.OrderService             : [orderService] 실행
2023-05-20 21:16:32.208  INFO 4142 --- [    Test worker] hello.aop.order.aop.AspectV3             : [log] String hello.aop.order.OrderRepository.save(String)
2023-05-20 21:16:32.211  INFO 4142 --- [    Test worker] hello.aop.order.OrderRepository          : [orderRepository] 실행
2023-05-20 21:16:32.212  INFO 4142 --- [    Test worker] hello.aop.order.aop.AspectV3             : [트랜잭션 커밋] void hello.aop.order.OrderService.orderItem(String)
2023-05-20 21:16:32.212  INFO 4142 --- [    Test worker] hello.aop.order.aop.AspectV3             : [리소르 릴리즈] void hello.aop.order.OrderService.orderItem(String)
```
