# syntax=docker/dockerfile:1
# ^ A "parser directive": pins which version of the Dockerfile *frontend*
# (the component that interprets this file's syntax) BuildKit uses, rather
# than whatever ships baked into the locally-installed Docker version. It
# must be the very first line of the file (comment or not) to be recognized.
# Needed here specifically so the `--mount=type=cache` flag used below on the
# `RUN` instructions is available — that flag is a BuildKit extension, not
# part of the original stable Dockerfile syntax, and this directive is how a
# Dockerfile opts into it explicitly and reproducibly.

# =============================================================================
# Dockerfile for the product-catalog service.
#
# This is a MULTI-STAGE build: it contains more than one `FROM` instruction.
# Each `FROM` starts a brand-new, independent build stage with its own
# filesystem. Only the *final* stage becomes the image you actually ship —
# earlier stages exist purely as scratch space, and everything in them
# (source code, the JDK, Maven's local repo cache, test classes, etc.) is
# discarded unless a later stage explicitly `COPY --from=<stage>`s a file out
# of it. That's the whole point: stage 1 gets to be big and messy because a
# JDK + full Maven dependency tree is needed to *compile* the app, but stage 2
# only needs a JRE (no compiler) to *run* the already-compiled jar. Shipping
# stage 1's filesystem would mean shipping ~200MB of compiler/build tooling
# and source code that serves no purpose at runtime and only expands the
# attack surface.
# =============================================================================


# -----------------------------------------------------------------------------
# Stage 1: "build" — compile the application and produce a runnable jar.
# -----------------------------------------------------------------------------
# eclipse-temurin is the Eclipse Adoptium project's OpenJDK distribution — the
# de facto standard "official" OpenJDK Docker image today (Oracle stopped
# publishing a free-to-redistribute JDK image years ago). Tag breakdown:
#   - "25"      -> Java version, matching pom.xml's <java.version>25</java.version>
#                  and the JDK actually installed on the host (CLAUDE.md pin).
#   - "jdk"     -> the full Java Development Kit: compiler (javac), plus the
#                  runtime. Needed here because this stage runs `mvn package`,
#                  which compiles .java source files.
#   - "noble"   -> pins the underlying OS layer to Ubuntu 24.04 LTS ("Noble
#                  Numbat") specifically, rather than accepting whatever
#                  Temurin's unqualified "25-jdk" tag currently points at.
#                  CLAUDE.md rule 6 says pin exact versions rather than
#                  floating tags — an unqualified tag can silently change its
#                  underlying OS/package versions on a future `docker pull`,
#                  which is exactly the kind of "works on my machine last
#                  week" drift a pinned tag prevents.
# `AS build` names this stage "build" so stage 2 can reference it by name in
# `COPY --from=build`, instead of by a fragile positional index like `--from=0`.
FROM eclipse-temurin:25-jdk-noble AS build

# Sets the working directory for every instruction that follows, for both
# this stage and (implicitly) creates it if it doesn't exist. All later
# COPY/RUN paths are relative to this. Equivalent to `cd /app`, but as a
# first-class Dockerfile instruction so it's captured in the image's history.
WORKDIR /app

# --- Dependency layer: copied and resolved BEFORE the source code ---
#
# Docker builds an image layer-by-layer. Each layer's cache key depends on
# BOTH its own instruction+inputs AND the cache result of the layer before
# it — the check is sequential, not independent per instruction. The moment
# one layer's inputs don't match a prior build (a "cache miss"), Docker
# stops trusting the cache from that point on: every instruction AFTER it
# reruns too, even if that later instruction's own inputs are completely
# unchanged. This cascade — not just "did this exact instruction's own input
# change" — is why multi-step COPY ordering matters: a miss early in the
# file poisons everything below it, regardless of what those later steps
# actually depend on.
#
# Application source code (src/) changes on almost every commit. The Maven
# wrapper and pom.xml — which together determine the *dependency* set —
# change far less often. By copying ONLY the wrapper + pom.xml first and
# resolving dependencies against them here, that expensive "download half the
# internet from Maven Central" step (the RUN on lines 108-109 below) gets
# cached and skipped on every subsequent build where dependencies haven't
# changed, even though the application source has.
#
# If `src/` were instead copied here, BEFORE that RUN, then any source-only
# change would make the `COPY src/` layer miss — which would then also drag
# the `dependency:go-offline` RUN below it into rerunning, even though
# pom.xml/mvnw (its actual inputs) hadn't changed at all, forcing a full
# re-download of every dependency on every single build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# The Maven Wrapper (`mvnw`) is a checked-in shell script that downloads and
# runs a specific pinned Maven version on demand (see
# .mvn/wrapper/maven-wrapper.properties — 3.9.16) — it's why no separate
# `maven:...` base image or host Maven install is needed here. It's also why
# .mvn/ was copied above: mvnw reads the wrapper properties file from there to
# know which Maven version to fetch on its first invocation.
#
# `dependency:go-offline` tells Maven to download every dependency (including
# ones needed by later build phases like test/package, so they're not missed)
# into the local repo cache, without actually compiling or packaging
# anything. Doing this as its own RUN — using only pom.xml, before any source
# exists — is what makes it its own cacheable layer per the note above.
#
# Flag syntax note: `--mount` takes ONE value, and that value is itself a
# comma-separated list of its own `key=value` sub-options — so the first `=`
# binds the flag to its value, and each subsequent `key=value` (`type=cache`,
# `target=/root/.m2`, `sharing=locked`) is a sub-option WITHIN that value, not
# a separate flag. `type` picks which kind of mount BuildKit creates (`cache`,
# `bind`, `secret`, `ssh`, ...); the other sub-options are specific to
# whichever `type` was chosen — `cache` accepts `target` and `sharing`.
#
# `--mount=type=cache,target=/root/.m2,sharing=locked` is a SEPARATE caching
# mechanism from the Docker layer caching discussed above, solving a
# different problem. A normal RUN's filesystem changes get baked into that
# instruction's image layer and thrown away entirely the moment any earlier
# layer's cache is invalidated (e.g. pom.xml changes) — meaning Maven's
# local repository (~/.m2, several hundred MB of downloaded jars) would be
# re-downloaded from Maven Central FROM SCRATCH on every such rebuild, and
# if a build fails partway through resolving dependencies (a real risk with
# hundreds of individual downloads — network hiccups happen), a retry
# re-downloads everything that already succeeded too, not just what failed.
# A cache mount instead persists /root/.m2 in BuildKit's own build cache,
# OUTSIDE any image layer — it survives across separate `docker build`
# invocations regardless of what layers get invalidated, is never included
# in the final image, and means a re-run after a network blip resumes from
# what's already cached rather than starting over. `sharing=locked` prevents
# two concurrent builds from corrupting the shared cache by writing to it at
# the same time.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw dependency:go-offline

# --- Source layer: copied after dependencies are resolved ---
#
# Now bring in the actual application code. This invalidates the Docker
# cache from this line down (source changes every commit), but the
# dependency-resolution layer above stays cached and is skipped.
COPY src/ src/

# Compile and package into a runnable jar.
#   - `clean`         -> wipe any stale target/ output before building (this
#                        stage starts from a fresh container filesystem
#                        anyway, so mostly here as a habit/safety net, matching
#                        the exact command a developer would run locally).
#   - `package`       -> compile + run the Spring Boot repackage step,
#                        producing target/app.jar (an executable jar
#                        containing the app plus all its dependencies —
#                        "app" because pom.xml pins <finalName>app</finalName>
#                        so this filename never changes across version bumps).
#   - `-DskipTests`   -> do NOT run the test suite as part of this build.
#                        This is intentional, not corner-cutting: the full
#                        suite (including Testcontainers tests that spin up a
#                        real Postgres via Docker) already runs via
#                        `mvn clean verify` locally and will run again in CI
#                        (Step 11) as an explicit, separate gate before an
#                        image is ever built from trusted source. Running
#                        Testcontainers *inside* this build stage would mean
#                        Docker-in-Docker (a container starting more
#                        containers), which is slow, fragile, and conflates
#                        two separate concerns: "does the code work" (tests)
#                        and "can we package what already passed" (this
#                        build). Keeping them separate is standard practice.
# Same cache mount as the dependency:go-offline step above, and for the same
# reason: without it, this RUN would start from an empty ~/.m2 in its own
# throwaway layer filesystem and re-download every dependency a second time,
# even though the previous instruction just finished resolving them all.
# Mounting the identical target path (`/root/.m2`) is what makes BuildKit
# reuse the SAME persisted cache across both RUN instructions.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw clean package -DskipTests

# --- Layer extraction: split the fat jar into cache-friendly pieces ---
#
# Spring Boot 4's executable jar is technically a zip containing your classes
# AND every dependency's classes bundled together. Its MANIFEST.MF does NOT
# point `Main-Class` directly at this project's own Spring Boot main class —
# it points at a Spring Boot LOADER class
# (org.springframework.boot.loader.launch.JarLauncher) that is itself bundled
# inside the jar. That loader's main() checks `System.getProperty("jarmode")`
# BEFORE doing anything else:
#   - unset (the normal case — see this file's ENTRYPOINT below, which passes
#     no -Djarmode) -> the loader builds a classloader that can see this
#     project's classes plus every dependency jar, then calls into this
#     project's real main class, and the Spring application starts as usual.
#   - set, e.g. `-Djarmode=tools` (used in the RUN below) -> the loader takes
#     a completely different branch: it uses the JDK's ServiceLoader
#     mechanism (looks up a pluggable implementation matching the name
#     "tools" at runtime, rather than the loader hardcoding that class name)
#     to find a `JarMode` implementation bundled inside the SAME jar, and
#     hands control to THAT instead. The Spring application, its
#     ApplicationContext, auto-configuration — none of it runs. This is a
#     one-off CLI utility mode reachable through the same `Main-Class` entry
#     point, not a normal application run.
# `extract --layers` is the specific subcommand this jarmode implementation
# understands: it unpacks that one fat jar into
# separate directories by *rate of change*, AND rewrites app.jar itself along
# the way into a much smaller "thin" jar: instead of bundling every
# dependency's classes inside it, the thin jar's manifest gets a `Class-Path`
# entry listing external `lib/*.jar` paths, and its `Main-Class` points
# straight at this project's own Spring Boot main class rather than any
# custom nested-jar loader — verified by inspecting the extracted output's
# META-INF/MANIFEST.MF directly, since this differs from older Spring Boot
# (pre-3.2) tutorials that describe a `JarLauncher`-based nested-jar layout.
# The four output directories:
#   - dependencies/            -> lib/*.jar: third-party libraries (Spring,
#                                 Hibernate, Postgres driver, ...) — changes
#                                 only when pom.xml's dependency versions do.
#   - spring-boot-loader/      -> EMPTY for this project/Spring Boot version:
#                                 the old nested-jar loader classes this
#                                 directory used to hold aren't needed by the
#                                 thin-jar-plus-Class-Path layout above. Still
#                                 copied in stage 2 below to match Spring
#                                 Boot's documented 4-layer extraction
#                                 contract, in case a future Spring Boot
#                                 version (or a project that ends up with
#                                 nested jars) populates it again.
#   - snapshot-dependencies/   -> any -SNAPSHOT dependency versions (none in
#                                 this project, so also empty) — kept separate
#                                 because SNAPSHOTs change more often than
#                                 released dependency versions.
#   - application/             -> the thin app.jar itself, described above.
#                                 Changes on essentially every commit.
# This split is what lets stage 2 COPY each piece as its OWN Docker layer
# (below), so that a source-only change only invalidates and re-uploads the
# small application/ layer, not the whole multi-hundred-MB dependency set —
# both for local `docker build` caching and for faster `docker push`/`pull`
# to a registry later (Step 11's CI/CD).
RUN java -Djarmode=tools -jar target/app.jar extract --layers --destination extracted


# -----------------------------------------------------------------------------
# Stage 2: "runtime" — the actual image that ships and runs.
# -----------------------------------------------------------------------------
# Note this is a JRE image, not JDK: the Java Runtime Environment can RUN
# compiled bytecode but has no compiler (javac) and no build tooling. Since
# this stage never compiles anything — stage 1 already did that — the JRE is
# all it needs, and it's meaningfully smaller than the JDK image, which
# means: faster `docker pull` in Step 3+ (Kubernetes pulls this same image),
# less disk, and fewer binaries (a compiler, e.g.) present that would be
# useless-but-attack-surface in a container serving production traffic. Same
# Java version (25) and OS (noble) pin as stage 1, for consistent behavior
# between build and run.
#
# `AS runtime` names this stage, mirroring `AS build` on stage 1 — but unlike
# `build` (which stage 2 actively references via `COPY --from=build` below),
# nothing in this file currently references `runtime` by name. That's not
# because the name is dead/decorative: it's a byproduct of this being a
# 2-stage file where `runtime` happens to be the LAST stage. Naming a stage
# does two concrete things regardless of whether anything uses it yet:
#   1. It lets any LATER stage `COPY --from=runtime` out of it (e.g. if a
#      future debug/diagnostics stage were appended after this one).
#   2. It lets a build be pinned to stop here explicitly via
#      `docker build --target runtime`, rather than relying on "whichever
#      stage happens to be physically last in the file" as the implicit
#      default. Right now those two behave identically since `runtime` IS
#      the last stage — but the name is what makes that an explicit,
#      refactor-safe choice instead of an accident of file ordering.
FROM eclipse-temurin:25-jre-noble AS runtime

WORKDIR /app

# --- Non-root user ---
#
# By default, a container's main process runs as root (UID 0) unless told
# otherwise — root *inside* the container, but that still means: if an
# attacker achieves code execution in this JVM (e.g. via a future dependency
# vulnerability), they get root inside the container, which is a strictly
# worse starting position than an unprivileged user, and on some
# misconfigurations can even map to privileges on the host. Creating and
# switching to a dedicated non-root user is a cheap, universally-recommended
# baseline hardening step — NOT the deep security work (read-only
# filesystems, seccomp/AppArmor profiles, image scanning, RBAC) that's
# explicitly scoped to Step 12. That's a full step; this is table stakes.
#
#   - `groupadd -r app` -> create a group named "app". `-r` makes it a
#     "system" group (allocates a UID/GID below the range normally handed to
#     real human user accounts) — the conventional choice for
#     service/daemon accounts that will never be someone's login.
#   - `useradd -r -g app -s /usr/sbin/nologin app` -> create a system user
#     "app" belonging to the "app" group (`-g app`), with `/usr/sbin/nologin`
#     as its shell so it's explicitly unusable for interactive login even if
#     someone tried `su app` or similar — this account exists only to own and
#     run this one process.
RUN groupadd -r app && useradd -r -g app -s /usr/sbin/nologin app

# --- Copy the layered application, in dependency-stability order ---
#
# Each COPY below is its own Docker layer. Ordered from "changes least
# often" to "changes most often" so that a rebuild after only touching
# application source code reuses every layer except the last one — Docker
# stops trusting the cache at the first layer whose input changed, so
# earlier/never-changing layers must come first for that caching to pay off.
# `--from=build` pulls from stage 1 by the name given via `AS build` above;
# `--chown=app:app` sets file ownership to the non-root user/group created
# above at copy time (cheaper than a separate `RUN chown` instruction, which
# would add its own extra layer).
COPY --from=build --chown=app:app /app/extracted/dependencies/ ./
COPY --from=build --chown=app:app /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /app/extracted/application/ ./

# Switch the active user for all subsequent instructions AND for the
# container's main process at runtime.
#
# Placing USER here, after the COPYs above, is NOT functionally required to
# make those COPYs succeed — it's easy to assume a non-root USER earlier
# would make them fail with a permission error, the same way a real
# non-root *runtime* process would be blocked writing into a root-owned
# directory. But COPY doesn't work like a live process: BuildKit executes it
# by directly constructing the new image layer's contents itself, not by
# spawning a process that performs a real, kernel-permission-checked write
# the way `RUN` (during build) or this image's own ENTRYPOINT (at runtime)
# do. So there's no Unix permission check for USER to trip on here, and each
# COPY's ownership is set explicitly via --chown anyway, independent of
# whatever USER is currently active.
#
# The real reason USER goes last is documentation/intent: everything above
# this line is "image setup as root" (creating the user, copying files in),
# and everything from here down — matching how the process actually runs —
# executes as the unprivileged "app" account.
USER app

# Documents which port the application listens on (matches
# application.yml's `server.port: 8080`). This is metadata only — it does
# NOT actually publish the port to the host; that's docker-compose.yml's
# `ports:` mapping. EXPOSE exists so anyone reading this Dockerfile (or
# tooling that inspects the image) knows which port to expect without
# reading the Spring config.
EXPOSE 8080

# The container's main process. Uses exec form (JSON array), not shell form
# (`ENTRYPOINT java ...`) — exec form runs `java` directly as PID 1 inside
# the container rather than wrapping it in `/bin/sh -c "..."`. That matters
# for two reasons: (1) signals like SIGTERM (which `docker stop` sends, and
# which Kubernetes sends before a pod is killed in Step 3) go straight to the
# JVM instead of being absorbed by an intermediate shell process that
# doesn't forward them, so the JVM gets a chance to shut down gracefully
# instead of being hard-killed; (2) it avoids an unnecessary extra shell
# process. `java -jar app.jar` works normally here — no custom loader class
# needed — because, as explained above, the extraction step already rewrote
# app.jar into a normal thin jar whose manifest points `Main-Class` at this
# project's own Spring Boot main class and lists the dependencies/ layer's
# jars via a `Class-Path` entry; the JVM resolves both directly.
ENTRYPOINT ["java", "-jar", "app.jar"]
