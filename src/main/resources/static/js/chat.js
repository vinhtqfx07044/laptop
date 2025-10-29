// Defines the ChatWidget class to manage chat functionality.
class ChatWidget {
    isOpen = false; // Tracks if the chat box is open.
    isStreaming = false; // Tracks if a message stream is active.
    eventSource = null; // Holds the EventSource object for server-sent events.
    conversationId = null; // Stores the current conversation ID.
    storageKey = 'chatWidgetState'; // Session storage key

    constructor() {
        this.init(); // Calls the initialization method.
    }

    /**
     * Renders markdown to safe HTML with support for bold, italic, headings, and links.
     * @param {string} text - The markdown text to render.
     * @returns {string} The safe HTML content.
     */
    renderMarkdown(text) {
        // First escape HTML to prevent XSS
        let html = this.escapeHtml(text);

        // Replace markdown patterns with safe HTML - order matters!
        html = html
            // Headings (h1-h6) - must be before links to avoid conflicts
            .replaceAll(/^# (.*$)/gm, '<h1>$1</h1>')              // # Heading 1
            .replaceAll(/^## (.*$)/gm, '<h2>$1</h2>')             // ## Heading 2
            .replaceAll(/^### (.*$)/gm, '<h3>$1</h3>')            // ### Heading 3
            .replaceAll(/^#### (.*$)/gm, '<h4>$1</h4>')           // #### Heading 4
            .replaceAll(/^##### (.*$)/gm, '<h5>$1</h5>')          // ##### Heading 5
            .replaceAll(/^###### (.*$)/gm, '<h6>$1</h6>')         // ###### Heading 6
            // Links - must be after headings to avoid conflicts
            .replaceAll(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
            // Bold and italic
            .replaceAll(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')  // **bold**
            .replaceAll(/\*(.*?)\*/g, '<em>$1</em>')              // *italic*
            // Line breaks
            .replaceAll('\n', '<br>');                            // line breaks

        return html;
    }

    /**
     * Escapes HTML characters to prevent XSS vulnerabilities.
     * @param {string} text - The text to escape.
     * @returns {string} The HTML-escaped string.
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    init() {
        this.loadState();
        this.attachEvents();
    }

    /**
     * Saves the current chat state to session storage.
     */
    saveState() {
        try {
            const messages = this.getMessages();
            const state = {
                conversationId: this.conversationId,
                messages: messages,
                isOpen: this.isOpen
            };

            console.log('Saving chat state:', {
                conversationId: this.conversationId,
                messageCount: messages.length,
                isOpen: this.isOpen,
                messages: messages.map(m => ({ type: m.type, content: m.content.substring(0, 50) + '...' }))
            });

            sessionStorage.setItem(this.storageKey, JSON.stringify(state));
        } catch (error) {
            console.warn('Không thể lưu trạng thái chat:', error);
        }
    }

    /**
     * Loads chat state from session storage.
     */
    loadState() {
        try {
            const savedState = sessionStorage.getItem(this.storageKey);
            if (savedState) {
                const state = JSON.parse(savedState);

                console.log('Loading chat state:', {
                    conversationId: state.conversationId,
                    messageCount: state.messages ? state.messages.length : 0,
                    isOpen: state.isOpen,
                    messages: state.messages ? state.messages.map(m => ({ type: m.type, content: m.content.substring(0, 50) + '...' })) : []
                });

                this.conversationId = state.conversationId || null;
                this.isOpen = state.isOpen || false;

                // Restore messages if they exist
                if (state.messages && state.messages.length > 0) {
                    this.restoreMessages(state.messages);
                }

                // Restore chat box visibility
                this.restoreChatBoxVisibility();
            }
        } catch (error) {
            console.warn('Không thể khôi phục trạng thái chat:', error);
            // If there's an error loading state, ensure we have a clean state
            this.newChat();
        }
    }

    /**
     * Gets all messages from the chat container.
     */
    getMessages() {
        const container = document.getElementById('chat-messages');
        if (!container) return [];

        const messages = [];
        const messageElements = container.querySelectorAll('.message');

        for (const element of messageElements) {
            const type = this.getMessageType(element);
            const content = this.getMessageContent(element);

            // Only save messages that have actual content
            if (content !== null && content !== undefined) {
                messages.push({ type, content });
            }
        }

        return messages;
    }

    /**
     * Determines message type from element classes.
     */
    getMessageType(element) {
        if (element.classList.contains('user')) return 'user';
        if (element.classList.contains('bot')) return 'bot';
        if (element.classList.contains('error')) return 'error';
        return 'bot'; // default
    }

    /**
     * Extracts message content from element, excluding icons and typing indicators.
     */
    getMessageContent(element) {
        // Clone the element to avoid modifying the original
        const clone = element.cloneNode(true);

        // Remove all icons and typing indicators
        const icons = clone.querySelectorAll('i, .typing-indicator');
        for (const icon of icons) {
            icon.remove();
        }

        // Get the remaining text content
        const content = clone.textContent.trim();

        // Check if this is an empty or loading bot message
        if (!content && element.classList.contains('bot')) {
            return null; // Don't save empty/loading bot messages
        }

        return content;
    }

    /**
     * Restores messages to the chat container.
     */
    restoreMessages(messages) {
        const container = document.getElementById('chat-messages');
        if (!container) return;

        container.innerHTML = '';
        for (const message of messages) {
            this.addMessageWithoutScrolling(message.type, message.content);
        }

        this.scrollToBottom();
    }

    /**
     * Adds a message without auto-scrolling (used during bulk restore).
     */
    addMessageWithoutScrolling(type, content) {
        const container = document.getElementById('chat-messages');
        const message = document.createElement('div');
        message.className = `message ${type}`;

        if (type === 'user') {
            message.innerHTML = `<i class="fas fa-user text-white me-2"></i>${this.escapeHtml(content)}`;
        } else if (type === 'error') {
            message.innerHTML = `<i class="fas fa-exclamation-triangle text-danger me-2"></i>${this.escapeHtml(content)}`;
        } else {
            message.innerHTML = `<i class="fas fa-robot text-primary me-2"></i>${this.escapeHtml(content)}`;
        }

        container.appendChild(message);
        return message;
    }

    /**
     * Restores chat box visibility based on saved state.
     */
    restoreChatBoxVisibility() {
        const chatBox = document.getElementById('chat-box');
        const toggle = document.getElementById('chat-toggle');

        if (!chatBox || !toggle) return;

        chatBox.style.display = this.isOpen ? 'block' : 'none';
        toggle.innerHTML = this.isOpen ? '<i class="fas fa-times"></i>' : '<i class="fas fa-comments"></i>';

        if (this.isOpen) {
            // Delay scroll to ensure DOM is ready
            setTimeout(() => this.scrollToBottom(), 100);
        }
    }

    /**
     * Clears saved chat state.
     */
    clearSavedState() {
        try {
            sessionStorage.removeItem(this.storageKey);
        } catch (error) {
            console.warn('Không thể xóa trạng thái chat:', error);
        }
    }

    /**
     * Attaches event listeners to various chat UI elements.
     */
    attachEvents() {
        const toggleBtn = document.getElementById('chat-toggle');
        const chatForm = document.getElementById('chat-form');
        const newChatBtn = document.getElementById('new-chat-btn');
        const chatInput = document.getElementById('chat-input');

        if (toggleBtn) toggleBtn.onclick = () => this.toggle();
        if (chatForm) chatForm.onsubmit = (e) => this.sendMessage(e);
        if (newChatBtn) newChatBtn.onclick = () => this.newChat();
        if (chatInput) {
            chatInput.onkeydown = (e) => {
                // Sends message on Enter key press, unless Shift is also pressed (for new line).
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault(); // Prevents default Enter behavior (e.g., new line).
                    this.sendMessage(e);
                }
            };
        }
    }

    toggle() {
        const chatBox = document.getElementById('chat-box');
        const toggle = document.getElementById('chat-toggle');

        this.isOpen = !this.isOpen; // Toggles the open state.
        chatBox.style.display = this.isOpen ? 'block' : 'none'; // Shows or hides the chat box.
        // Changes the toggle button icon based on the chat box state.
        toggle.innerHTML = this.isOpen ? '<i class="fas fa-times"></i>' : '<i class="fas fa-comments"></i>';

        // Save state after toggling
        this.saveState();

        if (this.isOpen) this.scrollToBottom();
    }

    /**
     * Sends a message from the user to the chat.
     * @param {Event} e - The event object (e.g., form submission event).
     */
    async sendMessage(e) {
        e.preventDefault();
        if (this.isStreaming) return; // Prevents sending new messages while streaming.

        const input = document.getElementById('chat-input');
        const message = input.value.trim(); // Gets and trims the message from the input field.

        // Validates message length.
        if (!message || message.length > 200) {
            this.addMessage('error', message.length > 200 ? 'Tin nhắn quá dài (tối đa 200 ký tự).' : 'Vui lòng nhập tin nhắn.');
            return;
        }

        input.value = '';
        this.addMessage('user', message);
        this.setStreaming(true);

        try {
            await this.streamResponse(message);
        } finally {
            this.setStreaming(false);
        }
    }

    /**
     * Streams the bot's response from the server using Server-Sent Events (SSE).
     * @param {string} message - The user's message to send.
     * @returns {Promise<void>} A promise that resolves when the stream is complete.
     */
    streamResponse(message) {
        return new Promise((resolve) => {
            // Generates a conversation ID on the first message if one doesn't exist.
            if (!this.conversationId) {
                this.conversationId = crypto.randomUUID();
            }

            // Hiển thị loading indicator ngay lập tức
            const botMessage = this.addMessage('bot', '');
            botMessage.innerHTML = `<i class="fas fa-robot text-primary me-2"></i><span class="typing-indicator"><span></span><span></span><span></span></span>`;

            // Prepares URL parameters for the SSE request.
            const params = new URLSearchParams({
                message,
                conversationId: this.conversationId
            });

            const url = `/api/chat/stream?${params}`;
            this.eventSource = new EventSource(url);
            let content = '';
            let hasReceivedMessage = false;

            // Handles incoming messages from the SSE stream.
            this.eventSource.onmessage = (event) => {
                hasReceivedMessage = true;

                const chatResponse = JSON.parse(event.data);
                const chunk = chatResponse.results?.[0]?.output?.text || '';

                // Checks if the chunk indicates an error message.
                if (this.isErrorMessage(chunk)) {
                    this.handleErrorMessage(botMessage, chunk);
                    this.cleanup();
                    resolve();
                    return;
                }

                content += chunk;

                // Updates the bot message display.
                botMessage.innerHTML = `<i class="fas fa-robot text-primary me-2"></i><div class="bot-message">${this.renderMarkdown(content)}</div>`;
                this.scrollToBottom();

                // Save state during streaming to capture partial bot responses
                this.saveState();
            };

            // Handles errors from the SSE stream.
            this.eventSource.onerror = (error) => {
                // Only shows an error message if no data has been received (indicates a connection error).
                if (!hasReceivedMessage) {
                    // Xóa loading indicator
                    if (botMessage?.parentNode) {
                        botMessage.remove();
                    }
                    this.addMessage('error', 'Không thể kết nối tới server. Vui lòng thử lại sau.');
                }

                this.cleanup();
                resolve();
            };

            // Event listener for when the SSE connection is closed.
            this.eventSource.addEventListener('close', () => {
                this.cleanup();
                // Save final state after the conversation is complete
                this.saveState();
                resolve();
            });
        });
    }

    /**
     * Checks if a given chunk of text contains an error message.
     * @param {string} chunk - The text chunk to check.
     * @returns {boolean} True if the chunk contains an error message, false otherwise.
     */
    isErrorMessage(chunk) {
        return chunk && (chunk.includes('Quá nhiều yêu cầu') ||
            chunk.includes('Vui lòng nhập tin nhắn') ||
            chunk.includes('Đã xảy ra lỗi') ||
            chunk.includes('Đã đạt giới hạn'));
    }

    /**
     * Handles and displays error messages in the chat.
     * @param {HTMLElement} botMessage - The bot message element to potentially remove.
     * @param {string} errorText - The error message to display.
     */
    handleErrorMessage(botMessage, errorText) {
        if (botMessage?.parentNode) {
            botMessage.remove(); // Removes the incomplete bot message.
        }
        this.addMessage('error', errorText);
    }

    /**
     * Adds a message to the chat display.
     * @param {'user' | 'bot' | 'error'} type - The type of message ('user', 'bot', or 'error').
     * @param {string} content - The content of the message.
     * @returns {HTMLElement} The created message element.
     */
    addMessage(type, content) {
        const container = document.getElementById('chat-messages');
        const message = document.createElement('div');
        message.className = `message ${type}`; // Sets CSS class based on message type.

        // Sets inner HTML with appropriate icon and rendered content.
        if (type === 'user') {
            // User messages: escape HTML to prevent any formatting
            message.innerHTML = `<i class="fas fa-user text-white me-2"></i><span class="user-message">${this.escapeHtml(content)}</span>`;
        } else if (type === 'error') {
            // Error messages: escape HTML to prevent any formatting
            message.innerHTML = `<i class="fas fa-exclamation-triangle text-danger me-2"></i><span class="error-message">${this.escapeHtml(content)}</span>`;
        } else {
            // Bot messages: render markdown for rich formatting
            message.innerHTML = `<i class="fas fa-robot text-primary me-2"></i><div class="bot-message">${this.renderMarkdown(content)}</div>`;
        }

        container.appendChild(message);
        this.scrollToBottom();

        // Save state after adding each message
        this.saveState();

        return message;
    }

    setStreaming(streaming) {
        this.isStreaming = streaming;
        const input = document.getElementById('chat-input');

        input.disabled = streaming;
        input.placeholder = streaming ? 'Đang trả lời...' : 'Nhập câu hỏi của bạn rồi nhấn Enter...';
    }

    newChat() {
        this.conversationId = null;
        document.getElementById('chat-messages').innerHTML = `<div class="message bot"><i class="fas fa-robot text-primary me-2"></i>Xin chào! Tôi là trợ lý ảo. Bạn cần hỗ trợ gì?</div>`;
        this.scrollToBottom();

        // Clear saved state when starting new chat
        this.clearSavedState();
    }

    cleanup() {
        if (this.eventSource) {
            this.eventSource.close(); // Closes the SSE connection.
            this.eventSource = null; // Clears the EventSource object.
        }
    }

    scrollToBottom() {
        const container = document.getElementById('chat-messages');
        container.scrollTop = container.scrollHeight; // Sets scroll position to the bottom.
    }
}

// Initializes a new ChatWidget instance only if the chat widget elements exist on the page.
document.addEventListener('DOMContentLoaded', () => {
    // Check if the chat widget elements exist before initializing
    const chatToggle = document.getElementById('chat-toggle');
    const chatBox = document.getElementById('chat-box');

    if (chatToggle && chatBox) {
        // Initialize the chat widget
        const chatWidget = new ChatWidget();
        // Optionally store the instance if needed later
        globalThis.chatWidget = chatWidget;
    }
});