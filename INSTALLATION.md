# Installation

Step-by-step instructions to build and run the Go Game on your machine.

## Prerequisites

| Requirement   | Version           | Notes                                                          |
|---------------|-------------------|----------------------------------------------------------------|
| Java JDK      | 17 or later       | Both client and server are compiled with `--source 17`         |
| Apache Maven  | 3.x               | Used to build both modules                                     |
| OS            | Windows / macOS / Linux | Client requires a desktop environment (Swing GUI). Server is headless. |

**Verify your setup:**

```bash
java -version
# Should print something like: openjdk version "17.0.x" ...

mvn -version
# Should print something like: Apache Maven 3.x.x ...
```

## Clone the Repository

```bash
git clone https://github.com/<your-username>/go-game.git
cd go-game
```

## Build Order

The server depends on the client module (it imports `GameLogic` from `com.mycompany.gogameclient`). So the client must be built and installed into the local Maven repository first.

### Step 1: Build the Client

```bash
cd GoGameClient
mvn clean install
```

This compiles the client code, packages it as `GoGameClient-1.0-SNAPSHOT.jar`, and installs it to your local Maven cache (`~/.m2/repository`).

### Step 2: Build the Server

```bash
cd ../GoGameServer
mvn clean package
```

This compiles the server and produces two JARs in `GoGameServer/target/`:
- `GoGameServer-1.0-SNAPSHOT.jar` — server classes only.
- `GoGameServer-1.0-SNAPSHOT-jar-with-dependencies.jar` — fat JAR containing the server classes and the `GameLogic` class from the client module. Use this one for deployment.

## Running Locally

### Start the Server

From the `GoGameServer` directory:

```bash
java -cp target/GoGameServer-1.0-SNAPSHOT-jar-with-dependencies.jar com.mycompany.gogameserver.GoServer
```

You should see:

```
GO Game Server started on port 5000
[Server] Waiting for new connections...
```

Leave this terminal running.

### Start Client 1

Open a new terminal. From the `GoGameClient` directory:

```bash
mvn exec:java
```

Or, if you prefer running the JAR directly:

```bash
java -jar target/GoGameClient-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The Start Screen window will appear. Enter `127.0.0.1` as the server IP and click **START**.

### Start Client 2

Open another terminal and repeat the same command. Enter `127.0.0.1` and click **START**.

Once both clients connect, the server pairs them and the game begins.

## Running from NetBeans IDE

Since the project was built using NetBeans, you can also open and run it directly from the IDE:

1. Open NetBeans.
2. Go to **File → Open Project** and select the `GoGameServer` folder.
3. Repeat for the `GoGameClient` folder.
4. Right-click `GoGameServer` → **Run** to start the server.
5. Right-click `GoGameClient` → **Run** to launch the first client.
6. Right-click `GoGameClient` → **Run** again to launch the second client.

## Playing Over a Network

To play between two different machines:

1. Run the server on one machine (Machine A).
2. Find Machine A's IP address:
   - Windows: `ipconfig` — look for the IPv4 address.
   - macOS/Linux: `ifconfig` or `ip addr`.
3. Make sure port 5000 is not blocked by a firewall.
4. On each client machine, enter Machine A's IP address in the Start Screen and click **START**.

## Deploying the Server to a Remote Host (e.g., AWS EC2)

1. Build the fat JAR:
   ```bash
   cd GoGameServer
   mvn clean package
   ```
2. Copy the fat JAR to the remote host:
   ```bash
   scp target/GoGameServer-1.0-SNAPSHOT-jar-with-dependencies.jar user@remote-host:~/
   ```
3. SSH into the remote host and run:
   ```bash
   java -jar GoGameServer-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```
4. Open port 5000 in the security group / firewall rules.
5. Clients connect using the server's public IP.

## Troubleshooting

### "Could not connect to server"
- Make sure the server is running and listening on port 5000.
- Check that the IP address is correct.
- Check firewall rules — port 5000 must be open for TCP traffic.

### "Address already in use"
- Another process is using port 5000. Either kill it or change the port in `GoServer.java` (and the corresponding port in `StartScreen.java`, line 155).

### Font not loading
- The client looks for `Retro-Gaming.ttf` in these paths (relative to the working directory):
  - `resources/fonts/Retro-Gaming.ttf`
  - `GoGameClient/resources/fonts/Retro-Gaming.ttf`
  - `src/main/resources/fonts/Retro-Gaming.ttf`
- If none of these paths resolve, the game still runs but uses the default system font.
- When running from the IDE, the working directory is typically the project root, so the file is found at `resources/fonts/Retro-Gaming.ttf`.

### Cursors not loading
- Similar to fonts, the cursor images are loaded from `resources/images/` relative to the working directory. If they aren't found, the game falls back to the default OS cursor.
