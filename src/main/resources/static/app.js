"use strict";

const STORAGE_KEY = "campus-ai-chat-state-v1";
const MAX_CONTEXT_MESSAGES = 20;
const elements = {
    chatList: document.getElementById("chat-list"),
    clearChat: document.getElementById("clear-chat"),
    chatForm: document.getElementById("chat-form"),
    chatMode: document.getElementById("chat-mode"),
    conversation: document.getElementById("conversation"),
    messageInput: document.getElementById("message-input"),
    messages: document.getElementById("messages"),
    newChat: document.getElementById("new-chat"),
    notice: document.getElementById("notice"),
    sendButton: document.getElementById("send-button"),
    themeToggle: document.getElementById("theme-toggle"),
    welcome: document.getElementById("welcome")
};

function newChat() {
    return { id: crypto.randomUUID(), title: "New chat", mode: "general", messages: [], updatedAt: Date.now() };
}

function loadState() {
    try {
        const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
        if (stored && Array.isArray(stored.chats) && stored.chats.length > 0) {
            const chats = stored.chats.filter(chat =>
                chat && typeof chat.id === "string" && Array.isArray(chat.messages));
            if (chats.length > 0) {
                const activeChat = chats.find(chat => chat.id === stored.activeChatId) || chats[0];
                return {
                    chats,
                    activeChatId: activeChat.id,
                    theme: stored.theme === "dark" ? "dark" : "light"
                };
            }
        }
    } catch {
        // A missing or unreadable saved chat should not prevent a fresh chat from loading.
    }
    return { chats: [newChat()], activeChatId: null, theme: "light" };
}

const state = loadState();
if (!state.activeChatId) state.activeChatId = state.chats[0].id;

function activeChat() {
    return state.chats.find(chat => chat.id === state.activeChatId) || state.chats[0];
}

function saveState() {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
        showNotice("This browser could not save your chat history. Check its storage settings.");
    }
}

function showNotice(message) {
    elements.notice.textContent = message;
    elements.notice.hidden = false;
}

function clearNotice() {
    elements.notice.textContent = "";
    elements.notice.hidden = true;
}

function escapeHtml(text) {
    return text.replace(/[&<>"']/g, character => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"
    })[character]);
}

function inlineMarkdown(text) {
    return escapeHtml(text)
        .replace(/`([^`]+)`/g, "<code>$1</code>")
        .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
        .replace(/\*([^*]+)\*/g, "<em>$1</em>");
}

function renderMarkdown(text) {
    const blocks = text.split(/```([^\n`]*)\n([\s\S]*?)```/g);
    let html = "";
    for (let index = 0; index < blocks.length; index += 1) {
        if (index % 3 === 1) {
            const code = blocks[index + 1] || "";
            html += `<pre><code>${escapeHtml(code.replace(/\n$/, ""))}</code></pre>`;
            index += 1;
            continue;
        }

        let listType = null;
        const lines = blocks[index].split("\n");
        for (const line of lines) {
            const unordered = line.match(/^\s*[-*]\s+(.+)$/);
            const ordered = line.match(/^\s*\d+\.\s+(.+)$/);
            const item = unordered || ordered;
            if (item) {
                const nextType = unordered ? "ul" : "ol";
                if (listType !== nextType) {
                    if (listType) html += `</${listType}>`;
                    html += `<${nextType}>`;
                    listType = nextType;
                }
                html += `<li>${inlineMarkdown(item[1])}</li>`;
                continue;
            }
            if (listType) {
                html += `</${listType}>`;
                listType = null;
            }
            if (!line.trim()) continue;

            const heading = line.match(/^(#{1,3})\s+(.+)$/);
            const quote = line.match(/^>\s?(.*)$/);
            if (heading) {
                const level = heading[1].length + 1;
                html += `<h${level}>${inlineMarkdown(heading[2])}</h${level}>`;
            } else if (quote) {
                html += `<blockquote>${inlineMarkdown(quote[1])}</blockquote>`;
            } else {
                html += `<p>${inlineMarkdown(line)}</p>`;
            }
        }
        if (listType) html += `</${listType}>`;
    }
    return html;
}

function renderChatList() {
    elements.chatList.replaceChildren();
    [...state.chats].sort((a, b) => b.updatedAt - a.updatedAt).forEach(chat => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `chat-item${chat.id === state.activeChatId ? " active" : ""}`;
        button.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M20 11.5a7.5 7.5 0 0 1-8 7.5 8.4 8.4 0 0 1-3.5-.8L4 20l1.4-3.5A7.2 7.2 0 0 1 4 12c0-4.1 3.6-7.5 8-7.5s8 3.1 8 7Z"/></svg>';
        const title = document.createElement("span");
        title.textContent = chat.title || "New chat";
        button.append(title);
        button.addEventListener("click", () => {
            state.activeChatId = chat.id;
            elements.chatMode.value = chat.mode || "general";
            clearNotice();
            render();
            saveState();
        });
        elements.chatList.append(button);
    });
}

function renderMessages() {
    const chat = activeChat();
    elements.messages.replaceChildren();
    elements.welcome.hidden = chat.messages.length > 0;

    chat.messages.forEach(message => {
        const row = document.createElement("article");
        row.className = `message ${message.role}`;
        const avatar = document.createElement("div");
        avatar.className = "avatar";
        avatar.setAttribute("aria-hidden", "true");
        avatar.textContent = message.role === "user" ? "You" : "✦";

        const body = document.createElement("div");
        body.className = "message-body";
        const content = document.createElement("div");
        content.className = "message-content";
        if (message.role === "assistant") {
            content.innerHTML = renderMarkdown(message.content);
            const copy = document.createElement("button");
            copy.type = "button";
            copy.className = "copy-button";
            copy.textContent = "Copy answer";
            copy.addEventListener("click", async () => {
                try {
                    await navigator.clipboard.writeText(message.content);
                    copy.textContent = "Copied";
                    window.setTimeout(() => { copy.textContent = "Copy answer"; }, 1400);
                } catch {
                    showNotice("Could not copy the answer. Select the text and copy it manually.");
                }
            });
            body.append(content, copy);
        } else {
            content.textContent = message.content;
            body.append(content);
        }
        row.append(avatar, body);
        elements.messages.append(row);
    });

    if (chat.loading) {
        const row = document.createElement("article");
        row.className = "message assistant";
        row.setAttribute("aria-label", "Campus AI is generating a reply");
        row.innerHTML = '<div class="avatar" aria-hidden="true">✦</div><div class="message-body"><div class="typing" aria-hidden="true"><span></span><span></span><span></span></div></div>';
        elements.messages.append(row);
    }
    elements.conversation.scrollTop = elements.conversation.scrollHeight;
}

function render() {
    document.documentElement.dataset.theme = state.theme;
    elements.themeToggle.textContent = state.theme === "dark" ? "☀" : "☾";
    elements.themeToggle.setAttribute("aria-label", state.theme === "dark" ? "Switch to light theme" : "Switch to dark theme");
    renderChatList();
    renderMessages();
}

function startNewChat() {
    const chat = newChat();
    state.chats.unshift(chat);
    state.activeChatId = chat.id;
    elements.chatMode.value = "general";
    clearNotice();
    render();
    saveState();
    elements.messageInput.focus();
}

function updateChatTitle(chat) {
    if (chat.title !== "New chat") return;
    const firstMessage = chat.messages.find(message => message.role === "user");
    if (firstMessage) {
        chat.title = firstMessage.content.trim().slice(0, 34) || "New chat";
        if (firstMessage.content.trim().length > 34) chat.title += "…";
    }
}

async function sendMessage(text) {
    const prompt = text.trim();
    if (!prompt || activeChat().loading) return;

    const chat = activeChat();
    clearNotice();
    const history = chat.messages.slice(-MAX_CONTEXT_MESSAGES).map(message => ({
        role: message.role,
        content: message.content
    }));
    chat.messages.push({ role: "user", content: prompt });
    chat.mode = elements.chatMode.value;
    chat.loading = true;
    chat.updatedAt = Date.now();
    updateChatTitle(chat);
    render();
    saveState();
    elements.messageInput.value = "";
    resizeInput();
    elements.sendButton.disabled = true;

    try {
        const response = await fetch("/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: prompt, history, mode: chat.mode })
        });
        const result = await response.json();
        if (!response.ok) {
            throw new Error(result.error || "The request could not be completed. Please try again.");
        }
        if (typeof result.answer !== "string" || !result.answer.trim()) {
            throw new Error("The AI returned an empty reply. Please try again.");
        }
        chat.messages.push({ role: "assistant", content: result.answer });
        chat.updatedAt = Date.now();
    } catch (error) {
        const message = error instanceof TypeError
            ? "Could not connect to Campus AI. Check that the app is running and your internet connection is available."
            : error.message;
        showNotice(message);
    } finally {
        chat.loading = false;
        elements.sendButton.disabled = false;
        updateChatTitle(chat);
        render();
        saveState();
        elements.messageInput.focus();
    }
}

function resizeInput() {
    elements.messageInput.style.height = "auto";
    elements.messageInput.style.height = `${Math.min(elements.messageInput.scrollHeight, 160)}px`;
}

elements.newChat.addEventListener("click", startNewChat);
elements.clearChat.addEventListener("click", () => {
    const chat = activeChat();
    chat.messages = [];
    chat.title = "New chat";
    chat.updatedAt = Date.now();
    clearNotice();
    render();
    saveState();
    elements.messageInput.focus();
});
elements.themeToggle.addEventListener("click", () => {
    state.theme = state.theme === "dark" ? "light" : "dark";
    render();
    saveState();
});
elements.chatMode.addEventListener("change", () => {
    activeChat().mode = elements.chatMode.value;
    saveState();
});
elements.chatForm.addEventListener("submit", event => {
    event.preventDefault();
    sendMessage(elements.messageInput.value);
});
elements.messageInput.addEventListener("input", resizeInput);
elements.messageInput.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        elements.chatForm.requestSubmit();
    }
});
document.querySelectorAll("[data-prompt]").forEach(button => {
    button.addEventListener("click", () => sendMessage(button.dataset.prompt));
});

elements.chatMode.value = activeChat().mode || "general";
render();
saveState();
