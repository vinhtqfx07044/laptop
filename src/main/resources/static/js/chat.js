class ChatWidget {
    constructor() {
        this.isOpen = false;
        this.isStreaming = false;
        this.eventSource = null;
        this.init();
    }

    init() {
        // Widget is already in DOM, just attach events
        this.attachEvents();
    }

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
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage(e);
                }
            };
        }
    }

    toggle() {
        const chatBox = document.getElementById('chat-box');
        const toggle = document.getElementById('chat-toggle');
        
        this.isOpen = !this.isOpen;
        chatBox.style.display = this.isOpen ? 'block' : 'none';
        toggle.innerHTML = this.isOpen ? '<i class="fas fa-times"></i>' : '<i class="fas fa-comments"></i>';
        
        if (this.isOpen) this.scrollToBottom();
    }

    async sendMessage(e) {
        e.preventDefault();
        if (this.isStreaming) return;

        const input = document.getElementById('chat-input');
        const message = input.value.trim();
        
        if (!message || message.length > 200) {
            this.addMessage('error', message.length > 200 ? 'Tin nhắn quá dài (tối đa 200 ký tự).' : 'Vui lòng nhập tin nhắn.');
            return;
        }
        
        input.value = '';
        this.addMessage('user', message);
        this.setStreaming(true);
        
        try {
            // Get full conversation history
            const conversation = this.getConversationHistory();
            await this.streamResponse(conversation);
        } finally {
            this.setStreaming(false);
        }
    }

    getConversationHistory() {
        const messages = [];
        const messageElements = document.querySelectorAll('#chat-messages .message');
        
        messageElements.forEach(element => {
            if (element.classList.contains('user')) {
                // Remove FontAwesome user icon and extract text
                const content = element.textContent.replace(/^\s*\s*/, '').trim();
                messages.push({ role: 'user', content });
            } else if (element.classList.contains('bot')) {
                // Remove FontAwesome robot icon and extract text  
                const content = element.textContent.replace(/^\s*\s*/, '').trim();
                if (content) messages.push({ role: 'assistant', content });
            }
        });
        
        return JSON.stringify(messages);
    }

    streamResponse(conversation) {
        return new Promise((resolve) => {
            const url = `/api/chat/stream?conversation=${encodeURIComponent(conversation)}`;
            this.eventSource = new EventSource(url);
            let botMessage = null;
            let content = '';
            
            this.eventSource.onmessage = (event) => {
                if (!botMessage) botMessage = this.addMessage('bot', '');
                
                const chunk = this.parseChunk(event.data);
                if (this.isErrorMessage(chunk)) {
                    this.handleErrorMessage(botMessage, chunk);
                    return;
                }
                
                content += chunk;
                botMessage.innerHTML = `<i class="fas fa-robot text-primary me-2"></i>${content}`;
                this.scrollToBottom();
            };
            
            this.eventSource.onerror = () => {
                this.addMessage('error', 'Không thể kết nối tới server. Vui lòng thử lại sau.');
                this.cleanup();
                resolve();
            };
        });
    }

    parseChunk(data) {
        try {
            const chatResponse = JSON.parse(data);
            return chatResponse.result?.output?.text || chatResponse.results?.[0]?.output?.text || '';
        } catch (e) {
            return data; // Fallback to plain text
        }
    }

    isErrorMessage(chunk) {
        return chunk && (chunk.includes('Quá nhiều yêu cầu') || 
                        chunk.includes('không hợp lệ') || 
                        chunk.includes('Đã xảy ra lỗi'));
    }

    handleErrorMessage(botMessage, errorText) {
        if (botMessage?.parentNode) {
            botMessage.parentNode.removeChild(botMessage);
        }
        this.addMessage('error', errorText);
    }

    addMessage(type, content) {
        const container = document.getElementById('chat-messages');
        const message = document.createElement('div');
        message.className = `message ${type}`;
        
        if (type === 'user') {
            message.innerHTML = `<i class="fas fa-user text-white me-2"></i>${content}`;
        } else if (type === 'error') {
            message.innerHTML = `<i class="fas fa-exclamation-triangle text-danger me-2"></i>${content}`;
        } else {
            message.innerHTML = `<i class="fas fa-robot text-primary me-2"></i>${content}`;
        }
        
        container.appendChild(message);
        this.scrollToBottom();
        return message;
    }

    setStreaming(streaming) {
        this.isStreaming = streaming;
        const input = document.getElementById('chat-input');
        
        input.disabled = streaming;
        input.placeholder = streaming ? 'Đang trả lời...' : 'Nhập câu hỏi của bạn rồi nhấn Enter...';
    }

    newChat() {
        document.getElementById('chat-messages').innerHTML = `
            <div class="message bot">
                <i class="fas fa-robot text-primary me-2"></i>
                Xin chào! Tôi là trợ lý ảo. Bạn cần hỗ trợ gì?
            </div>
        `;
        this.scrollToBottom();
    }

    cleanup() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
    }

    scrollToBottom() {
        const container = document.getElementById('chat-messages');
        container.scrollTop = container.scrollHeight;
    }
}

document.addEventListener('DOMContentLoaded', () => new ChatWidget());