# 高屋建瓴，RPC框架应该是什么样子的？

> 会当临绝顶，一览众山小。                               
>
> ​                                                                                                                                     ——杜甫《望岳》

​		假设时光倒流回1995年，此时的你先知先觉，知晓未来几十年RPC框架会在计算机通讯领域大放异彩，你怎么设计并开发一款RPC开源框架，与后来的的Thrift、Dubbo等国内外知名开源框架同场竞技呢？

​		先不要觉得鸭梨山大，毕竟万里长征也是从第一步开始的，先静下心来想想切入点。回想回想你在学校阶段独立完成一个软件开发大作业的日子，你的导师会给你布置一个任务，关于任务的详细描述组成了软件大作业的需求，然后你基于需求进行概要设计与详细设计，最终开发，测试，完成作业交付。此时，我们明确了一个点，需要先找到设计并开发RPC框架任务的详细描述，显然，RFC 1831成为万里长征路的起点。

> This document discusses clients, calls, servers, replies, services, programs, procedures, and versions.
>
> ​                                                                                                                                        ——RFC 1831

​		通过上述描述，我们可以知道RPC涉及客户端，调用，服务端，答复，服务，程序，进程以及版本这些术语概念，进一步的，这些概念是如何进行交互的可以简单如下图所示：

![image-20210225151226722](E:\aiWriting\RPC\image-20210225151226722.png)

​		现在我们有了RPC的简易模型，但有许多细节之处有待完善，比如最关键的call<->replay过程是怎样的？Client和Server交互的协议是什么（信息的交互过程）？交互过程中发生了异常怎么处理？通过网络的远程调用如何保证不比本地调用性能差太多？交互过程的信息安全性是如何保证的（鉴权）？

## 调用过程

​		在分析调用过程之前，我们先回顾一下RPC的初心，不忘初心，方得始终。RPC的目标是让天下没有困难的远程过程/方法调用，即程序员无论调用本地的还是远程的过程/方法，编写的调用代码本质上是相同的。让我们回想一下，本地方法调用有几种类型？

同步调用：

![image-20210225172024764](E:\aiWriting\RPC\image-20210225172024764.png)

异步调用：

![image-20210225172452116](E:\aiWriting\RPC\image-20210225172452116.png)

如果所示，同步调用当前线程会被阻塞，直到被调用方法执行完成返回，才能继续执行下一段代码；异步调用不会阻塞当前线程，不用等待被调用方返回，直接继续执行下一段代码，被调用方法执行完成后，通过回调的方式告之调用方方法执行完毕。

​		太阳底下没有新鲜事儿，RPC的调用与上述过程并无本质区别，只不过交互的双方由本地的不同方法，变为远程网络中的不同进程。

## 交互协议

​		调用过程中交互双方通过网络传递信息的约定“语义”即为协议。为何要约定语义，不约定语义就不能进行调用交互么？

​		让我们回想一下OSI七层网络模型，最上层的应用层有HTTP、FTP等应用协议，最底层的物理层完成的是机械、电子、定时接口通信信道上的原始比特流传输，抛开中间的各层不谈，简单来讲，网络通讯中的数据，经过各种转化，最终以比特流的形式在网络中传输。在传输的过程中，受硬件带宽的限制，数据有时候会被进行分割（传输层职责），并以分组的方式进行传递（网络层职责），此时就碰到了一个问题，如何标识一个完整数据请求的开始和结束位置？

> 蚌方出曝而鹬啄其肉蚌合而箝其喙鹬曰今日不雨明日不雨即有死蚌蚌亦谓鹬曰今日不出明日不出即有死鹬两者不肯相舍渔者得而并禽之
>
> ​                                                                                                                                1977年山东高考题目

​		1977年山东高考语文题目是给上述文言文加上标点，并翻译成现代汉语。题目的前半部分要求，有助于我们理解如何标识一个完成请求的开始和结束位置。针对整段的文言文，基于我们的古文知识体系及语言习惯，能够给整段文字加上标点，使之变成容易理解的句子：

> 蚌方出曝，而鹬啄其肉，蚌合而箝其喙。鹬曰：‘今日不雨，明日不雨，即有死蚌！’蚌亦谓鹬曰：‘今日不出，明日不出，即有死鹬！’两者不肯相舍，渔者得而并禽之。

​		同理，基于我们的网络知识体系及交互习惯，为RPC传输数据增加“标点符号”，使得发送数据和接受数据能够基于“标点符号”进行语义解析，这个“标点符号”的语义规则，即为协议。

​		接下来的工作就是如何设计这个“标点符号”来实现我们的RPC协议。在广义的TCP/IP五层网络某型中，RCP属于应用层，了解一下其他的应用层协议是如何设计的，对我们设计自己的RPC协议有着一定的借鉴意义。

![img](E:\aiWriting\RPC\2012072810301161.png)

​		一个HTTP请求的报文的语义规则如上图所属，我们可以看到协议由请求行、请求头部、空行和请求数据四部分组成。本着拿来主义的精神，做出一个大胆的假设，我们设计的RPC协议完完全全参考HTTP协议是否可行？

​		答案是技术上可行，性能上损失极大。首先HTTP协议中请求行+请求头部分的大小相对于数据本身要大很多，其次HTTP协议属于无状态协议，对于事务处理没有记忆[能力](https://baike.baidu.com/item/能力/33045)。缺少状态意味着如果后续处理需要前面的信息，则它必须重传，这样可能导致每次连接传送的数据量增大。基于以上两点，我们有必要设计一个性能表现更好且支持一定扩展性的RPC协议。

​		参考HTTP协议，并类比本地方法调用，我们可以大致得出一个RPC调用过程需要传递的属性有哪些。

- 调用过程：接口名称，方法名称，方法参数类型，参数值，调用属性（超时时间，方法版本等扩展属性）
- 返回过程：返回类型，返回值，返回码，异常信息等
- 协议元信息：序列化类型，协议版本，心跳消息标记，单向消息标记，相应消息标记，全局消息ID等，

其中的一些属性有必要简单说几句，比如序列化类型，把对象转化为可传输的字节序列过程成为序列化，序列化的目的是为了进行网络传输，跨平台存储，为了能够让通过RPC通讯的双方能够相互理解，序列化是必须的元信息。

​		一般来说，元信息会被组织为协议头，而调用过程、返回过程的各项属性会被组织为协议体，这样一个RPC协议的雏形就大概形成了，协议头是由一堆固定的长度参数组成，而协议体是根据请求接口和参数构造的，长度属于可变的，具体协议如下图所示：

![image-20210302161334133](E:\aiWriting\RPC\image-20210302161106770.png)



## 序列化与反序列化

- 序列化（serialization）就是将对象序列化为二进制形式，以完成网络传输或数据持久化操作；
- 反序列化（deserialization）则是将从网络、磁盘读取的二进制数据还原为对象形式，以方便后续业务的开展；

在RPC调用过程中，RPC框架将调用方的请求参数（对象）通过序列化算法转换为可以通过网络传输的二进制，服务提供方接收到网络传输的二进制，通过反序列算法将二进制数据还原为请求对象。可以说序列化算法性能的忧虑，在一定程度上决定了RPC框架的性能与稳定性，因此有必要对影响序列化算法性能的关键因素进行深入剖析。

​		首先，让我们以Java语言默认提供的序列化机制入手。在Java中，只有一个类实现了java.io.Serializable接口（该接口没有方法或字段，仅用于标识可序列化的语义），才可以被序列化。关于Java序列化有如下几点事项需要关注：

1. 通过`ObjectOutputStream`和`ObjectInputStream`对对象进行序列化及反序列化
2. 虚拟机是否允许反序列化，不仅取决于类路径和功能代码是否一致，一个非常重要的一点是两个类的序列化 ID 是否一致（就是 `private static final long serialVersionUID`）
3. 序列化并不保存静态变量。
4. 可序列化类的所有子类型本身都是可序列化的，要想将父类对象也序列化，就需要让父类也实现`Serializable` 接口。
5. Transient 关键字的作用是控制变量的序列化，在变量声明前加上该关键字，可以阻止该变量被序列化到文件中，在被反序列化后，transient 变量的值被设为初始值，如 int 型的是 0，对象型的是 null。

既然Java提供了序列化机制，那么为什么极少有RPC框架使用该机制作为其序列化算法呢，原因主要有一下三点：

第一，Java的序列化机制无法跨语言

第二，Java的序列化机制序列化后的码流过大

第三，Java的序列化机制序列化时间太久

我们知道无法跨语言说明其对异构系统支持不友好，序列化后的码流过大影响网络带宽的使用，序列化时间太久影响CPU资源的使用，因此极少有RPC框架使用Java默认的序列化机制作为其序列化算法。

- 多协议对比

![技术分享图片](E:\aiWriting\RPC\序列化)

## 网络通讯

​		一次RPC调用本质是一次网络IO交互，因此网络通讯是RPC框架不得不考虑的基础实现。

- 网络IO模型

常见的网络IO模型有四种：同步阻塞IO（BIO）、同步非阻塞IO（NIO）、IO多路复用（event driven IO）和异步非阻塞IO（AIO），其中IO多路复用也是同步的，只有AIO是异步的。

1.同步阻塞IO（Blocking IO）

顾名思义，同步阻塞IO的流程中，肯定存在block，那么block涉及的对象和阶段就变得至关重要。对于一次网络IO而言，会涉及两个系统对象，一个是调用网络IO的进程（线程），另一个是操作系统内核；而涉及的过程一般包括两个阶段，一个是操作系统内核等待数据准备（等到网络数据到来），另一个是将网络IO数据从操作系统内核拷贝到用户进程（线程）中。一个典型的BIO读操作流程大体如下：

![BIO](E:\aiWriting\RPC\BIO.gif)

从图可以看出，BIO的在等待数据和拷贝数据两个阶段都被block了。事实上在linux中，默认所有的socket操作都是block的，显然这种模型的性能一般，因为在进程（线程）阻塞期间，无法执行任何运算或响应任何的网络请求。

2.同步非阻塞IO（non-blocking IO）

通过配置socket可以将BIO改变为NIO，一个典型的NIO读操作流程大体如下：

![NIO](E:\aiWriting\RPC\NIO.gif)

从图中可以看出，NIO并不会阻塞用户进程，即使内核并没有准备好数据，此时内核会立即给用户进程返回一个错误提示。用户进程可以通过轮询的方式再次发起读请求，直至内核准备好数据，并将数据拷贝到用户进程内存，请求结束。可以看到，轮询的方式读取数据，无疑会浪费CPU资源，因此这种模型不被推荐使用。

3.多路复用IO（IO multiplexing）

IO多路复用更为人所熟知的形式应该是select/poll，select/poll的优势在于单个进程（线程）可以同时处理多个网络IO操作。一个典型的IO多路复用读操作流程大体如下：

![IOmultiplexing](E:\aiWriting\RPC\IOmultiplexing.gif)

从图可以看出，多路复用IO和BIO调用过程比较相似，select调用同样会阻塞用户进程，直到select负责的socekt中的任何一个数据准备ready,此时select会返回，用户进程调用read操作会将数据从内核拷贝到用户进程内存。需要注意的是，如果处理的IO请求数量不是很高的话，无法发挥出多路复用IO的优势，那么多路复用IO的性能表现可能还不如BIO。

4.异步IO（Asynchronous IO）

异步IO使用场景较少，一个典型的异步IO读操作流程大体如下：

![AIO](E:\aiWriting\RPC\AIO.gif)

从图可以看出，用户进程发起read操作后，不会被内核阻塞，即使数据准备没有完成。内核等数据准备完成后，将数据拷贝到用户进程内存，然后给用户进程发送一个signal，通知用户进程read操作完成。

Tips：同步IO与异步IO的区别，为何同步非阻塞IO（NIO）也是同步的

> A synchronous I/O operation causes the requesting process to be blocked until that I/O operation completes;
>
>  An asynchronous I/O operation does not cause the requesting process to be blocked;

说明：non-blocking IO在执行recvfrom这个系统调用的时候，如果kernel的数据没有准备好，这时候不会block进程。但是当kernel中数据准备好的时候，recvfrom会将数据从kernel拷贝到用户内存中，这个时候进程是被block了，在这段时间内进程是被block的。

- RPC框架应该如何选择网络IO模型？  

RPC调用在绝大多数情况下，都是一个高并发的场景，显然IO多路复用模型天然地与该场景需求匹配。虽然IO多路复用使用难度比较高，但是高级编程语言对IO多路复用模型支持的较好，如Java语言的Netyy框架对Java原生的API做了封装，应用比较广泛，而GO语言本身对IO多路复用的封装已经趋近简介优雅了。

- 零拷贝

让我们回想一下BIO的交互的两个阶段——等待数据和拷贝数据，具体的交互流程互流程如下：

![20200817095749154](E:\aiWriting\RPC\20200817095749154.png)

可以看到用户进程的一次读写操作，需要进行四次拷贝，且每次拷贝都需要发生一次CPU上下文切换，这显然存在着性能的消耗与浪费。是否有其他技术方式，能够减少用户进程与内核进程之间的数据拷贝，以提升读写性能呢？

零拷贝（Zero-copy）技术正是为了解决上述问题而诞生的，所谓的零拷贝是取消用户进程与内核空间之间的数据拷贝。

零拷贝有两种实现方式，分别是 mmap+write 方式和 sendfile 方式，其核心原理都是通过虚拟内存来实现的。

1.mmap+write

mmap是Linux提供的一种内存映射文件方法，即将一个进程的地址空间中的一段虚拟地址映射到磁盘文件地址。使用 mmap 的目的是将内核中读缓冲区（read buffer）的地址与用户空间的缓冲区（user buffer）进行映射，从而实现内核缓冲区与应用程序内存的共享，省去了将数据从内核读缓冲区（read buffer）拷贝到用户缓冲（user buffer）的过程，大致的流程如下：

![mmap](E:\aiWriting\RPC\mmap.png)

使用mmap的主要收益是提升IO性能，但是发送的数据要比较大，过小的数据会因为内存映射的对齐页边距要求，造成浪费。

2.sendfile

通过 sendfile 系统调用，数据可以直接在内核空间内部进行 I/O 传输，从而省去了数据在用户空间和内核空间之间的来回拷贝。与 mmap 内存映射方式不同的是， sendfile 调用中 I/O 数据对用户空间是完全不可见的。也就是说，这是一次完全意义上的数据传输过程。

![sendfile](E:\aiWriting\RPC\sendfile.png)

- Netty

1. Netty IO多路复用之Reactor模型

在介绍Netty IO多路复用模型之前，有必要了解一下极了实现IO Multiplexing的API

**select**

```
int select(int nfds, fd_set *readfds, fd_set *writefds,
           fd_set *exceptfds, struct timeval *timeout);
```

其中 nfds 是 readfds、writefds、exceptfds 中编号最大的那个文件描述符加一。readfds 是监听读操作的文件描述符列表，当被监听的文件描述符有可以不阻塞就读取的数据时 ( 读不出来数据也算，比如 end-of-file），select 会返回并将读就绪的描述符放在 readfds 指向的数组内。writefds 是监听写操作的文件描述符列表，当被监听的文件描述符中能可以不阻塞就写数据时（如果一口气写的数据太大实际也会阻塞），select 会返回并将就绪的描述符放在 writefds 指向的数组内。exceptfds 是监听出现异常的文件描述符列表，什么是异常需要看一下文档，与我们通常理解的异常并不太相同。timeout 是 select 最大阻塞时间长度，配置的最小时间精度是毫秒。

select 返回条件：

- 有文件描述符就绪，可读、可写或异常；
- 线程被 interrupt；
- timeout 到了

select 的问题：

- 监听的文件描述符有上限 FD_SETSIZE，一般是 1024。因为 `fd_set` 是个 bitmap，它为最多 `nfds` 个描述符都用一个 bit 去表示是否监听，即使相应位置的描述符不需要监听在 `fd_set` 里也有它的 bit 存在。`nfds` 用于创建这个 bitmap 所以 `fd_set` 是有限大小的。
- 在用户侧，select 返回后它并不是只返回处于 ready 状态的描述符，而是会返回传入的所有的描述符列表集合，包括 ready 的和非 ready 的描述符，用户侧需要去遍历所有 readfds、writefds、exceptfds 去看哪个描述符是 ready 状态，再做接下来的处理。还要清理这个 ready 状态，做完 IO 操作后再塞给 select 准备执行下一轮 IO 操作
- 在 Kernel 侧，select 执行后每次都要陷入内核遍历三个描述符集合数组为文件描述符注册监听，即在描述符指向的 Socket 或文件等上面设置处理函数，从而在文件 ready 时能调用处理函数。等有文件描述符 ready 后，在 select 返回退出之前，kernel 还需要再次遍历描述符集合，将设置的这些处理函数拆除再返回
- 有惊群问题。假设一个文件描述符 123 被多个进程或线程注册在自己的 select 描述符集合内，当这个文件描述符 ready 后会将所有监听它的进程或线程全部唤醒
- 无法动态添加描述符，比如一个线程已经在执行 select 了，突然想写数据到某个新描述符上，就只能等前一个 select 返回后重新设置 FD Set 重新执行 select

select 也有个优点，就是跨平台更容易。实现这个接口的 OS 更多。

**poll**

```
int poll(struct pollfd *fds, nfds_t nfds, int timeout);
```

nfds 是 fds 数组的长度，`struct pollfd` 定义如下：

```
struct pollfd {
               int   fd;         /* file descriptor */
               short events;     /* requested events */
               short revents;    /* returned events */
};
```

`poll` 的返回条件与 `select` 一样。

看到 fds 还是关注的描述符列表，只是在 poll 里更先进一些，将 events 和 reevents 分开了，所以如果关注的 events 没有发生变化就可以重用 fds，poll 只修改 revents 不会动 events。再有 fds 是个数组，不是 fds_set，没有了上限。

相对于 select 来说，poll 解决了 fds 长度上限问题，解决了监听描述符无法复用问题，但仍然需要在 poll 返回后遍历 fds 去找 ready 的描述符，也需要清理 ready 描述符对应的 revents，Kernel 也同样是每次 poll 调用需要去遍历 fds 注册监听，poll 返回时候拆除监听，也仍然有与 select 一样的惊群问题，也有无法动态修改描述符的问题。

**epoll**

```
int epoll_create(int size);
int epoll_ctl(int epfd, int op, int fd, struct epoll_event *event);
int epoll_wait(int epfd, struct epoll_event *events, int maxevents, int timeout);
```

其中 `struct epoll_event` 如下：

```
typedef union epoll_data {
               void        *ptr;
               int          fd;
               uint32_t     u32;
               uint64_t     u64;
} epoll_data_t;
struct epoll_event {
               uint32_t     events;      /* Epoll events */
               epoll_data_t data;        /* User data variable */
};
```

使用步骤：

1. 用 `epoll_create` 创建 epoll 的描述符;
2. 用 `epoll_ctl` 将一个个需要监听的描述符以及监听的事件类型用 `epoll_ctl` 注册在 epoll 描述符上；
3. 执行 `epoll_wait` 等着被监听的描述符 Ready，`epoll_wait` 返回后遍历 Ready 的描述符，根据 Ready 的事件类型处理事件
4. 如果某个被监听的描述符不再需要了，需要用 `epoll_ctl` 将它与 epoll 的描述符解绑
5. 当 epoll 描述符不再需要时需要主动 close，像关闭一个文件一样释放资源

epoll 优点:

- 监听的描述符没有上限；
- `epoll_wait` 每次只会返回 Ready 的描述符，不用完整遍历所有被监听的描述符；
- 监听的描述符被注册到 epoll 后会与 epoll 的描述符绑定，维护在内核，不主动通过 `epoll_ctl` 执行删除不会自动被清理，所以每次执行 `epoll_wait` 后用户侧不用重新配置监听，Kernel 侧在 `epoll_wait` 调用前后也不会反复注册和拆除描述符的监听；
- 可以通过 `epoll_ctl` 动态增减监听的描述符，即使有另一个线程已经在执行 `epoll_wait`；
- `epoll_ctl` 在注册监听的时候还能传递自定义的 `event_data`，一般是传描述符，但应用可以根据自己情况传别的；
- 即使没线程等在 `epoll_wait` 上，Kernel 因为知道所有被监听的描述符，所以在这些描述符 Ready 时候就能做处理，等下次有线程调用 `epoll_wait` 时候直接返回。这也帮助 epoll 去实现 IO Edge Trigger，即 IO Ready 时候 Kernel 就标记描述符为 Ready 之后在描述符被读空或写空前不再去监听它，后面详述；
- 多个不同的线程能同时调用 `epoll_wait` 等在同一个 epoll 描述符上，有描述符 Ready 后它们就去执行；

epoll 缺点：

- `epoll_ctl` 是个系统调用，每次修改监听事件，增加监听描述符时候都是一次系统调用，并且没有批量操作的方法。比如一口气要监听一万个描述符，要把一万个描述符从监听读改到监听写等就会很耗时，很低效；
- 对于服务器上大量连上又断开的连接处理效率低，即 `accept()` 执行后生成一个新的描述符需要执行 `epoll_ctl` 去注册新 Socket 的监听，之后 `epoll_wait` 又是一次系统调用，如果 Socket 立即断开了 `epoll_wait` 会立即返回，又需要再用 `epoll_ctl` 把它删掉；
- 依然有惊群问题，需要配合使用方式避免，后面详述；

### **什么是 Eage-Trigger，什么是 Level-Trigger？**

Epoll 有两种触发模式，一种叫 Eage Trigger 简称 ET，一种叫 Level Trigger 简称 LT。每一个使用 `epoll_ctl` 注册在 epoll 描述符上的被监听的描述符都能单独配置自己的触发模式。

对于这两种触发模式的区别从使用的角度上来说，ET 模式下当一个 FD (文件描述符) Ready 后，需要以 Non-Blocking 方式一直操作这个 FD 直到操作返回 `EAGAIN` 错误为止，期间 Ready 这个事件只会触发 `epoll_wait` 一次返回。而如果是 LT 模式，如果 FD 上的事件一直处在 Ready 状态没处理完，则每次调用 `epoll_wait` 都会立即返回。

When an FD becomes read or write ready, you might not necessarily want to read (or write) all the data immediately.

Level-triggered epoll will keep nagging you as long as the FD remains ready, whereas edge-triggered won't bother you again until the next time you get an `EAGAIN` (so it's more complicated to code around, but can be more efficient depending on what you need to do).

Say you're writing from a resource to an FD. If you register your interest for that FD becoming write ready as level-triggered, you'll get constant notification that the FD is still ready for writing. If the resource isn't yet available, that's a waste of a wake-up, because you can't write any more anyway.

If you were to add it as edge-triggered instead, you'd get notification that the FD was write ready once, then when the other resource becomes ready you write as much as you can. Then if `write(2)` returns `EAGAIN`, you stop writing and wait for the next notification.

The same applies for reading, because you might not want to pull all the data into user-space before you're ready to do whatever you want to do with it (thus having to buffer it, etc etc). With edge-triggered epoll you get told when it's ready to read, and then can remember that and do the actual reading "as and when".

**Java Selector**

Java 的 NIO 提供了一个叫 `Selector`的类，用于跨平台的实现 Socket Polling，也即 IO 多路复用。比如在 BSD 系统上它背后对应的就是 Kqueue，在 Windows 上对应的是 Select，在 Linux 上对应的是 Level Trigger 的 Epoll。Linux 上为什么非要是 Level Trigger 呢？主要是为了跨平台统一，在 Windows 上背后是 Select，它是 Level Trigger 的，那为了同一套代码多处运行，在 Linux 上也只能是 Level Trigger 的，不然使用方式就不同了。

这也是为什么 Netty 自己又为 Linux 单独实现了一套 `EpollEventLoop` 而不只是提供 `NioEventLoop` 就完了。因为 Netty 想支持 Edge Trigger，并且还有很多 Epoll 专有参数想支持。参看这里 Netty 的维护者的回答：[nio - Why native epoll support is introduced in Netty? - Stack Overflow](https://link.zhihu.com/?target=https%3A//stackoverflow.com/questions/23465401/why-native-epoll-support-is-introduced-in-netty)

简单举例一下 Selector 的使用：

1. 先通过 `Selector.open()` 创建出来 Selector；
2. 创建出来 SelectableChannel (可以理解为 Socket)，配置 Channel 为 Non-Blocking
3. 通过 Channel 下的 `register()` 接口注册 Channel 到 Selector，注册时可以带上关心的事件比如 OP_READ，OP_ACCEPT, OP_WRITE 等；
4. 调用 Selector 上的 `select()` 等待有 Channel 上有 Event 产生
5. `select()` 返回后说明有 Channel 有 Event 产生，通过 Selector 获取 `SelectionKey` 即哪些 Channel 有什么事件产生了；
6. 遍历所有获取的 `SelectionKey` 检查产生了什么事件，是 OP_READ 还是 OP_WRITE 等，之后处理 Channel 上的事件；
7. 从 `select()` 返回的 `Iterator` 中移除处理完的 `SelectionKey`

可以看到整个使用过程和使用 select, poll, epoll 的过程是能对应起来的。再补充一下，Selector 是通过 SPI 来实现不同平台使用不同 Selector 实现的。SPI 内容请参看 [[Java Service Provider Interface (SPI) 和类加载机制]]

**Netty如何使用Epoll**

Netty 对 Linux 的 Epoll 接口做了一层封装，封装为 JNI 接口供上层 JVM 来调用。以下内容以 Netty 4.1.48，且使用默认的 Edge Trigger 模式为例。

写数据

按照之前说的使用方式，写数据前需要先通过 `epoll_ctl` 修改 Interest List 为目标 Socket 的 FD 增加 `EPOLLOUT`事件监听。等 `epoll_wait` 返回后表示 Socket 可写，我们开始使劲写数据，直到 `write()`返回 `EAGAIN` 为止。之后我们要再次使用 `epoll_ctl` 去掉 Socket 的 `EPOLLOUT` 事件监听，不然下次我们可能并没有数据要写，可 `epoll_wait` 还会被错误唤醒一次。可以数一下这种使用方式至少有四次系统调用开销，假如每次写一条数据都这么多系统调用的话性能是上不去的。

那 Netty 是怎么做的呢，最核心的地方在这个 [doWrite()](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport-native-epoll/src/main/java/io/netty/channel/epoll/AbstractEpollStreamChannel.java%23L416)。可以看到最关键的是每次有数据要写 Socket 时并不是立即去注册监听 `EPOLLOUT` 写数据，而是用 Busy Loop 的方式直接尝试调用 `write()` 去写 Socket，写失败了就重试，能写多少写多少。如果 Busy Loop 时数据写完了，就直接返回。这种情况下是最优的，完全省去了 `epoll_ctl` 和 `epoll_wait` 的调用。

如果 Busy Loop 多次后没写完，则分两种情况。一种是下游 Socket 依然可写，一种是下游 Socket 已经不能写了 `write()` 返回了 Error。对于第一种情况，用于控制 Loop 次数的 `writeSpinCount` 能到 0，因为下游依然可写我们退出 Busy Loop 只是为了不为这一个 Socket 卡住 `EventLoop` 线程太久，所以此时依然不用设置 `EPOLLOUT` 监听，直接返回即可，这种情况也是最优的。补充说明一下，Netty 里一个 `EventLoop` 对应一个线程，每个线程会处理一批 Socket 的 IO 操作，还会处理 `submit()` 进来的 Task，所以线程不能为某个 Socket 处理 IO 操作处理太久，不然会影响到其它 Socket 的运行。比如我管理了 10000 个连接，其中有一个连接数据量超级大，如果线程都忙着处理这个数据超级大的连接上的数据，其它连接的 IO 操作就有延迟了。这也是为什么即使 Socket 依然可写，Netty 依然在写出一定次数消息后就退出 Busy Loop 的原因。

只有 Busy Loop 写数据时候发现 Socket 写不下去了，这种时候才会配置 `EPOLLOUT` 监听，才会使用 `epoll_ctl`，下一次等 `epoll_wait` 返回后会清理 `EPOLLOUT` 也有一次 `epoll_ctl` 的开销。

通过以上方法可以看到 Netty 已经尽可能减少 `epoll_ctl` 系统调用的执行了，从而提高写消息性能。上面的 `doWrite()` 下还有很多可以看的东西，比如写数据时候会区分是写一条消息，还是能进行批量写，批量写的时候为了调用 JNI 更优，还要把消息拷贝到一个单独的数组等。

读数据

本来读操作相对写操作来说可能更容易一些，每次 Accept 一个 Socket 后就可以把 Socket 对应的 FD 注册到 Epoll 上监听 `EPOLLIN` 事件，每当有事件产生就使劲读 Socket 直到遇到 `EAGAIN`。也就是说整个 Socket 生命周期里都可以不用 `epoll_ctl` 去修改监听的事件类型。但是对 Netty 来说它支持一个叫做 `Auto Read` 的配置，默认是 Auto Read 的，但可以关闭。关闭后必须上层业务主动调用 Channel 上的 `read()` 才能真的读数据进来。这就违反了 Edge Trigger 的约定。所以对于 Netty 在读操作上有这么几个看点：

1. 每次 Accept 一个 Socket 后 Netty 是如何为每个 Socket 设置 `EPOLLIN` 监听的；
2. 每次有读事件后，Edge Trigger 模式下 Netty 是如何读取数据的，能满足一直读取 Socket 直到 `read()`返回 `EAGAIN`
3. Edge Trigger 下 Netty 怎么保证不同 Socket 之间是公平的，即不能出现比如一个 Socket 上一直有数据要读而 EventLoop 就一直在读这一个 Socket 让其它 Socket 饥饿；
4. Netty 的 Auto Read 在 Edge Trigger 模式下是如何工作的

Accept Socket 后如何配置 EPOLLIN

1. Epoll 的 Server Channel 遇到 `EPOLLIN` 事件时就是去执行 Accept 操作，创建新 Socket 也即 Channel 并 [触发 Pipeline 的 Read](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport-native-epoll/src/main/java/io/netty/channel/epoll/AbstractEpollServerChannel.java%23L120)
2. `ServerBootstrap` 在 bind 一个地址时会给 Server Channel 绑定一个 [ServerBootstrapAcceptor handler](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport/src/main/java/io/netty/bootstrap/ServerBootstrap.java%23L157)，每次 Server Channel 有 Read 事件时会用这个 Handler 做处理；
3. 在 `ServerBootstrapAcceptor` 内会[将新来的 Channel 和一个 EventLoop 绑定](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport/src/main/java/io/netty/bootstrap/ServerBootstrap.java%23L218)
4. 新 Channel 和 EventLoop 绑定后会 [触发新 Channel 的 Active 事件](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport/src/main/java/io/netty/channel/AbstractChannel.java%23L510)
5. 新 Channel Active 后如果开启了 Auto Read，会 [立即执行一次 channel.read() 操作](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport/src/main/java/io/netty/channel/DefaultChannelPipeline.java%23L1400)。默认是 Auto Read 的，如果主动关掉 Auto Read 则每次 Channel Active 后需要业务主动去调用一次 `read()`
6. Channel 在执行 `read()` 时会走到 [doBeginRead()](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport/src/main/java/io/netty/channel/AbstractChannel.java%23L843)
7. 对 Epoll 来说在 `doBeginRead()` 内就会 [为 Channel 注册 EPOLLIN 事件监听](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport-native-epoll/src/main/java/io/netty/channel/epoll/AbstractEpollChannel.java%23L228)

Channel 在有 EPOLLIN 事件后如何处理

1. Channel 在有 `EPOLLIN`事件后，会走到 [一个 Loop 内从 Channel 读取数据](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport-native-epoll/src/main/java/io/netty/channel/epoll/AbstractEpollStreamChannel.java%23L778)；
2. 看到 Loop 内的 `allocHandle` 它就是 Netty 控制读数据操作的关键。每次执行 `read()` 后会将返回结果更新在 `allocHandle` 内，比如读了多少字节数据？成功执行了几次读取？当前 Channel 是不是 Edge Trigger 等。
3. Epoll Stream Channel 的 `allocHandle` 是 `DefaultMaxMessagesRecvByteBufAllocator` 这个类，每次以 Loop 方式从 Channel 读取数据后都会执行 `continueReading` 看是否还要继续读。从 `continueReading`实现能看到[循环结束条件](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport/src/main/java/io/netty/channel/DefaultMaxMessagesRecvByteBufAllocator.java%23L141)是是否关闭了 Auto Read，是否读了太多消息，是否是 Edge Trigger 等。默认[最大读取消息数量是 16](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport-native-epoll/src/main/java/io/netty/channel/epoll/AbstractEpollStreamChannel.java%23L59)，也就是说每个 Channel 如果能连续读取出来数据的话，最多读 16 次就不读了，会切换到别的 Channel 上去读；
4. 每次循环读取完数据，会走到 [epollInFinally()](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport-native-epoll/src/main/java/io/netty/channel/epoll/AbstractEpollChannel.java%23L400)，在这里判断是否 Channel 还有数据没读完，是的话需要 Schedule 一个 Task 过一会继续来读这个 Channel 上的数据。因为 Netty 上会分配 IO 操作和 Task 操作比例，一般是一半一半，等 IO 执行完后才会去执行 Task，且 Task 执行时间是有限的，所以不会出现比如一个 Channel 数据特别多导致 EventLoop 即使分配了 Task 实际还是一直在读取同一个 Channel 的数据没有时间处理别的 Channel 的 IO 操作；
5. 如果数据读完了，且 Auto Read 为关闭状态，则会在 `epollInFinally()` 内去掉 `EPOLLIN` 监听，在下一次用户调用 `read()` 时在 `doBeginRead()` 内再次[为 Channel 注册 EPOLLIN 事件监听](https://link.zhihu.com/?target=https%3A//github.com/netty/netty/blob/netty-4.1.48.Final/transport-native-epoll/src/main/java/io/netty/channel/epoll/AbstractEpollChannel.java%23L228)

## 动态代理

​		在讲动态代理之前，不得不提一下代理模式。代理模式在不改变原始类逻辑的前提下，通过引入代理类来给原始类附加功能。举个简单的例子，作为程序猿的你准备给女友买房，但是因为你工作繁忙日理万行代码，没有时间准备那么多繁琐的购买手续，这时房产中介出现了，替你完成这些琐碎的手续流程，而你就可以安心的继续敲代码，而不担心女朋友不高兴了。这就是典型的代理模式，你就是原始类，房产中介就是代理类，附加的功能就是准备购房所需的各种手续。

​		通过你不断地敲代码努力奋斗，老板对你十分赏识，升职加薪配股票，你走上了人生巅峰路。此时你的女朋友也升级为你的老婆，她又提出了买车的需求。然而，此时你工作更加繁忙了，只能依靠购车代理帮你满足老婆的需求。接下来，你老婆又要买保险、买首饰……没多久你发现你需要和N个代理或中介打交道，此时你肯定不胜其烦，于是你找了个一小秘，哦不，一个管家，后续这些事情都有管家帮你打理。上述这种场景就是典型的动态代理，管家在有实际需要时变身为各类代理或中介。

​		在GoF的《设计模式》中，将RPC成为远程代理，通过RPC，将网络通信、数据编码等细节隐藏起来，使得调用者像调用本地函数一样，无需关心与服务者交互的细节，于此同时，服务提供者也只需关注业务开发逻辑，就像开发本地函数一样，不需要关心跟调用者的交互细节。这里的用到的核心技术就是动态代理：

![proxy](E:\aiWriting\RPC\proxy.png)

**JDK动态代理实现原理**

```
/**
 * 原始类or被代理类的接口定义
 */
public interface HelloWorld {
    public String sayHello();
}

/**
 * 真实调用实现类
 */
public class HelloWorldImpl {
    public String invoke(){
        return "proxy say hello.";
    }
}

/**
 * JDK代理类
 */
public class HelloWorldProxy implements InvocationHandler {
    private Object target;
    HelloWorldProxy(Object target){
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return ((HelloWorldImpl)target).invoke();
    }
}

public class HelloWorldMain {
    public static void main(String[] args){
        HelloWorldProxy proxy = new HelloWorldProxy(new HelloWorldImpl());
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        System.setProperty("sun.misc.ProxyGenerator.saveGeneratedFiles","true");
        HelloWorld helloWorld = (HelloWorld) Proxy.newProxyInstance(classLoader, new Class[]{HelloWorld.class}, proxy);
        System.out.println(helloWorld.sayHello());
    }
}

```

Proxy.newProxyInstance是我们关注的重点，请跟随代码

```
    public static Object newProxyInstance(ClassLoader loader,
                                          Class<?>[] interfaces,
                                          InvocationHandler h)
        throws IllegalArgumentException
    {
        Objects.requireNonNull(h);

        final Class<?>[] intfs = interfaces.clone();
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            checkProxyAccess(Reflection.getCallerClass(), loader, intfs);
        }

        /*
         * Look up or generate the designated proxy class.
         */
        Class<?> cl = getProxyClass0(loader, intfs);

        /*
         * Invoke its constructor with the designated invocation handler.
         */
        try {
            if (sm != null) {
                checkNewProxyPermission(Reflection.getCallerClass(), cl);
            }

            final Constructor<?> cons = cl.getConstructor(constructorParams);
            final InvocationHandler ih = h;
            if (!Modifier.isPublic(cl.getModifiers())) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        cons.setAccessible(true);
                        return null;
                    }
                });
            }
            return cons.newInstance(new Object[]{h});
        } catch (IllegalAccessException|InstantiationException e) {
            throw new InternalError(e.toString(), e);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new InternalError(t.toString(), t);
            }
        } catch (NoSuchMethodException e) {
            throw new InternalError(e.toString(), e);
        }
    }

```

重点关注getProxyClass0方法生成代理类的字节码文件。

```
    private static Class<?> getProxyClass0(ClassLoader loader,
                                           Class<?>... interfaces) {
        if (interfaces.length > 65535) {
            throw new IllegalArgumentException("interface limit exceeded");
        }

        // If the proxy class defined by the given loader implementing
        // the given interfaces exists, this will simply return the cached copy;
        // otherwise, it will create the proxy class via the ProxyClassFactory
        return proxyClassCache.get(loader, interfaces);
    }
    
        /**
     * a cache of proxy classes
     */
    private static final WeakCache<ClassLoader, Class<?>[], Class<?>>
        proxyClassCache = new WeakCache<>(new KeyFactory(), new ProxyClassFactory());
```

缓存使用WeakCache实现，当缓存中没有接口对供应的代理类，则通过ProxyClassFactory类的apply方法创建代理类

```
    private static final class ProxyClassFactory
        implements BiFunction<ClassLoader, Class<?>[], Class<?>>
    {
        // prefix for all proxy class names
        private static final String proxyClassNamePrefix = "$Proxy";

        // next number to use for generation of unique proxy class names
        private static final AtomicLong nextUniqueNumber = new AtomicLong();

        @Override
        public Class<?> apply(ClassLoader loader, Class<?>[] interfaces) {

            Map<Class<?>, Boolean> interfaceSet = new IdentityHashMap<>(interfaces.length);
            for (Class<?> intf : interfaces) {
                /*
                 * Verify that the class loader resolves the name of this
                 * interface to the same Class object.
                 */
                Class<?> interfaceClass = null;
                try {
                    interfaceClass = Class.forName(intf.getName(), false, loader);
                } catch (ClassNotFoundException e) {
                }
                if (interfaceClass != intf) {
                    throw new IllegalArgumentException(
                        intf + " is not visible from class loader");
                }
                /*
                 * Verify that the Class object actually represents an
                 * interface.
                 */
                if (!interfaceClass.isInterface()) {
                    throw new IllegalArgumentException(
                        interfaceClass.getName() + " is not an interface");
                }
                /*
                 * Verify that this interface is not a duplicate.
                 */
                if (interfaceSet.put(interfaceClass, Boolean.TRUE) != null) {
                    throw new IllegalArgumentException(
                        "repeated interface: " + interfaceClass.getName());
                }
            }

            String proxyPkg = null;     // package to define proxy class in
            int accessFlags = Modifier.PUBLIC | Modifier.FINAL;

            /*
             * Record the package of a non-public proxy interface so that the
             * proxy class will be defined in the same package.  Verify that
             * all non-public proxy interfaces are in the same package.
             */
            for (Class<?> intf : interfaces) {
                int flags = intf.getModifiers();
                if (!Modifier.isPublic(flags)) {
                    accessFlags = Modifier.FINAL;
                    String name = intf.getName();
                    int n = name.lastIndexOf('.');
                    String pkg = ((n == -1) ? "" : name.substring(0, n + 1));
                    if (proxyPkg == null) {
                        proxyPkg = pkg;
                    } else if (!pkg.equals(proxyPkg)) {
                        throw new IllegalArgumentException(
                            "non-public interfaces from different packages");
                    }
                }
            }

            if (proxyPkg == null) {
                // if no non-public proxy interfaces, use com.sun.proxy package
                proxyPkg = ReflectUtil.PROXY_PACKAGE + ".";
            }

            /*
             * Choose a name for the proxy class to generate.
             */
            long num = nextUniqueNumber.getAndIncrement();
            String proxyName = proxyPkg + proxyClassNamePrefix + num;

            /*
             * Generate the specified proxy class.
             */
            byte[] proxyClassFile = ProxyGenerator.generateProxyClass(
                proxyName, interfaces, accessFlags);
            try {
                return defineClass0(loader, proxyName,
                                    proxyClassFile, 0, proxyClassFile.length);
            } catch (ClassFormatError e) {
                /*
                 * A ClassFormatError here means that (barring bugs in the
                 * proxy class generation code) there was some other
                 * invalid aspect of the arguments supplied to the proxy
                 * class creation (such as virtual machine limitations
                 * exceeded).
                 */
                throw new IllegalArgumentException(e.toString());
            }
        }
    }
```

在ProxyClassFactory类的apply方法中可看出真正生成代理类字节码的地方是ProxyGenerator类中的generateProxyClass

```
 public static byte[] generateProxyClass(final String var0, Class<?>[] var1, int var2) {
        ProxyGenerator var3 = new ProxyGenerator(var0, var1, var2);
        final byte[] var4 = var3.generateClassFile();
        if (saveGeneratedFiles) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    try {
                        int var1 = var0.lastIndexOf(46);
                        Path var2;
                        if (var1 > 0) {
                            Path var3 = Paths.get(var0.substring(0, var1).replace('.', File.separatorChar));
                            Files.createDirectories(var3);
                            var2 = var3.resolve(var0.substring(var1 + 1, var0.length()) + ".class");
                        } else {
                            var2 = Paths.get(var0 + ".class");
                        }

                        Files.write(var2, var4, new OpenOption[0]);
                        return null;
                    } catch (IOException var4x) {
                        throw new InternalError("I/O exception saving generated file: " + var4x);
                    }
                }
            });
        }

        return var4;
    }
```

在测试案例中，设置系统属性sun.misc.ProxyGenerator.saveGeneratedFiles值为true，我们可以在工程的目录下找到$Proxy0.class文件，通过反编译工具jd-gui打开

```
public final class $Proxy0 extends Proxy
  implements HelloWorld
{
  private static Method m1;
  private static Method m3;
  private static Method m2;
  private static Method m0;

  public $Proxy0(InvocationHandler paramInvocationHandler)
    throws 
  {
    super(paramInvocationHandler);
  }

  public final boolean equals(Object paramObject)
    throws 
  {
    try
    {
      return ((Boolean)this.h.invoke(this, m1, new Object[] { paramObject })).booleanValue();
    }
    catch (RuntimeException localRuntimeException)
    {
      throw localRuntimeException;
    }
    catch (Throwable localThrowable)
    {
      throw new UndeclaredThrowableException(localThrowable);
    }
  }

  public final String sayHello()
    throws 
  {
    try
    {
      return (String)this.h.invoke(this, m3, null);
    }
    catch (RuntimeException localRuntimeException)
    {
      throw localRuntimeException;
    }
    catch (Throwable localThrowable)
    {
      throw new UndeclaredThrowableException(localThrowable);
    }
  }

  public final String toString()
    throws 
  {
    try
    {
      return (String)this.h.invoke(this, m2, null);
    }
    catch (RuntimeException localRuntimeException)
    {
      throw localRuntimeException;
    }
    catch (Throwable localThrowable)
    {
      throw new UndeclaredThrowableException(localThrowable);
    }
  }

  public final int hashCode()
    throws 
  {
    try
    {
      return ((Integer)this.h.invoke(this, m0, null)).intValue();
    }
    catch (RuntimeException localRuntimeException)
    {
      throw localRuntimeException;
    }
    catch (Throwable localThrowable)
    {
      throw new UndeclaredThrowableException(localThrowable);
    }
  }

  static
  {
    try
    {
      m1 = Class.forName("java.lang.Object").getMethod("equals", new Class[] { Class.forName("java.lang.Object") });
      m3 = Class.forName("com.moqifei.llts.jellyfish.spring.utils.HelloWorld").getMethod("sayHello", new Class[0]);
      m2 = Class.forName("java.lang.Object").getMethod("toString", new Class[0]);
      m0 = Class.forName("java.lang.Object").getMethod("hashCode", new Class[0]);
      return;
    }
    catch (NoSuchMethodException localNoSuchMethodException)
    {
      throw new NoSuchMethodError(localNoSuchMethodException.getMessage());
    }
    catch (ClassNotFoundException localClassNotFoundException)
    {
      throw new NoClassDefFoundError(localClassNotFoundException.getMessage());
    }
  }
}
```

可看到

1、代理类继承了Proxy类并且实现了要代理的接口，由于java不支持多继承，所以JDK动态代理不能代理类

2、重写了equals、hashCode、toString

3、有一个静态代码块，通过反射或者代理类的所有方法

4、$Proxy0 类里面有一个跟 HelloWorld 一样签名的 sayHello() 方法，其中 this.h 绑定的是刚才传入的 JDKProxy 对象，所以**当我们调用 HelloWorld .sayHello() 的时候，其实它是被转发到了JDKProxy.invoke()**。

**其他动态代理实现**

其实在 Java 领域，除了 JDK 默认的 InvocationHandler 能完成代理功能，我们还有很多其他的第三方框架也可以，比如像 Javassist、Byte Buddy 这样的框架。

相对 JDK 自带的代理功能，Javassist 的定位是能够操纵底层字节码，所以使用起来并不简单，要生成动态代理类恐怕是有点复杂了。但好的方面是，通过 Javassist 生成字节码，不需要通过反射完成方法调用，所以性能肯定是更胜一筹的。在使用中，我们要注意一个问题，通过 Javassist 生成一个代理类后，此 CtClass 对象会被冻结起来，不允许再修改；否则，再次生成时会报错。

Byte Buddy 则属于后起之秀，在很多优秀的项目中，像 Spring、Jackson 都用到了 Byte Buddy 来完成底层代理。相比 Javassist，Byte Buddy 提供了更容易操作的 API，编写的代码可读性更高。更重要的是，生成的代理类执行速度比 Javassist 更快。

## 结语

​		此时此刻，你制定了RPC框架的交互协议，知晓了各类序列化反序列化算法的优缺点，明白了网络通讯首选IO多路复用模型以及Netty对IO多路复用的良好支持，学会了魔法般的动态代理原理，将上述知识点融汇贯通，即可得到一个单机版的RPC框架，还在等什么，赶紧上手开发吧！