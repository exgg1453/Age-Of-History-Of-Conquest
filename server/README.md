# Lobby server

This is the room directory and message relay for internet play. Without it,
Age Of History Of Conquest still supports same device and same network
multiplayer; this server is only needed for the online room list.

It holds no game logic. Rooms are kept in memory, and everything a client sends
is forwarded to the other members of its room. The host device remains
authoritative for the game itself.

## Running it

```
cd server
npm install
npm start
```

It listens on `PORT`, defaulting to 8080. Point the game at it by entering the
address in the online screen, for example `ws://192.168.1.20:8080`.

## Deploying it on a Sprite

If you have the Sprites CLI:

```
sprite create aohc-lobby
sprite exec -s aohc-lobby -- git clone https://github.com/exgg1453/Age-Of-History-Of-Conquest.git
sprite exec -s aohc-lobby -- sh -c "cd Age-Of-History-Of-Conquest/server && npm install --omit=dev"
sprite exec -s aohc-lobby -- sh -c "cd Age-Of-History-Of-Conquest/server && PORT=8080 node server.js"
```

Check it is alive, then use the address the sprite exposes on port 8080 in the
game as `ws://host:8080`, or `wss://host` if the sprite terminates TLS.

To keep it running after the console closes, start it under a process manager
that survives the session, for example:

```
sprite exec -s aohc-lobby -- sh -c "cd Age-Of-History-Of-Conquest/server && nohup node server.js > lobby.log 2>&1 &"
```

## Deploying it elsewhere

Any host that supports Node.js and WebSockets works. The free tiers of Render,
Railway, Fly.io and Koyeb are all sufficient, since the server is tiny and holds
only room metadata. There is nothing to configure beyond `PORT`, which those
platforms set themselves.

A `Dockerfile` is included for hosts that build containers, and a `render.yaml`
for Render, which will pick the service up from this repository without any
further setup.

On a plain Linux box:

```
git clone https://github.com/exgg1453/Age-Of-History-Of-Conquest.git
cd Age-Of-History-Of-Conquest/server
./start.sh
```

Once deployed over HTTPS, use the `wss://` form of the address in the game, for
example `wss://your-app.onrender.com`.

`GET /health` returns the current room and player counts.

## Environment

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | 8080 | port to listen on |
| `MAX_ROOMS` | 500 | refuse new rooms beyond this |

Idle rooms are dropped after thirty minutes.

## Protocol

Messages are JSON objects with a `t` field.

Client to server: `hello`, `list`, `create`, `join`, `update`, `leave`, `relay`.
Server to client: `welcome`, `rooms`, `created`, `joined`, `roomUpdated`,
`peerJoined`, `peerLeft`, `relay`, `roomClosed`, `error`.

The `data` field of a `relay` message is opaque to the server. It carries the
same line protocol the local network mode uses, so both transports share the
game side code.
