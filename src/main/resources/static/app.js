import { CourseStompClient } from "/stomp-client.js";

const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];

const state = {
    token: null,
    memberId: "member-1",
    saleId: "sale-1",
    joined: false,
    stomp: null,
    connected: false
};

bindEvents();
renderAuthState();
logEvent("UI", "22회차 연결 관찰 화면을 열었습니다.");

function bindEvents() {
    $$(".member-option").forEach(button => button.addEventListener("click", () => selectMember(button.dataset.member)));
    $("#start-journey").addEventListener("click", startJourney);
    $("#disconnect").addEventListener("click", disconnect);
    $("#open-lab").addEventListener("click", openLab);
    $("#close-lab").addEventListener("click", closeLab);
    $("#drawer-backdrop").addEventListener("click", closeLab);
    $("#clear-log").addEventListener("click", () => $("#event-log").replaceChildren());
    $("#sale-id").addEventListener("input", event => {
        state.saleId = event.target.value.trim();
        $("#room-title").textContent = state.saleId || "채티방";
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
    if (!state.saleId) throw new Error("채티방 ID가 필요합니다.");
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
    client.subscribe(`/topic/live-sales/${state.saleId}`, frame => logEvent("MESSAGE", frame.body));
    setConnection("connected", "구독 완료");
    $("#disconnect").disabled = false;
}

function disconnect() {
    state.stomp?.disconnect();
    state.connected = false;
    setConnection("disconnected", "연결 종료");
    $("#disconnect").disabled = true;
    logEvent("STOMP", "사용자가 DISCONNECT를 요청했습니다.");
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
    renderAuthState();
    setConnection("disconnected", "연결 전");
    $("#disconnect").disabled = true;
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
