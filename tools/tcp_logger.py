#!/usr/bin/env python3
import argparse
import socket
import sys
import time


def timestamp():
    return time.strftime("%Y-%m-%d %H:%M:%S")


def main():
    parser = argparse.ArgumentParser(description="Receive Poops_flow TCP log output.")
    parser.add_argument("--host", default="0.0.0.0", help="bind address")
    parser.add_argument("--port", type=int, default=1337, help="TCP listen port")
    parser.add_argument("--out", default="poops_flow_tcp.log", help="log file to append")
    parser.add_argument("--once", action="store_true", help="exit after the first client disconnects")
    args = parser.parse_args()

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((args.host, args.port))
    server.listen(4)

    print("[%s] Listening on %s:%d; writing to %s" % (timestamp(), args.host, args.port, args.out))

    try:
        while True:
            conn, addr = server.accept()
            print("[%s] Connection from %s:%d" % (timestamp(), addr[0], addr[1]))

            with conn, open(args.out, "a", encoding="utf-8", errors="replace") as log:
                log.write("\n[%s] Connection from %s:%d\n" % (timestamp(), addr[0], addr[1]))
                log.flush()

                while True:
                    data = conn.recv(4096)
                    if not data:
                        break

                    text = data.decode("utf-8", errors="replace")
                    log.write(text)
                    log.flush()
                    sys.stdout.write(text)
                    sys.stdout.flush()

                log.write("\n[%s] Client disconnected\n" % timestamp())
                log.flush()

            print("[%s] Client disconnected" % timestamp())
            if args.once:
                return 0
    except KeyboardInterrupt:
        print("\n[%s] Stopped." % timestamp())
        return 0
    finally:
        server.close()


if __name__ == "__main__":
    raise SystemExit(main())
