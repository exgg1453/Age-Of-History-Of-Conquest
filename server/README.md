# Lobby servers

Internet play needs a lobby: something that lists rooms and passes messages
between players. Neither holds game logic, and the host device stays
authoritative in both cases.

There are two implementations, and the game picks between them from the address
you enter on the online screen.

| Folder | Transport | Address form | Host on |
|---|---|---|---|
| `netlify-server/` | HTTP polling | `https://your-site.netlify.app` | Netlify |
| `server/` | WebSocket | `wss://your-app.example.com` | any Node.js host |

## Which one to use

**Netlify** is the easier option and is what the game defaults to. Netlify runs
serverless functions, which cannot hold a WebSocket open, so that version polls
about once a second instead. For a turn based game the difference is not
noticeable, and rooms are stored in Netlify Blobs so nothing else needs setting
up. See `netlify-server/README.md`.

**The WebSocket server** is lower latency and does not poll, but needs a host
that keeps a process running, such as Render, Railway, Fly.io or a machine of
your own.

## Running the WebSocket server

```
cd server
npm install
npm start
```

It listens on `PORT`, defaulting to 8080. Point the game at `ws://your-ip:8080`
on a local machine, or `wss://your-app.example.com` once deployed behind HTTPS.

`GET /health` returns the current room and player counts.

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
same line protocol the local network mode uses, so every transport shares the
game side code.
