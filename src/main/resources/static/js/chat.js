// Defines the ChatWidget class to manage chat functionality.
class ChatWidget {
    /**
     * Initializes the ChatWidget.
     */
    constructor() {
        this.isOpen = false; // Tracks if the chat box is open.
        this.isStreaming = false; // Tracks if a message stream is active.
        this.eventSource = null; // Holds the EventSource object for server-sent events.
        this.conversationId = null; // Stores the current conversation ID.
        this.init(); // Calls the initialization method.
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

    /**
     * Initializes the chat widget by attaching event listeners.
     */
    init() {
        this.attachEvents();
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

    /**
     * Toggles the visibility of the chat box.
     */
    toggle() {
        const chatBox = document.getElementById('chat-box');
        const toggle = document.getElementById('chat-toggle');

        this.isOpen = !this.isOpen; // Toggles the open state.
        chatBox.style.display = this.isOpen ? 'block' : 'none'; // Shows or hides the chat box.
        // Changes the toggle button icon based on the chat box state.
        toggle.innerHTML = this.isOpen ? '<i class="fas fa-times"></i>' : '<i class="fas fa-comments"></i>';

        if (this.isOpen) this.scrollToBottom();
    }

    /**
     * Sends a message from the user to the chat.
     * @param {Event} e - The event object (e.g., form submission event).
     */
    async sendMessage(e) {
        e.preventDefault(); // Prevents default form submission.
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

            // Prepares URL parameters for the SSE request.
            const params = new URLSearchParams({
                message,
                conversationId: this.conversationId
            });

            const url = `/api/chat/stream?${params}`;
            this.eventSource = new EventSource(url); // Creates a new EventSource.
            let botMessage = null; // Holds the DOM element for the bot's message.
            let content = ''; // Accumulates the streamed content.
            let hasReceivedMessage = false; // Tracks if any message data has been received.

            // Handles incoming messages from the SSE stream.
            this.eventSource.onmessage = (event) => {
                hasReceivedMessage = true;
                if (!botMessage) botMessage = this.addMessage('bot', '');

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
                botMessage.innerHTML = `<i class="fas fa-robot text-primary me-2"></i>${this.escapeHtml(content)}`;
                this.scrollToBottom();
            };

            // Handles errors from the SSE stream.
            this.eventSource.onerror = (error) => {
                // Only shows an error message if no data has been received (indicates a connection error).
                if (!hasReceivedMessage) {
                    this.addMessage('error', 'Không thể kết nối tới server. Vui lòng thử lại sau.');
                }

                this.cleanup();
                resolve();
            };

            // Event listener for when the SSE connection is closed.
            this.eventSource.addEventListener('close', () => {
                this.cleanup();
                resolve();
            });

            // Event listener for when the SSE connection is opened.
            this.eventSource.onopen = () => {
                // No specific action needed on open for this implementation.
            };
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
            botMessage.parentNode.removeChild(botMessage); // Removes the incomplete bot message.
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

        // Sets inner HTML with appropriate icon and escaped content.
        if (type === 'user') {
            message.innerHTML = `<i class="fas fa-user text-white me-2"></i>${this.escapeHtml(content)}`;
        } else if (type === 'error') {
            message.innerHTML = `<i class="fas fa-exclamation-triangle text-danger me-2"></i>${this.escapeHtml(content)}`;
        } else {
            message.innerHTML = `<i class="fas fa-robot text-primary me-2"></i>${this.escapeHtml(content)}`;
        }

        container.appendChild(message);
        this.scrollToBottom();
        return message;
    }

    /**
     * Sets the streaming state and updates the input field accordingly.
     * @param {boolean} streaming - True if streaming, false otherwise.
     */
    setStreaming(streaming) {
        this.isStreaming = streaming;
        const input = document.getElementById('chat-input');

        input.disabled = streaming;
        input.placeholder = streaming ? 'Đang trả lời...' : 'Nhập câu hỏi của bạn rồi nhấn Enter...';
    }

    /**
     * Starts a new chat session.
     */
    newChat() {
        this.conversationId = null;
        document.getElementById('chat-messages').innerHTML = `
            <div class="message bot">
                <i class="fas fa-robot text-primary me-2"></i>
                Xin chào! Tôi là trợ lý ảo. Bạn cần hỗ trợ gì?
            </div>
        `;
        this.scrollToBottom();
    }

    /**
     * Cleans up the EventSource connection.
     */
    cleanup() {
        if (this.eventSource) {
            this.eventSource.close(); // Closes the SSE connection.
            this.eventSource = null; // Clears the EventSource object.
        }
    }

    /**
     * Scrolls the chat messages container to the bottom.
     */
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
        new ChatWidget();
    }
});