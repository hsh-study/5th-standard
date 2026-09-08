import { createChatState, mergeMessage, applyHistory, readCandidate, drainForward } from "/chat-state.mjs";
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
    manualDisconnect: false,
    reconnectAttempt: 0,
    reconnectTimer: null,
    reconnectStableTimer: null,
    messages: new Map(),
    lastCommand: null,
    chat: createChatState(),
    contextEpoch: 0,
    requestController: new AbortController(),
    syncInFlight: null,
    syncTimer: null,
    syncAttempt: 0,
    initializing: false
};

state.messages = state.chat.messages;
bindEvents();
renderAuthState();
updateLastSeenId();
logEvent("UI", "25회차 Cursor 조회 관찰 화면을 열었습니다.");

function bindEvents() {
    $$(".member-option").forEach(button => button.addEventListener("click", () => selectMember(button.dataset.member)));
    $("#start-journey").addEventListener("click", startJourney);
    $("#disconnect").addEventListener("click", disconnect);
    $("#message-form").addEventListener("submit", sendMessage);
    $("#retry-last").addEventListener("click", retryLastMessage);
    $("#load-older").addEventListener("click", guard("이전 대화 조회 실패", loadOlder));
    $("#load-history").addEventListener("click", guard("메시지 동기화 실패", () => synchronizeMessages()));
    $("#mark-read").addEventListener("click", guard("읽음 처리 실패", markRead));
    $("#open-lab").addEventListener("click", openLab);
    $("#close-lab").addEventListener("click", closeLab);
    $("#drawer-backdrop").addEventListener("click", closeLab);
    $("#clear-log").addEventListener("click", () => $("#event-log").replaceChildren());
    $("#sale-id").addEventListener("input", event => {
        resetJourney();
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
    resetJourney();
    state.memberId = memberId;
    $("#member-id").value = memberId;
    $$(".member-option").forEach(button => button.classList.toggle("active", button.dataset.member === memberId));
}

async function startJourney() {
    const button = $("#start-journey");
    button.disabled = true;
    resetJourney();
    const epoch = state.contextEpoch;
    try {
        setStep("login", "active");
        await login();
        setStep("login", "done");
        await joinSale();
        setStep("join", "done");
        await initializeChat();
        await connectStomp();
        setStep("connect", "done");
        setStep("subscribe", "done");
        await synchronizeMessages();
        await refreshUnread();
    } catch (error) {
        console.error(error);
        if (epoch === state.contextEpoch) handleError("입장 실패", error);
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
    const epoch = state.contextEpoch;
    state.manualDisconnect = false;
    setConnection("connecting", "연결 중");
    const wsProtocol = location.protocol === "https:" ? "wss:" : "ws:";
    const client = new CourseStompClient(`${wsProtocol}//${location.host}/ws`, {
        onDebug: message => logEvent("STOMP", message),
        onFrame: frame => logEvent("FRAME", frame.command),
        onError: error => { if (state.stomp === client) handleError("STOMP 오류", error); },
        onClose: close => {
            if (state.stomp !== client || epoch !== state.contextEpoch) return;
            state.connected = false;
            clearTimeout(state.reconnectStableTimer);
            disableComposer();
            setConnection("disconnected", `연결 종료 · ${close.code}`);

        }
    });
    state.stomp = client;
    await client.connect({ Authorization: `Bearer ${state.token}` });
    if (epoch !== state.contextEpoch || state.stomp !== client) { client.disconnect(); return; }
    state.connected = true;
    client.subscribe(`/topic/live-sales/${state.saleId}`, frame => {
        if (epoch === state.contextEpoch && state.stomp === client) receiveChat(JSON.parse(frame.body), "MESSAGE");
    });
    setConnection("connected", "연결됨 · 구독 요청 전송");

    clearTimeout(state.reconnectStableTimer);
    state.reconnectStableTimer = setTimeout(() => state.reconnectAttempt = 0, 5000);
    $("#disconnect").disabled = false;
    $("#message-input").disabled = false;
    $("#send-message").disabled = false;
    updateLastSeenId();
}

function disconnect() {
    state.manualDisconnect = true;
    cancelChatWork();
    const client = state.stomp;
    state.stomp = null;
    client?.disconnect();
    state.connected = false;
    setConnection("disconnected", "연결 종료 · 목록 유지");
    disableComposer();
    updateLastSeenId();
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
    if (!state.lastCommand || !state.connected) return;
    publishChat(state.lastCommand);
    logEvent("RETRY", `동일 clientMessageId 재전송 · ${shortId(state.lastCommand.clientMessageId)}`);
}

function publishChat(command) {
    state.stomp.publish(`/app/live-sales/${state.saleId}/messages`, JSON.stringify(command));
    logEvent("SEND", `${shortId(command.clientMessageId)} · ${command.content.slice(0, 24)}`);
}

function receiveChat(message, source) {
    mergeMessage(state.chat, message);
    renderMessages();
    updateLastSeenId();
    logEvent(source, `id=${message.id} · sender=${message.senderId}`);
}

async function synchronizeMessages(all = false) {
    if (!state.token || !state.joined) throw new Error("먼저 로그인하고 입장하세요.");
    if (!state.chat.initialized) { await initializeChat(); }
    if (state.syncInFlight) return state.syncInFlight;
    const epoch = state.contextEpoch;
    const chat = state.chat;
    const task = drainForward(chat, cursor => api(`${chatPath()}/messages?cursor=${cursor}&size=30`), {
        all, current: () => epoch === state.contextEpoch,
        onPage: page => { renderMessages(); updateLastSeenId(); logEvent("CURSOR", `next=${page.nextCursor} · hasNext=${page.hasNext}`); }
    });
    state.syncInFlight = task;
    try {
        const complete = await task;
        if (epoch !== state.contextEpoch) return false;
        state.syncAttempt = 0;
        if (complete) { chat.status = 'complete'; toast("조회 완료 · 읽음 위치는 유지했습니다."); }
        else if (all) scheduleSync(0);
        updateLastSeenId();
        return complete;
    } catch (error) {
        if (epoch === state.contextEpoch) {
            chat.status = 'failed';

            updateLastSeenId();
        }
        throw error;
    } finally {
        if (state.syncInFlight === task) state.syncInFlight = null;
    }
}

async function markRead() {
    const candidate = readCandidate(state.chat);
    if (candidate === null) throw new Error("이전 미읽음 대화를 더 불러오거나 새 메시지를 동기화하세요.");
    const snapshot = await api(`${chatPath()}/mark-read`, { method: "POST", body: { lastReadId: candidate } });
    state.chat.serverLastReadId = snapshot.lastReadId;
    updateLastSeenId();
    await refreshUnread();
}

async function refreshUnread() {
    const result = await api(`${chatPath()}/unread-count`);
    $("#composer-help").textContent = `미읽음 ${result.count}개 · 저장된 읽음 ID ${state.chat.serverLastReadId}`;
}

async function api(path, options = {}) {
    const epoch = state.contextEpoch;
    const signal = state.requestController.signal;
    const method = options.method ?? "GET";
    const headers = { ...(options.headers ?? {}) };
    if (options.auth !== false && state.token) headers.Authorization = `Bearer ${state.token}`;
    if (options.body !== undefined) headers["Content-Type"] = "application/json";
    logEvent("HTTP", `${method} ${path}`);
    const response = await fetch(path, {
        signal,
        method,
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body)
    });
    const text = await response.text();
    if (epoch !== state.contextEpoch) throw new DOMException("이전 화면 요청", "AbortError");
    const data = text ? safeJson(text) : null;
    if (!response.ok) { const error = new Error(`${response.status} ${data?.message || data?.detail || text || response.statusText}`.trim()); error.status = response.status; throw error; }
    return data;
}

function renderAuthState() {
    const loggedIn = Boolean(state.token);
    $("#member-badge").textContent = loggedIn ? state.memberId.toUpperCase() : "GUEST";
    setGlobalStatus(loggedIn ? "success" : "neutral", loggedIn ? `${state.memberId} 로그인` : "로그인 전");
}

function resetJourney() {
    disconnect();
    ["login", "join", "connect", "subscribe"].forEach(step => setStep(step, ""));
    state.token = null;
    state.joined = false;
    state.chat = createChatState();
    state.messages = state.chat.messages;
    state.lastCommand = null;
    state.reconnectAttempt = 0;
    state.syncAttempt = 0;
    $("#retry-last").disabled = true;
    renderAuthState();
    renderMessages();
    updateLastSeenId();
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
        const idBadge = document.createElement("span");
        idBadge.className = "message-id";
        idBadge.textContent = `ID ${message.id} · ${shortId(message.clientMessageId)}`;
        article.append(meta, bubble, idBadge);
        list.append(article);
    });
    list.scrollTop = list.scrollHeight;
}

function disableComposer() {
    $("#disconnect").disabled = true;
    $("#message-input").disabled = true;
    $("#send-message").disabled = true;
    $("#mark-read").disabled = true;
    $("#composer-help").textContent = "연결을 완료하면 메시지를 보낼 수 있습니다.";
}

function updateLastSeenId() {
    const chat = state.chat;
    $("#last-id-label").textContent = `조회 ${chat.syncCursor} · 수신 ${chat.maxReceivedId} · 읽음 ${chat.serverLastReadId} · ${chat.status}`;
    $("#mark-read").disabled = readCandidate(chat) === null;
    $("#load-older").disabled = !state.joined || (chat.initialized && !chat.hasPrevious);
    $("#load-older").textContent = chat.initialized ? "이전 대화 더 보기" : "최초 대화 다시 조회";
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

function chatPath() { return `/api/live-sales/${encodeURIComponent(state.saleId)}/chat`; }
function cancelChatWork() {
    state.contextEpoch++;
    state.requestController.abort();
    state.requestController = new AbortController();
    clearTimeout(state.syncTimer);
    clearTimeout(state.reconnectTimer);
    clearTimeout(state.reconnectStableTimer);
    state.syncTimer = state.reconnectTimer = null;
    state.syncInFlight = null;
    state.initializing = false;
    state.historyLoading = false;
}
async function initializeChat() {
    if (state.initializing) throw new Error("최초 대화 조회 중입니다.");
    const epoch = state.contextEpoch;
    state.initializing = true;
    try {
        const snapshot = await api(`${chatPath()}/read-state`);
        const page = await api(`${chatPath()}/messages/history?size=30`);
        state.chat.serverLastReadId = snapshot.lastReadId;
        applyHistory(state.chat, page, true);
        renderMessages();
        updateLastSeenId();
    } finally { if (epoch === state.contextEpoch) state.initializing = false; }
}
async function loadOlder() {
    if (!state.joined) throw new Error("먼저 입장하세요.");
    if (!state.chat.initialized) return initializeChat();
    if (!state.chat.hasPrevious || state.historyLoading) return;
    const epoch = state.contextEpoch;
    state.historyLoading = true;
    const list = $("#message-list"), previousHeight = list.scrollHeight, previousTop = list.scrollTop;
    try {
        const page = await api(`${chatPath()}/messages/history?before=${state.chat.beforeCursor}&size=30`);
        applyHistory(state.chat, page);
        renderMessages();
        list.scrollTop = previousTop + list.scrollHeight - previousHeight;
        updateLastSeenId();
    } finally { if (epoch === state.contextEpoch) state.historyLoading = false; }
}
function stopOnAuth(error) {
    if (error.status !== 401 && error.status !== 403) return false;
    disconnect();
    toast("인증 또는 참여 권한을 확인하고 다시 입장하세요.", true);
    return true;
}
function scheduleSync(delay) {
    clearTimeout(state.syncTimer);
    const epoch = state.contextEpoch;
    state.syncTimer = setTimeout(() => {
        state.syncTimer = null;
        if (epoch === state.contextEpoch && !state.manualDisconnect) synchronizeMessages(true).catch(error => handleError("동기화 실패", error));
    }, delay);
}
