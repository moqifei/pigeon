上一节，我们自上而下，抽丝破茧般地分析了实现一个单机版RPC框架所需的要素。本着“Talk is cheap. Show me the code.”的精神，本人实现了一个简易可运行的单机版RPC框架 [pigeon](https://github.com/moqifei/pigeon/tree/master)，请大家指正。

## 传输模块

​		让我们看一下pigeon的代码，以Server端为例，pigeon选择使用Java原生的Socket作为网络传输协议，对应的网络IO模型是BIO。

```java
 ServerSocket serverSocket = new ServerSocket(port);
 for (; ;) {
     final Socket socket = serverSocket.accept();
     new Thread(new SimpleRpcServerHandler(socket, servcie)).start();
 }
```

上一节我们分析过，RPC框架为了追求极致的性能体验，首选的网络IO模型是IO多路复用，因此针对网络传输我们有必要将Socket替换为Netty。更进一步的，考虑到pigeon目前代码基本没有模块化，有必要针对网络传输（Netty）封装一个单独的模块，可称为传输模块。

## 协议模块

​		调用过程中交互双方通过网络传递信息的约定“语义”即为协议，让我们看看pigeon的交互协议部分是怎么实现的

Client端

```java
//写入方法名称
outputStream.writeUTF(method.getName());
//写入方法入参类型
outputStream.writeObject(method.getParameterTypes());
//写入方法参数
outputStream.writeObject(args);
//读取返回结果
Object result = inputStream.readObject();
```

Server端

```java
//读取方法名称
String methodName = inputStream.readUTF();
//读取方法入参类型
Class<?>[] parameterTypes = (Class<?>[]) inputStream.readObject();
//读取参数数组
Object[] arguments = (Object[]) inputStream.readObject();
//写入执行结果
outputStream.writeObject(result);
```

可以看到，我们是通过硬编码的方式实现pigeon的交互协议，传递的信息仅仅是方法名称，参数类型，及参数这三类内容，从扩展性及用户易用性角度讲显然是有待重构的。与此同时，通过代码我们可以看到pigeon使用java原声的ObjectInputStream、ObjectOutputStream进行序列化及反序列化操作，从性能角度上看显然是低效的。因此有必要创建一个协议模块完成协议格式的定义，序列化/反序列化算法的封装，更进一步的考虑对报文的压缩与解压缩等

## 调用集成

​		RPC的初心是实现像调用本地一样地调用远程，让我们看看目前pigeon是如何发起RPC调用的。

Server端

```java
 HelloWorldService helloWorldService = new HelloWorldServiceImpl();
 SimpleRpcServer rpcServer = new SimpleRpcServer();
 rpcServer.reply(helloWorldService, 8080);
```

Client端

```java
 SimpleRpcClient rpcClient = new SimpleRpcClient();
 HelloWorldService helloWorldService = rpcClient.call(HelloWorldService.class, "127.0.0.1", 8080);
 String result = helloWorldService.sayHello("Hello World!");
```

​		显然，实际项目开发中我们几乎不用new的方式创建对象，绝大多数项目使用Spring技术栈，我们的目标是将RPC调用的入口集成到Spring容器中，通过依赖注入的方式引用，因此需要一个集成调用模块。

## 集群模块

​		显然，作为单机版的RPC框架，pigeon并没有关于集群模块的代码。所谓集群，简单理解就是对于同一个服务，有多个服务提供者，而调用方无需关心这些事情，仍然能够像调用本地方法一样透明地调用远程服务。为了能够让调用方透明地发起RPC调用，集群模块必须维护一套服务与服务提供方地址之间的映射关系，以便发调用方发起请求时能够扎到对应的服务提供者，这就是RPC领域里的“服务发现”。

​		除了服务发现，集群模块还需要考虑对长连接的管理以提升网络性能，实现一个负载均衡算法以高效地从服务提供者中选择某一个服务发起调用，以及一些配置和容错性管理工作

​		基于上述分析，一个比较完善的RPC框架大致如下

![RPC框架1](E:\aiWriting\RPC\RPC框架1.png)

## 架构的扩展性

​		一图在手，天下我有，你按照RPC的架构图，选择了合适的开源技术框架，实现了上述四个模块，跑通了测试案列，并开始在你的团队内部小范围推广使用，成就感爆棚。

​		然而，你的Leader看了你的RPC实现后，提出了一个小小的建议，他说啊：

> 年轻人干得不错，有落地能力值得表扬，这里提出一个小小的建议，供你参考：传输模块能否在支持TCP传输的同时也支持HTTP传输，并让使用方按需选择？此外，序列化算法和负载均衡算法都太少了，我看组里的XX美眉潜力很大，要不要你带带她一起丰富一下RPC框架的功能。

听到建议的你如接圣旨，开始思考怎么改，代码里写一堆if else显得太弱鸡了。此时你不禁想起软件设计原则中的面向接口编程原则，能否将各个模块要实现的功能定义为接口，这个接口作为模块的锲约，后续的接口实现都需要遵循这个契约。而RPC框架使用者可以按需选择不同的模块实现，模块做到即插即用，如果现有实现不满足要求，使用者还可以按照接口约定进行自定义开发。这个过程就是RPC框架的插件化。

​		如何实现插件化，我们可以看看Java自带的SPI（Service Provider Interface）服务发现机制，SPI是一套用来被第三方实现或者扩展的接口，可以用来启动框架扩展和替换插架，SPI的作用就是能够自动帮被扩展的接口寻找服务实现。

**Java SPI使用约定**

1、当服务提供者提供了接口的一种具体实现后，在jar包的META-INF/services目录下创建一个以“接口全限定名”为命名的文件，内容为实现类的全限定名；
2、接口实现类所在的jar包放在主程序的classpath中；
3、主程序通过java.util.ServiceLoder动态装载实现模块，它通过扫描META-INF/services目录下的配置文件找到实现类的全限定名，把类加载到JVM；
4、SPI的实现类必须携带一个不带参数的构造方法；

**Java SPI使用举例**

首先定义一个打招呼的接口实现

```java
package com.moqifei.rpc.spi;

public interface HelloWorld {
    public void sayHello();
}
```

该接口有两个实现，分别是通过中文及英文打招呼

```java
package com.moqifei.rpc.spi.impl;

import com.moqifei.rpc.spi.HelloWorld;

public class ChineseHelloWorld implements HelloWorld {
    @Override
    public void sayHello() {
        System.out.println("你好！");
    }
}
```

```java
package com.moqifei.rpc.spi.impl;

import com.moqifei.rpc.spi.HelloWorld;

public class EnglishHelloWorld implements HelloWorld {
    @Override
    public void sayHello() {
        System.out.println("Hello!");
    }
}
```

在META-INF/services目录下，创建一个与HellWorld的全限定一致的文件，并在文件中写入HelloWorld实现类的全限定名称

![image-20210310134100700](E:\aiWriting\RPC\spi.png)

最后，通过ServiceLoader加载实现类并调用：

```java
public class HelloWorldMain {
    public static void main(String[] args){
        ServiceLoader<HelloWorld> helloWorlds =       
            ServiceLoader.load(HelloWorld.class);
        Iterator<HelloWorld> iterator = helloWorlds.iterator();
        while(iterator.hasNext()){
            HelloWorld helloWorld = iterator.next();
            helloWorld.sayHello();
        }
    }
}
```

数据结果如下：

> 你好！
> Hello!

**Java SPI原理解析**

通过追踪源码，我们对SPI原理进行分析：

```java
ServiceLoader<HelloWorld> helloWorlds = ServiceLoader.load(HelloWorld.class);
```

load方法如下，传入当前线程的类加载器，及class对象，创建新的ServiceLoader对象

```java
public static <S> ServiceLoader<S> load(Class<S> service,ClassLoader loader)
{
	return new ServiceLoader<>(service, loader);
}
```

在ServiceLoader构造函数中，创建一个LazyIteraotr的实例lookupIterator

```java
public void reload() {
    providers.clear();
    lookupIterator = new LazyIterator(service, loader);
}
```

LazyIterator是一个私有的内部类，上述调用到此结束，我们还没有真正触及到魔法部分，让我们把目光回到使用过程

```java
ServiceLoader<HelloWorld> helloWorlds = ServiceLoader.load(HelloWorld.class);
Iterator<HelloWorld> iterator = helloWorlds.iterator();
while(iterator.hasNext()){
     HelloWorld helloWorld = iterator.next();
     helloWorld.sayHello();
}
```

我们获取了Helloworld的迭代器，并通过hasNext（）方法遍历，使用next（）获得Helloworld实例，让我们看看ServiceLoader类中对Iterator的封装

```java
 public Iterator<S> iterator() {
        return new Iterator<S>() {

            Iterator<Map.Entry<String,S>> knownProviders
                = providers.entrySet().iterator();

            public boolean hasNext() {
                if (knownProviders.hasNext())
                    return true;
                return lookupIterator.hasNext();
            }

            public S next() {
                if (knownProviders.hasNext())
                    return knownProviders.next().getValue();
                return lookupIterator.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }
```

可以看到，当调用hasNext方法时，实际上调用了我们上面创建的lookupIterator的hasNext方法, 该方法最终是扫描META-INF/services目录下的配置文件并parse其中的所有service名称

```java
private boolean hasNextService() {
            if (nextName != null) {
                return true;
            }
            if (configs == null) {
                try {
                    String fullName = PREFIX + service.getName();
                    if (loader == null)
                        configs = ClassLoader.getSystemResources(fullName);
                    else
                        configs = loader.getResources(fullName);
                } catch (IOException x) {
                    fail(service, "Error locating configuration files", x);
                }
            }
            while ((pending == null) || !pending.hasNext()) {
                if (!configs.hasMoreElements()) {
                    return false;
                }
                pending = parse(service, configs.nextElement());
            }
            nextName = pending.next();
            return true;
        }
```

当调用next()方法时，实际上调用了lookupIterator的next方法，该发放最终将配置文件中的所有配置的类，通过反射加载，实例化并存储到缓存中

```java
private S nextService() {
            if (!hasNextService())
                throw new NoSuchElementException();
            String cn = nextName;
            nextName = null;
            Class<?> c = null;
            try {
                c = Class.forName(cn, false, loader);
            } catch (ClassNotFoundException x) {
                fail(service,
                     "Provider " + cn + " not found");
            }
            if (!service.isAssignableFrom(c)) {
                fail(service,
                     "Provider " + cn  + " not a subtype");
            }
            try {
                S p = service.cast(c.newInstance());
                providers.put(cn, p);
                return p;
            } catch (Throwable x) {
                fail(service,
                     "Provider " + cn + " could not be instantiated",
                     x);
            }
            throw new Error();          // This cannot happen
        }
```

**Java SPI的局限性**

通过上述代码分析，我们了解Java SPI的实现原理

- 虽然ServiceLoader也算是使用的延迟加载，但是基本只能通过遍历全部获取，也就是接口的实现类全部加载并实例化一遍。如果你并不想用某些实现类，它也被加载并实例化了，这就造成了浪费。
- 获取某个实现类的方式不够灵活，只能通过Iterator形式获取，不能根据某个参数来获取对应的实现类。
- 多个并发多线程使用ServiceLoader类的实例是不安全的。
- 使用线程上下文加载类，也要注意保证多个需要通信的线程间的类加载器应该是同一个，防止因为不同的类加载器导致类型转换异常(ClassCastException)。
- 扩展如果依赖其他的扩展，做不到自动注入和装配，比如扩展里面依赖了一个Spring bean，原生的Java SPI不支持。

**SPI机制的增强**

1.dubbo SPI

dubbo SPI有如下几个概念：

  （1）**扩展点**：一个接口。

  （2）**扩展**：扩展（接口）的实现。

  （3）**扩展自适应实例：**其实就是一个Extension的代理，它实现了扩展点接口。在调用扩展点的接口方法时，会根据实际的参数来决定要使用哪个扩展。dubbo会根据接口中的参数，自动地决定选择哪个实现。

  （4）**@SPI**:该注解作用于扩展点的接口上，表明该接口是一个扩展点。

  （5）**@Adaptive：**@Adaptive注解用在扩展接口的方法上。表示该方法是一个自适应方法。Dubbo在为扩展点生成自适应实例时，如果方法有@Adaptive注解，会为该方法生成对应的代码。

具体实现原理，可参考[dubbo](https://github.com/apache/dubbo)源码

2.hmily SPI

除dubbo SPI外，个人推荐分布式事务开源框架[hmily](https://github.com/dromara/hmily/tree/master/hmily-spi)中的SPI代码机制，通过注解的方式，指定名称按需加载SPI扩展实现，更为灵活且容易上手，感兴趣的同学可以通过源码了解其实现原理，然后将其移植到你的RPC框架代码中。

## 结语

​		本节我们从单机版RPC框架pigeon入手，分析了功能完备具有集群能力的RPC框架应该有哪些模块，以及为了后续RPC框架能够在满足开闭原则下进行按需迭代扩展，介绍了SPI机制和对SPI机制的增强实现，基于以上信息，我们可以得出一个具有一定扩展性的RPC框架

![image-20210310151353179](E:\aiWriting\RPC\RPC2.png)

​		下面，让我们大刀阔斧地对pigeon进行重构，向着具备集群能力且扩展无限可能的RPC框架前进吧！