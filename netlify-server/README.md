# Netlify lobby

The room directory and message relay for internet play, running as a single
Netlify function with Netlify Blobs for storage. There is no database to create
and nothing to configure.

Netlify functions are serverless and cannot hold a WebSocket open, so this
version works by polling roughly once a second. For a turn based game that is
not noticeable. If you want a persistent socket instead, use the Node.js server
in `server/`.

## Deploying

From this directory:

```
npx netlify deploy --prod
```

The first run asks you to link or create a site. Alternatively, connect the
repository in the Netlify dashboard and set the base directory to
`netlify-server`.

Once deployed, enter the site address in the game's online screen, for example
`https://your-site.netlify.app`. The game appends the API path itself.

`GET /api/lobby/health` returns the current room count.

## What it stores

| Key | Contents |
|---|---|
| `room/<CODE>` | room settings and its member list |
| `msg/<CODE>/<timestamp>` | one relayed message |

Messages are deleted five minutes after they are written, and rooms thirty
minutes after their last activity, so the store stays small on its own.

Each message is written under its own key rather than appended to a shared log,
because Netlify Blobs is last write wins and two players sending at the same
moment would otherwise overwrite each other.

## Actions

`POST /api/lobby` with a JSON body containing an `action` field: `list`,
`create`, `join`, `update`, `leave`, `send` or `poll`.

The `data` field of `send` is opaque to the server. It carries the same line
protocol every other transport uses, so the game side code is shared.
