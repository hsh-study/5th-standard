const NULL = "\u0000";

export class CourseStompClient {
    constructor(url, hooks = {}) {
        this.url = url;
        this.hooks = hooks;
        this.socket = null;
        this.buffer = "";
        this.subscriptions = new Map();
        this.nextSubscriptionId = 0;
        this.connected = false;
        this.connectPromise = null;
    }

    connect(headers = {}) {
        if (this.socket && this.socket.readyState <= WebSocket.OPEN) {
            return this.connectPromise;
        }

        this.connectPromise = new Promise((resolve, reject) => {
            this.socket = new WebSocket(this.url);
            this.socket.addEventListener("open", () => {
                this.hooks.onDebug?.("WebSocket 101 이후 STOMP CONNECT 전송");
                this.sendFrame("CONNECT", {
                    "accept-version": "1.2",
                    "heart-beat": "0,0",
                    ...headers
                });
            });
            this.socket.addEventListener("message", event => this.consume(String(event.data), resolve, reject));
            this.socket.addEventListener("error", () => reject(new Error("WebSocket 연결에 실패했습니다.")));
            this.socket.addEventListener("close", event => {
                const wasConnected = this.connected;
                this.connected = false;
                this.subscriptions.clear();
                this.hooks.onClose?.({ code: event.code, reason: event.reason, wasConnected });
                if (!wasConnected) reject(new Error(`STOMP 연결 전에 종료되었습니다. (${event.code})`));
            });
        });
        return this.connectPromise;
    }

    subscribe(destination, callback) {
        this.requireConnected();
        const id = `sub-${this.nextSubscriptionId++}`;
        this.subscriptions.set(id, callback);
        this.sendFrame("SUBSCRIBE", { id, destination, ack: "auto" });
        return id;
    }

    unsubscribe(id) {
        if (!id || !this.subscriptions.has(id)) return;
        this.requireConnected();
        this.subscriptions.delete(id);
        this.sendFrame("UNSUBSCRIBE", { id });
    }

    publish(destination, body, headers = {}) {
        this.requireConnected();
        this.sendFrame("SEND", {
            destination,
            "content-type": "application/json",
            ...headers
        }, body);
    }

    disconnect() {
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;
        if (this.connected) this.sendFrame("DISCONNECT", { receipt: `bye-${Date.now()}` });
        this.socket.close(1000, "사용자 연결 종료");
        this.connected = false;
    }

    abortForDemo() {
        if (this.socket) this.socket.close(4001, "강제 단절 실험");
    }

    consume(chunk, resolve, reject) {
        this.buffer += chunk;
        while (this.buffer.length) {
            if (this.buffer.startsWith("\n")) {
                this.buffer = this.buffer.slice(1);
                continue;
            }
            const end = this.buffer.indexOf(NULL);
            if (end < 0) return;
            const rawFrame = this.buffer.slice(0, end);
            this.buffer = this.buffer.slice(end + 1);
            if (!rawFrame.trim()) continue;
            const frame = parseFrame(rawFrame);
            this.hooks.onFrame?.(frame);

            if (frame.command === "CONNECTED") {
                this.connected = true;
                resolve(frame);
            } else if (frame.command === "MESSAGE") {
                this.subscriptions.get(frame.headers.subscription)?.(frame);
            } else if (frame.command === "ERROR") {
                const error = new Error(frame.body || frame.headers.message || "STOMP ERROR");
                this.hooks.onError?.(error, frame);
                reject(error);
            }
        }
    }

    sendFrame(command, headers = {}, body = "") {
        if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
            throw new Error("열려 있는 WebSocket이 없습니다.");
        }
        const lines = [command];
        Object.entries(headers).forEach(([name, value]) => lines.push(`${escapeHeader(name)}:${escapeHeader(value)}`));
        this.socket.send(`${lines.join("\n")}\n\n${body}${NULL}`);
        this.hooks.onDebug?.(`${command}${headers.destination ? ` ${headers.destination}` : ""}`);
    }

    requireConnected() {
        if (!this.connected) throw new Error("STOMP CONNECTED 상태가 아닙니다.");
    }
}

function parseFrame(raw) {
    const separator = raw.indexOf("\n\n");
    const head = separator >= 0 ? raw.slice(0, separator) : raw;
    const body = separator >= 0 ? raw.slice(separator + 2) : "";
    const lines = head.replace(/\r/g, "").split("\n");
    const command = lines.shift();
    const headers = {};
    lines.forEach(line => {
        const colon = line.indexOf(":");
        if (colon > 0) headers[unescapeHeader(line.slice(0, colon))] = unescapeHeader(line.slice(colon + 1));
    });
    return { command, headers, body };
}

function escapeHeader(value) {
    return String(value).replace(/\\/g, "\\\\").replace(/\r/g, "\\r").replace(/\n/g, "\\n").replace(/:/g, "\\c");
}

function unescapeHeader(value) {
    return String(value).replace(/\\c/g, ":").replace(/\\n/g, "\n").replace(/\\r/g, "\r").replace(/\\\\/g, "\\");
}
