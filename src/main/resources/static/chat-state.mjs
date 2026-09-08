/** 화면 중복, 조회 경계, 사용자 읽음 경계는 서로 다른 상태다. */
export function createChatState() {
    return { messages: new Map(), syncCursor: 0, maxReceivedId: 0, beforeCursor: null,
        hasPrevious: false, serverLastReadId: 0, initialized: false, status: 'idle' };
}
export function mergeMessage(state, message) {
    state.messages.set(String(message.messageId), message);
    state.maxReceivedId = Math.max(state.maxReceivedId, Number(message.id));
    // 실시간 수신은 REST가 중간 구간까지 확인했다는 증거가 아니다.
}
export function applyHistory(state, page, initial = false) {
    page.items.forEach(message => mergeMessage(state, message));
    state.beforeCursor = page.beforeCursor;
    state.hasPrevious = page.hasPrevious;
    if (initial) {
        state.syncCursor = page.items.length ? Number(page.items.at(-1).id) : 0;
        state.initialized = true;
    }
}
export function applyForward(state, page) {
    const next = Number(page.nextCursor);
    if ((page.items.length || page.hasNext) && (!Number.isSafeInteger(next) || next <= state.syncCursor)) {
        throw new Error('전진하지 않는 nextCursor 응답');
    }
    page.items.forEach(message => mergeMessage(state, message));
    if (page.items.length) state.syncCursor = next;
    state.status = page.hasNext ? 'pending' : 'complete';
}
export function readCandidate(state) {
    // 최근 30건만 로딩했다면 그 앞의 미읽음 구간을 건너뛰지 않는다.
    // 숫자의 연속성이 아닌, before 조회로 확보한 구간 경계를 판단한다.
    const covered = !state.hasPrevious || Number(state.beforeCursor) <= state.serverLastReadId;
    return state.initialized && covered && state.syncCursor > state.serverLastReadId ? state.syncCursor : null;
}

export async function drainForward(state, fetchPage, { all = false, current = () => true,
    onPage = () => {}, maxPages = 10, now = Date.now, budgetMs = 5000 } = {}) {
    const started = now();
    for (let count = 0; count < maxPages; count++) {
        const page = await fetchPage(state.syncCursor);
        if (!current()) return false;
        applyForward(state, page);
        onPage(page);
        if (!page.hasNext) return true;
        if (!all || now() - started >= budgetMs) return false;
    }
    return false;
}
