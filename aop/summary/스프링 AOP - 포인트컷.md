# 스프링 AOP - 포인트컷

## 포인트컷 지시자

포인트컷 표현식은 AspectJ pointcut expression 애스펙트J가 제공하는 포인트컷 표현식을 의미한다.

**포인트컷 지시자의 종류**

- `execution`: 메소드 실행 조인 포인트 매칭.
- `within`: 특정 타입 내의 조인 포인트 매칭.
- `args`: 인자가 주어진 타입의 인스턴스인 조인 포인트
- `this`: 스프링 빈 객체(스프링 AOP 프록시)를 대상으로 하는 조인 포인트
- `target`: Target 객체(스프링 AOP 프록시가 가르키는 실제 대상)를 대상으로 하는 조인 포인트
- `@target`: 실행 객체의 클래스에 주어진 타입의 애노테이션이 있는 조인 포인트
- `@within`: 주어진 애노테이션이 있는 타입 내 조인 포인트
- `@annotation`: 메서드가 주어진 애노테이션을 가지고 있는 조인 포인트 매칭
- `@args`: 전달된 실제 인수의 런타임 타입이 주어진 타입의 애노테이션을 갖는 조인 포인트
- `bean`: 스프링 전용 포인트컷 지시자. 빈의 이름으로 포인트컷 지정

## execution

```text
execution(modifiers-pattern? ret-type-pattern declaring-type-pattern?namepattern(param-pattern) throws-pattern?)

execution(접근제어자? 반환타입 선언타입?메서드이름(파라미터) 예외?)


//public(modifiers-pattern?) java.lang.String(return-type-pattern) hello.aop.member.MemberServiceImpl(declaring-type-pattern?).hello(name-pattern)(java.lang.String (param-pattern))
```

메서드 실행 조인 포인트 매칭

?는 생략 가능

'*' 같은 패턴을 지정할 수 있다.

**반환 타입, 메서드 이름: 필수**

```java
@Test
public void exactMatch(){
        //public java.lang.String hello.aop.member.MemberServiceImpl.hello(java.lang.String)
        pointcut.setExpression("execution(public String hello.aop.member.MemberServiceImpl.hello(String))");
        assertThat(pointcut.matches(helloMethod,MemberServiceImpl.class)).isTrue();
        }
```

**매칭 조건**

- 접근제어자?: public
- 반환타입: String
- 선언타입?: hello.aop.member.MemberServiceImpl
- 메서드이름: hello
- 파라미터: (String)
- 예외?: 생략

```java
@Test
public void allMatch(){
        /*
        public 생략
        String: *
        hello.aop.member.MemberServiceImpl (생략)
        hello(): *
        java.lang.String: (..)
         */
        pointcut.setExpression("execution(* *(..))");
        assertThat(pointcut.matches(helloMethod,MemberServiceImpl.class)).isTrue();
        }
```

### 파라미터 매칭

```java
@Test
public void argsMatch(){
        pointcut.setExpression("execution(* *(..))");
        assertThat(pointcut.matches(helloMethod,MemberServiceImpl.class)).isTrue();
}

@Test
public void argsMatchNoArgs(){
        pointcut.setExpression("execution(* *())");
        assertThat(pointcut.matches(helloMethod,MemberServiceImpl.class)).isFalse();
}

@Test
public void argsMatchStar(){
        pointcut.setExpression("execution(* *(*))");
        assertThat(pointcut.matches(helloMethod,MemberServiceImpl.class)).isTrue();
}

@Test
public void argsMatchAll(){
        pointcut.setExpression("execution(* *(..))");
        assertThat(pointcut.matches(helloMethod,MemberServiceImpl.class)).isTrue();
}

@Test
public void argsMatchComplex(){
        pointcut.setExpression("execution(* *(String, ..))");
        assertThat(pointcut.matches(helloMethod,MemberServiceImpl.class)).isTrue();
}
```

**execution 파라미터 매칭 규칙**

- (String): 정확하게 String타입 
- (): 파라미터 없음
- (*): 정확히 하나의 파라미터, 타입 무관
- (*, *): 정확히 두개의 파라미터, 타입 무관
- (..): 파라미터 제한 없음. 모든 타입을 허용, 개수와 무관
- (String, ...): String 타입으로 시작하고 개수와 무관한 모든 타입 허용