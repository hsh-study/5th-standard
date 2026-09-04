import { CourseStompClient } from "/stomp-client.js";

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];

const state = {
    token: null,
    memberId: "member-1",
    saleId: "sale-1",
    joined: false,
    stomp: null,
    connected: false,
    messages: new Map(),
    lastCommand: null,
    lastSequence: 0
};

bindEvents();
renderAuthState();
logEvent("UI", "24회차 읽음 상태 관찰 화면을 열었습니다.");

function bindEvents() {
    $$(".member-option").forEach(button => button.addEventListener("click", () => selectMember(button.dataset.member)));
    $("#start-journey").addEventListener("click", startJourney);
    $("#disconnect").addEventListener("click", disconnect);
    $("#message-form").addEventListener("submit", sendMessage);
    $("#retry-last").addEventListener("click", retryLastMessage);
    $("#load-history").addEventListener("click", guard("메시지 동기화 실패", () => synchronizeMessages(0)));
    $("#open-lab").addEventListener("click", openLab);
    $("#close-lab").addEventListener("click", closeLab);
    $("#drawer-backdrop").addEventListener("click", closeLab);
    $("#clear-log").addEventListener("click", () => $("#event-log").replaceChildren());
    $("#sale-id").addEventListener("input", event => {
        state.saleId = event.target.value.trim();
        $("#room-title").textContent = state.saleId || "채팅방";
    });
    $("#message-input").addEventListener("input", resizeComposer);
    $("#message-input").addEventListener("keydown", event => {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            $("#message-form").requestSubmit();
        }
    });
}

function selectMember(memberId) {
    state.memberId = memberId;
    $("#member-id").value = memberId;
    $$(".member-option").forEach(button => button.classList.toggle("active", button.dataset.member === memberId));
}

async function startJourney() {
    const button = $("#start-journey");
    button.disabled = true;
    state.stomp?.disconnect();
    resetJourney();
    try {
        setStep("login", "active");
        await login();
        setStep("login", "done");
        setStep("join", "active");
        await joinSale();
        setStep("join", "done");
        setStep("connect", "active");
        await connectStomp();
        setStep("connect", "done");
        setStep("subscribe", "done");
        toast(`${state.memberId}로 ${state.saleId}에 입장했습니다.`);
    } catch (error) {
        handleError("입장 실패", error);
    } finally {
        button.disabled = false;
    }
}

async function login() {
    const issued = await api("/api/auth/token", {
        method: "POST",
        auth: false,
        body: { memberId: $("#member-id").value.trim(), password: $("#password").value }
    });
    state.token = issued.accessToken;
    state.memberId = issued.memberId;
    logEvent("HTTP", `JWT 발급 · sub=${state.memberId}`);
    renderAuthState();
}

async function joinSale() {
    state.saleId = $("#sale-id").value.trim();
    if (!state.saleId) throw new Error("채팅방 ID가 필요합니다.");
    await api(`/api/live-sales/${encodeURIComponent(state.saleId)}/chat/join`, { method: "POST" });
    state.joined = true;
    $("#room-title").textContent = state.saleId;
    logEvent("HTTP", `${state.saleId} 참여 권한 등록`);
}

async function connectStomp() {
    setConnection("connecting", "연결 중");
    const wsProtocol = location.protocol === "https:" ? "wss:" : "ws:";
    const client = new CourseStompClient(`${wsProtocol}//${location.host}/ws`, {
        onDebug: message => logEvent("STOMP", message),
        onFrame: frame => logEvent("FRAME", `${frame.command}${frame.headers.destination ? ` · ${frame.headers.destination}` : ""}`),
        onError: error => handleError("STOMP 오류", error),
        onClose: close => {
            if (state.stomp !== client) return;
            state.connected = false;
            setConnection("disconnected", `연결 종료 · ${close.code}`);
        }
    });
    state.stomp = client;
    await client.connect({ Authorization: `Bearer ${state.token}` });
    state.connected = true;
    client.subscribe(`/topic/live-sales/${state.saleId}`, frame => receiveChat(JSON.parse(frame.body), "MESSAGE"));
    setConnection("connected", "구독 완료");
    $("#disconnect").disabled = false;
    $("#message-input").disabled = false;
    $("#send-message").disabled = false;
    $("#composer-help").textContent = `${state.memberId}의 메시지는 서버 Principal로 기록됩니다.`;
}

function disconnect() {
    state.stomp?.disconnect();
    state.connected = false;
    setConnection("disconnected", "연결 종료");
    disableComposer();
    logEvent("STOMP", "사용자가 DISCONNECT를 요청했습니다.");
}

function sendMessage(event) {
    event.preventDefault();
    const content = $("#message-input").value.trim();
    if (!content || !state.connected) return;
    const command = { clientMessageId: crypto.randomUUID(), content };
    publishChat(command);
    state.lastCommand = command;
    $("#message-input").value = "";
    resizeComposer();
    $("#retry-last").disabled = false;
}

function retryLastMessage() {
    if (!state.lastCommand) return;
    publishChat(state.lastCommand);
    logEvent("RETRY", `동일 clientMessageId 재전송 · ${shortId(state.lastCommand.clientMessageId)}`);
}

function publishChat(command) {
    state.stomp.publish(`/app/live-sales/${state.saleId}/messages`, JSON.stringify(command));
    logEvent("SEND", `${shortId(command.clientMessageId)} · ${command.content.slice(0, 24)}`);
}

function receiveChat(message, source) {
    const id = String(message.messageId);
    if (state.messages.has(id)) {
        logEvent("DEDUP", `${shortId(id)} 화면 중복 제거`);
        return;
    }
    state.messages.set(id, message);
    state.lastSequence = Math.max(state.lastSequence, Number(message.id) || 0);
    renderMessages();
    updateSequence();
    logEvent(source, `sequence=${message.id} · sender=${message.senderId}`);
    if (message.senderId !== state.memberId) refreshUnread().catch(() => {});
}

async function synchronizeMessages(after, { quiet = false } = {}) {
    if (!state.token || !state.joined) {
        if (!quiet) toast("먼저 로그인하고 채팅방에 입장하세요.", true);
        return;
    }
    const messages = await api(`/api/live-sales/${encodeURIComponent(state.saleId)}/chat/messages?after=${after}&size=100`);
    messages.forEach(message => receiveChat(message, "SYNC"));
    if (messages.length) await markRead();
    else if (!quiet) toast("새로 동기화할 메시지가 없습니다.");
}

async function markRead() {
    if (!state.lastSequence) return;
    const snapshot = await api(`/api/live-sales/${encodeURIComponent(state.saleId)}/chat/mark-read`, {
        method: "POST",
        body: { lastReadId: state.lastSequence }
    });
    logEvent("READ", `requested=${state.lastSequence} · stored=${snapshot.lastReadId}`);
    await refreshUnread();
}

async function refreshUnread() {
    const result = await api(`/api/live-sales/${encodeURIComponent(state.saleId)}/chat/unread-count`);
    $("#composer-help").textContent = result.count
        ? `읽지 않은 메시지 ${result.count}개 · 동기화 후 읽음 위치를 갱신합니다.`
        : `${state.memberId} · 마지막 읽음 ID ${state.lastSequence}`;
}

async function api(path, options = {}) {
    const method = options.method ?? "GET";
    const headers = { ...(options.headers ?? {}) };
    if (options.auth !== false && state.token) headers.Authorization = `Bearer ${state.token}`;
    if (options.body !== undefined) headers["Content-Type"] = "application/json";
    logEvent("HTTP", `${method} ${path}`);
    const response = await fetch(path, {
        method,
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
    });
    const text = await response.text();
    const data = text ? safeJson(text) : null;
    if (!response.ok) throw new Error(`${response.status} ${data?.message || data?.detail || text || response.statusText}`.trim());
    return data;
}

function renderAuthState() {
    const loggedIn = Boolean(state.token);
    $("#member-badge").textContent = loggedIn ? state.memberId.toUpperCase() : "GUEST";
    setGlobalStatus(loggedIn ? "success" : "neutral", loggedIn ? `${state.memberId} 로그인` : "로그인 전");
}

function resetJourney() {
    ["login", "join", "connect", "subscribe"].forEach(step => setStep(step, ""));
    state.token = null;
    state.joined = false;
    state.connected = false;
    state.messages.clear();
    state.lastSequence = 0;
    renderAuthState();
    setConnection("disconnected", "연결 전");
    disableComposer();
    renderMessages();
    updateSequence();
}

function renderMessages() {
    const list = $("#message-list");
    const emptyChat = $("#empty-chat");
    list.replaceChildren();
    const messages = [...state.messages.values()].sort((a, b) => Number(a.id) - Number(b.id));
    if (!messages.length) {
        if (emptyChat) list.append(emptyChat);
        return;
    }
    messages.forEach(message => {
        const article = document.createElement("article");
        article.className = `message ${message.senderId === state.memberId ? "mine" : "other"}`;
        const meta = document.createElement("div");
        meta.className = "message-meta";
        const sender = document.createElement("strong");
        const time = document.createElement("time");
        sender.textContent = message.senderId;
        time.textContent = formatTime(message.sentAt);
        meta.append(sender, time);
        const bubble = document.createElement("div");
        bubble.className = "message-bubble";
        bubble.textContent = message.content;
        const sequence = document.createElement("span");
        sequence.className = "message-sequence";
        sequence.textContent = `SEQ ${message.id} · ${shortId(message.clientMessageId)}`;
        article.append(meta, bubble, sequence);
        list.append(article);
    });
    list.scrollTop = list.scrollHeight;
}

function disableComposer() {
    $("#disconnect").disabled = true;
    $("#message-input").disabled = true;
    $("#send-message").disabled = true;
    $("#composer-help").textContent = "연결을 완료하면 메시지를 보낼 수 있습니다.";
}

function updateSequence() {
    $("#sequence-label").textContent = `마지막 읽음 ID · ${state.lastSequence}`;
}

function resizeComposer() {
    const input = $("#message-input");
    input.style.height = "auto";
    input.style.height = `${Math.min(input.scrollHeight, 110)}px`;
}

function setStep(step, status) {
    const element = $(`#step-${step}`);
    element.classList.remove("active", "done");
    if (status) element.classList.add(status);
}

function setConnection(status, label) {
    $("#ws-dot").className = `connection-dot ${status}`;
    $("#ws-label").textContent = label;
}

function setGlobalStatus(status, label) {
    const element = $("#global-status");
    element.className = `status-pill ${status}`;
    element.innerHTML = "<i></i>";
    element.append(document.createTextNode(` ${label}`));
}

function logEvent(kind, message, level = "info") {
    const row = document.createElement("div");
    row.className = `log-entry ${level}`;
    const time = document.createElement("time");
    const badge = document.createElement("b");
    const copy = document.createElement("span");
    time.textContent = new Date().toLocaleTimeString("ko-KR", { hour12: false });
    badge.textContent = kind;
    copy.textContent = message;
    row.append(time, badge, copy);
    $("#event-log").prepend(row);
}

function handleError(title, error) {
    const message = error instanceof Error ? error.message : String(error);
    logEvent("ERROR", `${title} · ${message}`, "error");
    toast(`${title}: ${message}`, true);
}

function guard(title, action) {
    return async () => {
        try {
            await action();
        } catch (error) {
            handleError(title, error);
        }
    };
}

function toast(message, error = false) {
    const element = document.createElement("div");
    element.className = `toast${error ? " error" : ""}`;
    element.textContent = message;
    $("#toast-region").append(element);
    setTimeout(() => element.remove(), 3600);
}

function openLab() {
    $("#lab-drawer").classList.add("open");
    $("#lab-drawer").setAttribute("aria-hidden", "false");
    $("#drawer-backdrop").hidden = false;
}

function closeLab() {
    $("#lab-drawer").classList.remove("open");
    $("#lab-drawer").setAttribute("aria-hidden", "true");
    $("#drawer-backdrop").hidden = true;
}

function safeJson(text) {
    try { return JSON.parse(text); } catch { return text; }
}

function shortId(value) {
    const text = String(value ?? "");
    return text.length > 10 ? text.slice(0, 8) : text;
}

function formatTime(value) {
    if (!value) return "방금";
    return new Date(value).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
}
