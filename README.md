# NetPoops-PS5
sys_netcontrol UAF kernel exploit for PS5 BD-J. Achieves arbitrary kernel R/W, root privileges, enables Debug Settings, and deploys an ELF loader.

## Features

- Full kernel R/W using the [`ExploitNetControlImpl.java`](https://gist.github.com/TheOfficialFloW/7174351201b5260d7780780f4059bebf) `sys_netcontrol` UAF primitive from TheOfficialFloW.
- Jailbreak patches for the current BD-J process, including credentials, root directory, jail state, syscall range, and dlsym range.
- Debug Settings patches through the GPU DMA path, including security flags, target ID, QA flags, and UTOKEN.
- Standalone ELF loader deployment on port 9021.
- Optional autoloader helper mode that sends `ps5_autoload.elf` and `ps5_killdiscplayer.elf` after the ELF loader starts.
- Startup safety checks for already-jailbroken/root state and sleep health before running the exploit.

## Payload Usage

Use one of the prebuilt JARs with your BD-J remote JAR loader:

```sh
payload.jar
```

This is the default payload. It runs Poops, applies the jailbreak/debug settings
path, and starts the standalone ELF loader on port 9021. It does not send the
extra autoloader helper ELFs.

```sh
payload_with_autoloader.jar
```

This variant does the same Poops setup and then sends `ps5_autoload.elf` and
`ps5_killdiscplayer.elf` to the ELF loader.

## Build Usage

Build both release JARs:

```sh
tools/build_payload_autoloader.sh
```

Build only the default payload without autoloader helpers:

```sh
tools/build_payload_autoloader.sh --without-autoloader
```

Build only the payload with autoloader helpers:

```sh
tools/build_payload_autoloader.sh --with-autoloader
```

Each command verifies the generated JAR with `unzip -t` and prints a SHA-256
hash at the end.

## Credits

This project relies heavily on the research and open-source contributions of the
PlayStation security community:

- **[TheFlow (Andy Nguyen)](https://github.com/TheOfficialFloW):** For the discovery of the [`ExploitNetControlImpl`](https://gist.github.com/TheOfficialFloW/7174351201b5260d7780780f4059bebf) vulnerability and the original BD-J sandbox escape research.
- **[Gezine](https://github.com/Gezine) & [egycnq](https://github.com/egycnq):** For the original [`poops_ps5.lua`](https://github.com/Gezine/Luac0re/blob/main/payloads/poops_ps5.lua) script and the core implementation of the exploitation chain.
- **[Jamie](https://github.com/jaigaresc):** For [`Poops-PS5-Java`](https://github.com/jaigaresc/Poops-PS5-Java), a Java implementation of Poops based on the work by Gezine and egycnq.
- **[SpecterDev (Cryptogenic)](https://github.com/Cryptogenic), [ChendoChap](https://github.com/ChendoChap), [John Törnblom](https://github.com/john-tornblom), and the [ps5-payload-dev](https://github.com/ps5-payload-dev) contributors:** For foundational work on PS5 ELF loading, memory mapping, and the overall payload ecosystem.

- **Testers:** Sonic-Iso, [DrYenyen](https://github.com/DrYenyen), and a special thanks to [owendswang](https://github.com/owendswang). This would not have been possible without you.


## Support ☕

If you found this project helpful and want to support my work, consider buying me a coffee or what ever you want!

[Ko-fi](https://ko-fi.com/masszero80428)