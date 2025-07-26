class ChatWidget {
    constructor() {
        this.isOpen = false;
        this.isStreaming = false;
        this.eventSource = null;
        this.conversationId = null;
        this.init();
    }

    init() {
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
            await this.streamResponse(message);
        } finally {
            this.setStreaming(false);
        }
    }

    streamResponse(message) {
        return new Promise((resolve) => {
            // Generate conversation ID on first message if not exists
            if (!this.conversationId) {
                this.conversationId = crypto.randomUUID();
            }
            
            const params = new URLSearchParams({ 
                message,
                conversationId: this.conversationId 
            });
            
            const url = `/api/chat/stream?${params}`;
            this.eventSource = new EventSource(url);
            let botMessage = null;
            let content = '';
            let hasReceivedMessage = false;
            
            this.eventSource.onmessage = (event) => {
                hasReceivedMessage = true;
                if (!botMessage) botMessage = this.addMessage('bot', '');
                
                const chatResponse = JSON.parse(event.data);
                const chunk = chatResponse.results?.[0]?.output?.text || '';
                
                if (this.isErrorMessage(chunk)) {
                    this.handleErrorMessage(botMessage, chunk);
                    this.cleanup();
                    resolve();
                    return;
                }
                
                content += chunk;
                
                botMessage.innerHTML = `<i class="fas fa-robot text-primary me-2"></i>${content}`;
                this.scrollToBottom();
            };
            
            this.eventSource.onerror = (error) => {
                console.log('SSE connection closed. Has received message:', hasReceivedMessage);
                
                // Only show error if we haven't received any data (real connection error)
                if (!hasReceivedMessage) {
                    console.error('Real SSE Error:', error);
                    this.addMessage('error', 'Không thể kết nối tới server. Vui lòng thử lại sau.');
                }
                
                this.cleanup();
                resolve();
            };
            
            // Clean up when stream is done
            this.eventSource.addEventListener('close', () => {
                console.log('SSE stream completed successfully');
                this.cleanup();
                resolve();
            });
            
            this.eventSource.onopen = () => {
                
            };
        });
    }

    isErrorMessage(chunk) {
        return chunk && (chunk.includes('Quá nhiều yêu cầu') || 
                        chunk.includes('Vui lòng nhập tin nhắn') || 
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
        this.conversationId = null; // Reset conversation ID
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